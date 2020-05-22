package com.view.refresh;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;

import com.utils.LogUtils;
import com.view.R;
import com.view.refresh.ext.DefaultRefreshLayout;

/**
 * @describe 仿官方SwipeRefreshLayout写的刷新控件,copy from {@link androidx.swiperefreshlayout.widget.SwipeRefreshLayout}
 * @author: lixiaopeng
 * @Date: 2019-06-14
 */
public class SwipeRefreshLayout extends ViewGroup implements NestedScrollingParent,
        NestedScrollingChild {

    private static final String LOG_TAG = "SwipeRefreshLayout";

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;
    private float mTotalUnconsumed;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private boolean mNestedScrollInProgress;

    protected View mTarget; // the target of the gesture
    protected OnRefreshListener mRefreshListener;
    private int mTouchSlop;

    private float mInitialMotionY, mLastMotionY;
    private float mInitialDownY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;

    /**
     * 是否正在刷新
     */
    protected boolean isRefreshing = false;
    /**
     * 是否可以拖动
     */
    private boolean canDragged = false;
    @NonNull
    private final DecelerateInterpolator mDecelerateInterpolator;

    protected boolean canRefresh = true;

    protected LoadingLayout headLoadingLayout;

    private float moveOffset = 0;
    protected int freshViewType = 0;

    private ValueAnimator resetHeaderAnimator;

    public SwipeRefreshLayout(Context context) {
        this(context, null);
    }

    public SwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        setWillNotDraw(false);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeRefreshLayout);
        freshViewType = typedArray.getInteger(R.styleable.SwipeRefreshLayout_fresh_type, 0);
        setEnabled(typedArray.getBoolean(R.styleable.SwipeRefreshLayout_enabled, true));
        typedArray.recycle();

        addView(createLoadingLayout());

        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);

        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    /**
     * 配置刷新头部
     *
     * @return
     */
    protected LoadingLayout createLoadingLayout() {
        if (freshViewType == 0) {
            if (headLoadingLayout == null) {
                headLoadingLayout = new DefaultRefreshLayout(getContext());
                headLoadingLayout.setRefreshLayoutInstance(this);
            }
        }/* else if(freshViewType==1){

        }*/ else {
            if (headLoadingLayout == null) {
                headLoadingLayout = new DefaultRefreshLayout(getContext());
                headLoadingLayout.setRefreshLayoutInstance(this);
            }
        }

        return headLoadingLayout;
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        mRefreshListener = listener;
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(headLoadingLayout)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        try {
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            if (getChildCount() == 0) {
                return;
            }
            if (mTarget == null) {
                ensureTarget();
            }
            if (mTarget == null) {
                return;
            }

            View child = mTarget;
            int childLeft = getPaddingLeft();
            int childTop = getPaddingTop();
            int childWidth = width - getPaddingLeft() - getPaddingRight();
            int childHeight = height - getPaddingTop() - getPaddingBottom();
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);

            int headWidth = headLoadingLayout.getMeasuredWidth();
            int headHeight = headLoadingLayout.getMeasuredHeight();

            headLoadingLayout.layout((width / 2 - headWidth / 2), -headHeight, (width / 2 + headWidth / 2), 0);

            headLoadingLayout.setTargetViewHeight(child.getMeasuredHeight());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        mTarget.measure(
                MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                        MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                        MeasureSpec.EXACTLY));

        headLoadingLayout.measure(
                MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                        MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
    }

    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0 && (absListView.getFirstVisiblePosition() > 0
                        || absListView.getChildAt(0).getTop() < absListView.getPaddingTop());
            } else {
                return mTarget.canScrollVertically(-1) || mTarget.getTranslationY() > 0;
            }
        } else {
            return mTarget.canScrollVertically(-1);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        ensureTarget();
        int action = ev.getActionMasked();

        if (canChildScrollUp() || !canRefresh || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;
                final float initialDownY = getMotionEventY(ev, mActivePointerId);
                if (initialDownY == -1) {
                    return false;
                }
                canDragged = true;
                if (isRefreshing()) {
                    moveOffset = mTarget.getTranslationY();
                } else {
                    moveOffset = 0;
                }
                mInitialDownY = mLastMotionY = initialDownY;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }

                final float y = getMotionEventY(ev, mActivePointerId);
                if (y == -1) {
                    return false;
                }

                final float yDiff = y - mInitialDownY;
                if (yDiff > mTouchSlop && !mIsBeingDragged) {
                    mInitialMotionY = mInitialDownY + mTouchSlop;
                    mIsBeingDragged = true;
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = ev.findPointerIndex(activePointerId);
        if (index < 0) {
            return -1;
        }
        return ev.getY(index);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        final int action = ev.getActionMasked();
        int pointerIndex = -1;

        if (!isEnabled() || canChildScrollUp()) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;
                canDragged = true;
                break;

            case MotionEvent.ACTION_MOVE:
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                if (!canDragged) {
                    return false;
                }
                float y = ev.getY(pointerIndex);
                moveOffset += ((y - mLastMotionY) * DRAG_RATE);
                mLastMotionY = y;
                if (mIsBeingDragged) {
                    return onTargetMove(moveOffset);
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN: {
                pointerIndex = MotionEventCompat.getActionIndex(ev);
                if (pointerIndex < 0) {
                    return false;
                }
                mActivePointerId = MotionEventCompat.getPointerId(ev, pointerIndex);

                mLastMotionY = MotionEventCompat.getY(ev, pointerIndex);

                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
                break;

            case MotionEvent.ACTION_UP: {
                pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }

                mIsBeingDragged = false;
                canDragged = true;
                moveOffset = 0;
                finishMove();
                mActivePointerId = INVALID_POINTER;
                return false;
            }
            case MotionEvent.ACTION_CANCEL:
                return false;
        }

        return true;
    }

    protected void finishMove() {
        if (mTarget.getTranslationY() >= headLoadingLayout.getLoadingOffsetHeight()
                && mRefreshListener != null
                && !isRefreshing()) {
            changeFreshState(true);
            mRefreshListener.onRefresh();
            headLoadingLayout.onRefreshing();
        }
        resetHeader();
    }

    /**
     * 被包裹控件move中
     *
     * @param offset
     */
    private boolean onTargetMove(float offset) {
        if (offset < headLoadingLayout.getLoadingOffsetHeight()) {//没有超过刷新控件的高度
            headLoadingLayout.setTranslationY(offset);
            mTarget.setTranslationY(offset);
        } else {//超过刷新控件的高度
            //设置刷新头部显示出来
            float translationY = headLoadingLayout.getTranslationY();
            if (translationY != headLoadingLayout.getLoadingOffsetHeight()) {
                headLoadingLayout.setTranslationY(headLoadingLayout.getLoadingOffsetHeight());
            }
            //设置被包裹控件移动
            mTarget.setTranslationY(offset);
        }

        if (!isRefreshing()){
            headLoadingLayout.onMove(offset, isRefreshing());
        }

        if (mIHeaderScroll != null) {
            mIHeaderScroll.onHeaderScroll(offset);
        }

        return offset >= 0;
    }

    protected void resetHeader() {
        if (resetHeaderAnimator != null && resetHeaderAnimator.isRunning()) {
            resetHeaderAnimator.cancel();
        }
        final float scrollY = mTarget.getTranslationY();
        resetHeaderAnimator = ValueAnimator.ofFloat(scrollY,
                isRefreshing() ? headLoadingLayout.getLoadingOffsetHeight() : 0);
        resetHeaderAnimator.setDuration(400);
        resetHeaderAnimator.setInterpolator(mDecelerateInterpolator);
        resetHeaderAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                float animatedValue = (float) animation.getAnimatedValue();
                onTargetMove(animatedValue);
            }
        });
        resetHeaderAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

                if (!isRefreshing()) {
                    canDragged = false;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        resetHeaderAnimator.start();
    }

    private void showLoadingView() {
        ensureTarget();
        final float targetY = mTarget.getTranslationY();
        ValueAnimator animator = ValueAnimator.ofFloat(targetY, headLoadingLayout.getLoadingOffsetHeight());
        animator.setDuration(400);
        animator.setInterpolator(mDecelerateInterpolator);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                float animatedValue = (float) animation.getAnimatedValue();
                onTargetMove(animatedValue);
            }
        });
        animator.start();
    }

    public synchronized boolean isRefreshing() {
        return isRefreshing;
    }

    private synchronized void changeFreshState(boolean isRefreshing){
        this.isRefreshing = isRefreshing;
    }

    public void stopRefresh() {
        LogUtils.d(LOG_TAG, "stopRefresh: " + System.currentTimeMillis()/1000);
        if (isRefreshing()) {
            changeFreshState(false);
            headLoadingLayout.onRefreshFinish();
            resetHeader();
        }
    }

    public void setRefresh(boolean isRefresh) {
        if (isRefresh) {
            changeFreshState(true);
            showLoadingView();
            mRefreshListener.onRefresh();
            headLoadingLayout.onRefreshing();
        } else {
            stopRefresh();
        }
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setCanRefresh(boolean canRefresh) {
        this.canRefresh = canRefresh;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        if ((android.os.Build.VERSION.SDK_INT < 21 && mTarget instanceof AbsListView)
                || (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return canRefresh //&& !mReturningToStart
                // && !isRefreshing
                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = 0;
        mNestedScrollInProgress = true;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            onTargetMove(mTotalUnconsumed * DRAG_RATE);
        }
        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {
            finishMove();
            mTotalUnconsumed = 0;
        }
        // Dispatch up our nested parent
        stopNestedScroll();
    }


    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (dy < 0 && !canChildScrollUp()) {
            mTotalUnconsumed += Math.abs(dy);
            onTargetMove(mTotalUnconsumed * DRAG_RATE);
        }
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX,
                                    float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY,
                                 boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public interface OnRefreshListener {
        public void onRefresh();
    }

    public interface IHeaderScroll {
        public void onHeaderScroll(float scrollY);
    }

    @Nullable
    private IHeaderScroll mIHeaderScroll = null;

    public void setIHeaderScroll(IHeaderScroll mIHeaderScroll) {
        this.mIHeaderScroll = mIHeaderScroll;
    }

    @Override
    public boolean canScrollVertically(int direction) {
        View scrollingChild = findScrollingChild(mTarget);
        if (scrollingChild != null) {
            return scrollingChild.canScrollVertically(direction);
        }
        return mTarget.canScrollVertically(direction);
    }


    @Nullable
    private View findScrollingChild(View view) {
        if (ViewCompat.isNestedScrollingEnabled(view)) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                View scrollingChild = findScrollingChild(group.getChildAt(i));
                if (scrollingChild != null) {
                    return scrollingChild;
                }
            }
        }
        return view;
    }
}
