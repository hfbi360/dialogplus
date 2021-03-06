package com.orhanobut.dialogplus;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;


/**
 * @author Orhan Obut
 */
public class DialogPlus {

    private static final String TAG = DialogPlus.class.getSimpleName();

    /**
     * Determine whether the resources are set or not
     */
    private static final int INVALID = -1;

    /**
     * DialogPlus base layout root view
     */
    private final ViewGroup rootView;

    /**
     * DialogPlus content container which is a different layout rather than base layout
     */
    private final ViewGroup contentContainer;

    /**
     * Determines the position of the dialog, only BOTTOM and TOP should be used, as default
     * it is BOTTOM
     */
    private final int gravity;

    /**
     * Either the content should fill the all screen or only the half, when the content reaches
     * the limit, it will be scrollable, BasicHolder cannot be scrollable, use it only for
     * a few items
     */
    private final ScreenType screenType;

    /**
     * Determines whether dialog should be dismissed by back button or touch in the black overlay
     */
    private final boolean isCancelable;

    /**
     * Determines whether dialog is showing dismissing animation and avoid to repeat it
     */
    private boolean isDismissing;

    /**
     * topView and bottomView are used to set the position of the dialog
     * If the position is top, bottomView will fill the screen, otherwise
     * topView will the screen
     */
    private final View topView;
    private final View bottomView;

    /**
     * footerView, headerView are used
     * to set header/footer for the dialog content
     */
    private final View footerView;
    private final View headerView;

    /**
     * Content adapter
     */
    private final BaseAdapter adapter;

    /**
     * Listener for the user to take action by clicking any item
     */
    private final OnItemClickListener onItemClickListener;

    /**
     * Content
     */
    private final Holder holder;

    /**
     * basically activity root view
     */
    private final ViewGroup decorView;
    private final LayoutInflater inflater;

    /**
     * Determines the content background color, as default it is white
     */
    private final int backgroundColorResourceId;

    /**
     * Determines the in and out animation of the dialog. Default animation are bottom sliding animations
     */
    private final int inAnimationResource;
    private final int outAnimationResource;

    public enum ScreenType {
        HALF, FULL
    }

    private DialogPlus(Builder builder) {
        inflater = LayoutInflater.from(builder.context);
        Activity activity = (Activity) builder.context;

        this.holder = getHolder(builder.holder);
        this.backgroundColorResourceId = builder.backgroundColorResourceId;
        this.headerView = getView(builder.headerViewResourceId, builder.headerView);
        this.footerView = getView(builder.footerViewResourceId, builder.footerView);
        this.screenType = builder.screenType;
        this.adapter = builder.adapter;
        this.onItemClickListener = builder.onItemClickListener;
        this.isCancelable = builder.isCancelable;
        this.gravity = builder.gravity;
        this.inAnimationResource = builder.inAnimation;
        this.outAnimationResource = builder.outAnimation;

        /**
         * Avoid getting directly from the decor view because by doing that we are overlapping the black soft key on
         * nexus device. I think it should be tested on different devices but in my opinion is the way to go.
         * @link http://stackoverflow.com/questions/4486034/get-root-view-from-current-activity
         */
        decorView = (ViewGroup) activity.getWindow().getDecorView().findViewById(android.R.id.content);
        rootView = (ViewGroup) inflater.inflate(R.layout.base_container, null);
        contentContainer = (ViewGroup) rootView.findViewById(R.id.content_container);
        topView = rootView.findViewById(R.id.top_view);
        bottomView = rootView.findViewById(R.id.bottom_view);

        createDialog();
    }

    /**
     * It adds the dialog view into rootView which is decorView of activity
     */
    public void show() {
        if (isShowing()) {
            return;
        }
        onAttached(rootView);
    }

    /**
     * It basically check if the rootView contains the dialog plus view.
     *
     * @return true if it contains
     */
    public boolean isShowing() {
        View view = decorView.findViewById(R.id.outmost_container);
        return view != null;
    }

    /**
     * It is called when to dismiss the dialog, either by calling dismiss() method or with cancellable
     */
    public void dismiss() {
        if (isDismissing) {
            return;
        }

        Context context = decorView.getContext();
        Animation outAnim = AnimationUtils.loadAnimation(context, this.outAnimationResource);
        outAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                decorView.removeView(rootView);
                isDismissing = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        contentContainer.startAnimation(outAnim);
        isDismissing = true;
    }

    private void createDialog() {
        initViews();
        initContentView();
        initPosition();
        initCancelable();
    }

    /**
     * Initialize the appropriate views and also set for the back press button.
     */
    private void initViews() {
        if (backgroundColorResourceId != INVALID) {
            contentContainer.setBackgroundResource(backgroundColorResourceId);
        }
    }

