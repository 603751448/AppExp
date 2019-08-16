package com.view.loadmore;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.view.R;
import com.view.loadmore.LoadMoreRecyclerView.OnLoadMoreListener;

import java.util.List;

/**
 * 加载更多适配器
 */
public abstract class LoadMoreAdapter extends RecyclerView.Adapter {

    /**
     * 加载更多条目类型
     */
    public static final int TYPE_LOADMORE = -100;

    protected LoadMoreRecyclerView mLoadMoreRecyclerview;
    protected Context context;

    private LayoutManager layoutManager;

    public LoadMoreAdapter(Context context) {
        this.context = context;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mLoadMoreRecyclerview = (LoadMoreRecyclerView) recyclerView;
        layoutManager = mLoadMoreRecyclerview.getLayoutManager();
        //解决网格占一整行
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int itemViewType = getItemViewType(position);
                    return itemViewType == TYPE_LOADMORE ? gridLayoutManager.getSpanCount() : 1;
                }
            });
        }
    }


    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1) return TYPE_LOADMORE;
        return super.getItemViewType(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADMORE) {
            return new LoadmoreViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_loadmore, parent, false));
        } else {
            return mOnCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position, List payloads) {
        // super.onBindViewHolder(viewHolder, position, payloads);
        // 预加载处理
        int loadmoreStatus = mLoadMoreRecyclerview.getLoadMoreStatus();
        if (mLoadMoreRecyclerview.isPreLoad()
                && (loadmoreStatus == LoadMoreRecyclerView.LM_LOAD_SUCCESS
                || loadmoreStatus == LoadMoreRecyclerView.LM_LOAD_FAILURE)) {
            int toBottomItemCount = getItemCount() - position;
            if (toBottomItemCount <= LoadMoreRecyclerView.PRE_LOAD_COUNT) {
                mLoadMoreRecyclerview.setLoadMoreStatus(LoadMoreRecyclerView.LM_LOADING);
                notifyLoadmore();
            }
        }

        //解决瀑布流加载更多占一整行
        if (layoutManager instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
            ViewGroup.LayoutParams targetParams = viewHolder.itemView.getLayoutParams();
            StaggeredGridLayoutManager.LayoutParams StaggerLayoutParams;
            if (targetParams != null) {
                StaggerLayoutParams =
                        new StaggeredGridLayoutManager.LayoutParams(targetParams.width, targetParams.height);
            } else {
                StaggerLayoutParams =
                        new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            StaggerLayoutParams.setFullSpan(true);
        }

        if (viewHolder.getItemViewType() == TYPE_LOADMORE) {
            bindLoadMoreViewHolder(viewHolder);
            return;
        } else {
            mOnBindViewHolder(viewHolder, position, null);
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        onBindViewHolder(viewHolder, position, null);
    }

    @Override
    public int getItemCount() {
        return 1 + mGetItemCount();
    }

    protected abstract int mGetItemCount();

    /**
     * 绑定加载更多数据
     *
     * @param viewHolder
     */
    private void bindLoadMoreViewHolder(ViewHolder viewHolder) {
        LoadmoreViewHolder holder = (LoadmoreViewHolder) viewHolder;
        final int loadmoreStatus = mLoadMoreRecyclerview.getLoadMoreStatus();
        switch (mLoadMoreRecyclerview.getLoadMoreStatus()) {
            case LoadMoreRecyclerView.LM_LOAD_COMPLETE:
                holder.loadmoreTitle.setText(context.getResources().getString(R.string.load_complete));
                holder.loadmoreView.setVisibility(View.GONE);
                break;
            case LoadMoreRecyclerView.LM_LOAD_SUCCESS:
                holder.loadmoreTitle.setText(context.getString(R.string.click_load));
                holder.loadmoreView.setVisibility(View.GONE);
                break;
            case LoadMoreRecyclerView.LM_LOAD_FAILURE:
                holder.loadmoreTitle.setText(context.getString(R.string.load_failure));
                holder.loadmoreView.setVisibility(View.GONE);
                break;
            case LoadMoreRecyclerView.LM_LOADING:
                holder.loadmoreTitle.setText(context.getString(R.string.loading));
                holder.loadmoreView.setVisibility(View.VISIBLE);
                break;
            case LoadMoreRecyclerView.LM_LOAD_NON:
                holder.loadmoreTitle.setText("");
                holder.loadmoreView.setVisibility(View.GONE);
                break;

        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loadmoreStatus == LoadMoreRecyclerView.LM_LOAD_FAILURE || loadmoreStatus == LoadMoreRecyclerView.LM_LOAD_SUCCESS) {
                    mLoadMoreRecyclerview.setLoadMoreStatus(LoadMoreRecyclerView.LM_LOADING);
                    notifyItemChanged(getItemCount() - 1);
                    notifyLoadmore();
                }
            }
        });
    }

    /**
     * 通知加载更多
     */
    private void notifyLoadmore() {
        OnLoadMoreListener onLoadmoreListener = mLoadMoreRecyclerview.getOnLoadmoreListener();
        if (mLoadMoreRecyclerview.getOnLoadmoreListener() != null) {
            onLoadmoreListener.onLoadmore();
        }
    }


    static class LoadmoreViewHolder extends ViewHolder {

        ProgressBar loadmoreView;
        TextView loadmoreTitle;

        public LoadmoreViewHolder(View itemView) {
            super(itemView);
            loadmoreView = itemView.findViewById(R.id.loadmore);
            loadmoreTitle = itemView.findViewById(R.id.tv_loadmore_title);
        }
    }

    /**
     * @param parent
     * @param viewType
     * @return
     * @see RecyclerView.Adapter#onCreateViewHolder(ViewGroup, int)
     */
    protected abstract ViewHolder mOnCreateViewHolder(ViewGroup parent, int viewType);

    /**
     * @param viewHolder
     * @param position
     * @param payloads
     * @see RecyclerView.Adapter#onBindViewHolder(ViewHolder, int, List)
     */
    protected abstract void mOnBindViewHolder(ViewHolder viewHolder, int position, List payloads);
}