    /**
     * It is called in order to create content
     */
    private void initContentView() {
        View contentView = createView(inflater);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, gravity
        );
        contentView.setLayoutParams(params);
        contentContainer.addView(contentView);
    }

    /**
     * It is called to set whether the dialog is cancellable by pressing back button or
     * touching the black overlay
     */
    private void initCancelable() {
        if (!isCancelable) {
            return;
        }
        topView.setOnTouchListener(onCancelableTouchListener);
        bottomView.setOnTouchListener(onCancelableTouchListener);
    }

    /**
     * It is called when the set the position of the dialog
     */
    private void initPosition() {
        if (screenType == ScreenType.FULL) {
            topView.setVisibility(View.GONE);
            bottomView.setVisibility(View.GONE);
            return;
        }
        switch (gravity) {
            case Gravity.TOP:
                bottomView.setVisibility(View.VISIBLE);
                topView.setVisibility(View.GONE);
                break;
            default:
                bottomView.setVisibility(View.GONE);
                topView.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * it is called when the content view is created
     *
     * @param inflater
     * @return any view which is passed
     */
    private View createView(LayoutInflater inflater) {
        View view = holder.getView(inflater, rootView);
        holder.addFooter(footerView);
        holder.addHeader(headerView);
        if (adapter != null) {
            holder.setAdapter(adapter);
        }

        holder.setOnItemClickListener(new OnHolderListener() {
            @Override
            public void onItemClick(Object item, View view, int position) {
                if (onItemClickListener == null) {
                    return;
                }
                onItemClickListener.onItemClick(DialogPlus.this, item, view, position);
            }
        });
        return view;
    }

    /**
     * It is used to create content
     *
     * @return BasicHolder it setHolder is not called
     */
    private Holder getHolder(Holder holder) {
        if (holder == null) {
            holder = new ListHolder();
        }
        return holder;
    }

    /**
     * This will be called in order to create view, if the given view is not null,
     * it will be used directly, otherwise it will check the resourceId
     *
     * @return null if both resourceId and view is not set
     */
    private View getView(int resourceId, View view) {
        if (view != null) {
            return view;
        }
        if (resourceId != INVALID) {
            view = inflater.inflate(resourceId, null);
        }
        return view;
    }

    /**
     * It is called when the show() method is called
     *
     * @param view is the dialog plus view
     */
    private void onAttached(View view) {
        decorView.addView(view);
        Context context = decorView.getContext();
        Animation inAnim = AnimationUtils.loadAnimation(context, this.inAnimationResource);
        contentContainer.startAnimation(inAnim);

        contentContainer.requestFocus();
        holder.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                switch (event.getAction()) {
                    case KeyEvent.ACTION_UP:
                        if (keyCode == KeyEvent.KEYCODE_BACK && isCancelable) {
                            dismiss();
                            return true;
                        }
                        break;
                }
                return false;
            }
        });
    }

    /**
     * Called when the user touch on black overlay in order to dismiss the dialog
     */
    private final View.OnTouchListener onCancelableTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                dismiss();
            }
            return false;
        }
    };

    /**
     * Use this builder to create a dialog
     */
    public static class Builder {
        private BaseAdapter adapter;
        private Context context;
        private int footerViewResourceId = INVALID;
        private View footerView;
        private int headerViewResourceId = INVALID;
        private View headerView;
        private boolean isCancelable = true;
        private Holder holder;
        private int backgroundColorResourceId = INVALID;
        private int gravity = Gravity.BOTTOM;
        private ScreenType screenType = ScreenType.HALF;
        private OnItemClickListener onItemClickListener;
        private int inAnimation = R.anim.slide_in;
        private int outAnimation = R.anim.slide_out;

        private Builder() {
        }

        public Builder(Context context) {
            if (context == null) {
                throw new NullPointerException("Context may not be null");
            }
            this.context = context;
        }

        public Builder setAdapter(BaseAdapter adapter) {
            if (adapter == null) {
                throw new NullPointerException("Adapter may not be null");
            }
            this.adapter = adapter;
            return this;
        }

        public Builder setFooter(int resourceId) {
            this.footerViewResourceId = resourceId;
            return this;
        }

        public Builder setFooter(View view) {
            this.footerView = view;
            return this;
        }

        public Builder setHeader(int resourceId) {
            this.headerViewResourceId = resourceId;
            return this;
        }

        public Builder setHeader(View view) {
            this.headerView = view;
            return this;
        }

        public Builder setCancelable(boolean isCancelable) {
            this.isCancelable = isCancelable;
            return this;
        }

        public Builder setHolder(Holder holder) {
            this.holder = holder;
            return this;
        }

        public Builder setBackgroundColorResourceId(int resourceId) {
            this.backgroundColorResourceId = resourceId;
            return this;
        }

        public Builder setGravity(int gravity) {
            this.gravity = gravity;
            return this;
        }

        public Builder setInAnimation(int inAnimResource) {
            this.inAnimation = inAnimResource;
            return this;
        }

        public Builder setOutAnimation(int outAnimResource) {
            this.outAnimation = outAnimResource;
            return this;
        }

        public Builder setScreenType(ScreenType screenType) {
            this.screenType = screenType;
            return this;
        }

        public Builder setOnItemClickListener(OnItemClickListener listener) {
            this.onItemClickListener = listener;
            return this;
        }

        public DialogPlus create() {
            return new DialogPlus(this);
        }
    }
}
