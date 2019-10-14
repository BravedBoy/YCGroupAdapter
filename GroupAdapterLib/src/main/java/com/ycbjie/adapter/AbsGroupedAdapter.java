package com.ycbjie.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * <pre>
 *     @author yangchong
 *     blog  : https://github.com/yangchong211
 *     time  : 2019/9/18
 *     desc  : 通用的分组列表Adapter，通过它可以很方便的实现列表的分组效果。
 *     revise: 这个类提供了一系列的对列表的更新、删除和插入等操作的方法。
 *             使用者要使用这些方法的列表进行操作，而不要直接使用RecyclerView.Adapter的方法。
 *             因为当分组列表发生变化时，需要及时更新分组列表的组结构{@link AbsGroupedAdapter#mStructures}
 *             https://github.com/yangchong211/YCGroupAdapter
 * </pre>
 */
public abstract class AbsGroupedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 1;
    static final int TYPE_FOOTER = 2;
    static final int TYPE_CHILD = 3;
    private static final int TYPE_NO = 4;

    private OnHeaderClickListener mOnHeaderClickListener;
    private OnFooterClickListener mOnFooterClickListener;
    private OnChildClickListener mOnChildClickListener;

    protected Context mContext;
    /**
     * 保存分组列表的组结构
     */
    protected ArrayList<GroupStructure> mStructures = new ArrayList<>();
    /**
     * 数据是否发生变化。如果数据发生变化，要及时更新组结构。
     */
    private boolean isDataChanged;
    /**
     * itemViewType类型
     */
    private int itemType;

    public AbsGroupedAdapter(Context context) {
        mContext = context;
        registerAdapterDataObserver(new GroupDataObserver());
    }

    /**
     * 这个方法最先调用，与recyclerView绑定，可以做初始化工作
     * @param recyclerView                          recyclerView
     */
    @Override
    public void onAttachedToRecyclerView(@NotNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        structureChanged();
    }

    /**
     * 瀑布流视图中，item中view与window绑定时调用，也就是说view处于可见时会调用该方法
     * @param holder                                holder
     */
    @Override
    public void onViewAttachedToWindow(@NotNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        //处理StaggeredGridLayout，保证组头和组尾占满一行。
        if (isStaggeredGridLayout(holder)) {
            handleLayoutIfStaggeredGridLayout(holder, holder.getLayoutPosition());
        }
    }

    /**
     * 判断是否是瀑布流视图
     * @param holder                                holder
     * @return
     */
    private boolean isStaggeredGridLayout(RecyclerView.ViewHolder holder) {
        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
        if (layoutParams != null && layoutParams instanceof StaggeredGridLayoutManager.LayoutParams) {
            return true;
        }
        return false;
    }

    /**
     * 注意，在瀑布流视图中，header和footer视图布局，设置setFullSpan为true，也就是说占一行
     * @param holder                                holder
     * @param position                              索引
     */
    private void handleLayoutIfStaggeredGridLayout(RecyclerView.ViewHolder holder, int position) {
        if (judgeType(position) == TYPE_HEADER || judgeType(position) == TYPE_FOOTER) {
            StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams)
                    holder.itemView.getLayoutParams();
            p.setFullSpan(true);
        }
    }

    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View view;
        if (viewType != TYPE_NO){
            int layoutId = getLayoutId(itemType, viewType);
            view = LayoutInflater.from(mContext).inflate(layoutId, parent, false);
        } else {
            //使用空布局
            view = new View(parent.getContext());
        }
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NotNull final RecyclerView.ViewHolder holder, int position) {
        int type = judgeType(position);
        final int groupPosition = getGroupPositionForPosition(position);
        if (type == TYPE_HEADER) {
            if (mOnHeaderClickListener != null) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnHeaderClickListener != null) {
                            mOnHeaderClickListener.onHeaderClick(AbsGroupedAdapter.this,
                                    (GroupViewHolder) holder, groupPosition);
                        }
                    }
                });
            }
            onBindHeaderViewHolder((GroupViewHolder) holder, groupPosition);
        } else if (type == TYPE_FOOTER) {
            if (mOnFooterClickListener != null) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnFooterClickListener != null) {
                            mOnFooterClickListener.onFooterClick(AbsGroupedAdapter.this,
                                    (GroupViewHolder) holder, groupPosition);
                        }
                    }
                });
            }
            onBindFooterViewHolder((GroupViewHolder) holder, groupPosition);
        } else if (type == TYPE_CHILD) {
            final int childPosition = getChildPositionForPosition(groupPosition, position);
            if (mOnChildClickListener != null) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnChildClickListener != null) {
                            mOnChildClickListener.onChildClick(AbsGroupedAdapter.this,
                                    (GroupViewHolder) holder, groupPosition, childPosition);
                        }
                    }
                });
            }
            onBindChildViewHolder((GroupViewHolder) holder, groupPosition, childPosition);
        }
    }

    /**
     * 获取所有的孩子数量，包括
     * @return
     */
    @Override
    public int getItemCount() {
        if (isDataChanged) {
            structureChanged();
        }
        return count();
    }

    @Override
    public int getItemViewType(int position) {
        itemType = position;
        int type = judgeType(position);
        if (type == TYPE_HEADER) {
            return TYPE_HEADER;
        } else if (type == TYPE_FOOTER) {
            return TYPE_FOOTER;
        } else if (type == TYPE_CHILD) {
            return TYPE_CHILD;
        }
        return super.getItemViewType(position);
    }

    private int getLayoutId(int position, int viewType) {
        int type = judgeType(position);
        if (type == TYPE_HEADER) {
            return getHeaderLayout(viewType);
        } else if (type == TYPE_FOOTER) {
            return getFooterLayout(viewType);
        } else if (type == TYPE_CHILD) {
            return getChildLayout(viewType);
        }
        return 0;
    }

    private int count() {
        return countGroupRangeItem(0, mStructures.size());
    }

    /**
     * 判断item的type 头部 尾部 和 子项
     *
     * @param position
     * @return
     */
    public int judgeType(int position) {
        int itemCount = 0;
        //获取组的数量
        int groupCount = mStructures.size();

        for (int i = 0; i < groupCount; i++) {
            GroupStructure structure = mStructures.get(i);

            //判断是否有header头部view
            if (structure.hasHeader()) {
                itemCount += 1;
                if (position < itemCount) {
                    return TYPE_HEADER;
                }
            }

            //获取孩子的数量
            itemCount += structure.getChildrenCount();
            if (position < itemCount) {
                return TYPE_CHILD;
            }

            //判断是否有footer数量
            if (structure.hasFooter()) {
                itemCount += 1;
                if (position < itemCount) {
                    return TYPE_FOOTER;
                }
            }
        }

        //以防万一，为了避免在插入刷新，移除刷新时，避免索引越界异常，不要throw异常
        //即使当 position == getItemCount() 为true时，可以用空页面替代
        return TYPE_NO;
        //throw new IndexOutOfBoundsException("can't determine the item type of the position." +
        //        "position = " + position + ",item count = " + getItemCount());
    }

    /**
     * 重置组结构列表
     */
    private void structureChanged() {
        mStructures.clear();
        int groupCount = getGroupCount();
        for (int i = 0; i < groupCount; i++) {
            mStructures.add(new GroupStructure(hasHeader(i), hasFooter(i), getChildrenCount(i)));
        }
        isDataChanged = false;
    }

    /**
     * 根据下标计算position所在的组（groupPosition）
     *
     * @param position 下标
     * @return 组下标 groupPosition
     */
    public int getGroupPositionForPosition(int position) {
        int count = 0;
        int groupCount = mStructures.size();
        for (int i = 0; i < groupCount; i++) {
            count += countGroupItem(i);
            if (position < count) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 根据下标计算position在组中位置（childPosition）
     *
     * @param groupPosition 所在的组
     * @param position      下标
     * @return 子项下标 childPosition
     */
    public int getChildPositionForPosition(int groupPosition, int position) {
        if (groupPosition >= 0 && groupPosition < mStructures.size()) {
            int itemCount = countGroupRangeItem(0, groupPosition + 1);
            GroupStructure structure = mStructures.get(groupPosition);
            int p = structure.getChildrenCount() - (itemCount - position)
                    + (structure.hasFooter() ? 1 : 0);
            if (p >= 0) {
                return p;
            }
        }
        return -1;
    }

    /**
     * 获取一个组的组头下标 如果该组没有组头 返回-1
     *
     * @param groupPosition 组下标
     * @return 下标
     */
    public int getPositionForGroupHeader(int groupPosition) {
        if (groupPosition >= 0 && groupPosition < mStructures.size()) {
            GroupStructure structure = mStructures.get(groupPosition);
            if (!structure.hasHeader()) {
                return -1;
            }
            return countGroupRangeItem(0, groupPosition);
        }
        return -1;
    }

    /**
     * 获取一个组的组尾下标 如果该组没有组尾 返回-1
     *
     * @param groupPosition 组下标
     * @return 下标
     */
    public int getPositionForGroupFooter(int groupPosition) {
        if (groupPosition >= 0 && groupPosition < mStructures.size()) {
            GroupStructure structure = mStructures.get(groupPosition);
            if (!structure.hasFooter()) {
                return -1;
            }
            return countGroupRangeItem(0, groupPosition + 1) - 1;
        }
        return -1;
    }

    /**
     * 获取一个组指定的子项下标 如果没有 返回-1
     *
     * @param groupPosition 组下标
     * @param childPosition 子项的组内下标
     * @return 下标
     */
    public int getPositionForChild(int groupPosition, int childPosition) {
        if (groupPosition >= 0 && groupPosition < mStructures.size()) {
            GroupStructure structure = mStructures.get(groupPosition);
            if (structure.getChildrenCount() > childPosition) {
                int itemCount = countGroupRangeItem(0, groupPosition);
                return itemCount + childPosition + (structure.hasHeader() ? 1 : 0);
            }
        }
        return -1;
    }

    /**
     * 计算一个组里有多少个Item（头加尾加子项）
     *
     * @param groupPosition
     * @return
     */
    public int countGroupItem(int groupPosition) {
        int itemCount = 0;
        if (groupPosition >= 0 && groupPosition < mStructures.size()) {
            GroupStructure structure = mStructures.get(groupPosition);
            if (structure.hasHeader()) {
                itemCount += 1;
            }
            itemCount += structure.getChildrenCount();
            if (structure.hasFooter()) {
                itemCount += 1;
            }
        }
        return itemCount;
    }

    /**
     * 计算多个组的项的总和
     *
     * @return
     */
    public int countGroupRangeItem(int start, int count) {
        int itemCount = 0;
        //获取组的数量
        int size = mStructures.size();
        for (int i = start; i < size && i < start + count; i++) {
            itemCount += countGroupItem(i);
        }
        return itemCount;
    }

    //****** 刷新操作 *****//

    /**
     * Use {@link #notifyDataChanged()} instead.
     */
    @Deprecated
    public void changeDataSet() {
        notifyDataChanged();
    }

    /**
     * 通知数据列表刷新
     */
    public void notifyDataChanged() {
        isDataChanged = true;
        notifyDataSetChanged();
    }

    /**
     * Use {@link #notifyGroupChanged(int)} instead.
     *
     * @param groupPosition
     */
    @Deprecated
    public void changeGroup(int groupPosition) {
        notifyGroupChanged(groupPosition);
    }

    /**
     * 通知一组数据刷新，包括组头,组尾和子项
     *
     * @param groupPosition
     */
    public void notifyGroupChanged(int groupPosition) {
        int index = getPositionForGroupHeader(groupPosition);
        int itemCount = countGroupItem(groupPosition);
        if (index >= 0 && itemCount > 0) {
            notifyItemRangeChanged(index, itemCount);
        }
    }

    /**
     * Use {@link #notifyGroupRangeChanged(int, int)} instead.
     *
     * @param groupPosition
     * @param count
     */
    @Deprecated
    public void changeRangeGroup(int groupPosition, int count) {
        notifyGroupRangeChanged(groupPosition, count);
    }

    /**
     * 通知多组数据刷新，包括组头,组尾和子项
     *
     * @param groupPosition
     */
    public void notifyGroupRangeChanged(int groupPosition, int count) {
        int index = getPositionForGroupHeader(groupPosition);
        int itemCount = 0;
        if (groupPosition + count <= mStructures.size()) {
            itemCount = countGroupRangeItem(groupPosition, groupPosition + count);
        } else {
            itemCount = countGroupRangeItem(groupPosition, mStructures.size());
        }
        if (index >= 0 && itemCount > 0) {
            notifyItemRangeChanged(index, itemCount);
        }
    }

    /**
     * Use {@link #notifyHeaderChanged(int)} instead.
     *
     * @param groupPosition
     */
    @Deprecated
    public void changeHeader(int groupPosition) {
        notifyHeaderChanged(groupPosition);
    }

    /**
     * 通知组头刷新
     *
     * @param groupPosition
     */
    public void notifyHeaderChanged(int groupPosition) {
        int index = getPositionForGroupHeader(groupPosition);
        if (index >= 0) {
            notifyItemChanged(index);
        }
    }

    /**
     * Use {@link #notifyFooterChanged(int)} instead.
     *
     * @param groupPosition
     */
    @Deprecated
    public void changeFooter(int groupPosition) {
        notifyFooterChanged(groupPosition);
    }

    /**
     * 通知组尾刷新
     *
     * @param groupPosition
     */
    public void notifyFooterChanged(int groupPosition) {
        //如果index返回-1，则表示没有找到
        int index = getPositionForGroupFooter(groupPosition);
        if (index >= 0) {
            notifyItemChanged(index);
        }
    }

    /**
     * Use {@link #notifyChildChanged(int, int)} instead.
     *
     * @param groupPosition
     * @param childPosition
     */
    @Deprecated
    public void changeChild(int groupPosition, int childPosition) {
        notifyChildChanged(groupPosition, childPosition);
    }

    /**
     * 通知一组里的某个子项刷新
     *
     * @param groupPosition
     * @param childPosition
     */
    public void notifyChildChanged(int groupPosition, int childPosition) {
        int index = getPositionForChild(groupPosition, childPosition);
        if (index >= 0) {
            notifyItemChanged(index);
        }
    }

    /**
     * Use {@link #notifyChildRangeChanged(int, int, int)} instead.
     *
     * @param groupPosition
     * @param childPosition
     * @param count
     */
    @Deprecated
    public void changeRangeChild(int groupPosition, int childPosition, int count) {
        notifyChildRangeChanged(groupPosition, childPosition, count);
    }

    /**
     * 通知一组里的多个子项刷新
     *
     * @param groupPosition
     * @param childPosition
     * @param count
     */
    public void notifyChildRangeChanged(int groupPosition, int childPosition, int count) {
        if (groupPosition < mStructures.size()) {
            int index = getPositionForChild(groupPosition, childPosition);
            if (index >= 0) {
                GroupStructure structure = mStructures.get(groupPosition);
                if (structure.getChildrenCount() >= childPosition + count) {
                    notifyItemRangeChanged(index, count);
                } else {
                    notifyItemRangeChanged(index, structure.getChildrenCount() - childPosition);
                }
            }
        }
    }

    /**
     * Use {@link #notifyChildrenChanged(int)} instead.
     *
     * @param groupPosition
     */
    @Deprecated
    public void changeChildren(int groupPosition) {
        notifyChildrenChanged(groupPosition);
    }

    /**
     * 通知一组里的所有子项刷新
     *
     * @param groupPosition
     */
    public void notifyChildrenChanged(int groupPosition) {
        if (groupPosition >= 0 && groupPosition < mStructures.size()) {
            int index = getPositionForChild(groupPosition, 0);
            if (index >= 0) {
                GroupStructure structure = mStructures.get(groupPosition);
                notifyItemRangeChanged(index, structure.getChildrenCount());
            }
        }
    }

    //****** 删除操作 *****//

    /**
     * Use {@link #notifyDataRemoved()} instead.
     */
    @Deprecated
    public void removeAll() {
        notifyDataRemoved();
    }

    /**
     * 通知所有数据删除
     */
    public void notifyDataRemoved() {
        notifyItemRangeRemoved(0, getItemCount());
        mStructures.clear();
    }

    /**
     * Use {@link #notifyGroupRemoved(int)} instead.
     */
    @Deprecated
    public void removeGroup(int groupPosition) {
        notifyGroupRemoved(groupPosition);
    }

    /**
     * 通知一组数据删除，包括组头,组尾和子项
     *
     * @param groupPosition
     */
    public void notifyGroupRemoved(int groupPosition) {
        int index = getPositionForGroupHeader(groupPosition);
        int itemCount = countGroupItem(groupPosition);
        if (index >= 0 && itemCount > 0) {
            notifyItemRangeRemoved(index, itemCount);
            notifyItemRangeChanged(index, getItemCount() - itemCount);
            mStructures.remove(groupPosition);
        }
    }

    /**
     * Use {@link #notifyGroupRangeRemoved(int, int)} instead.
     */
    @Deprecated
    public void removeRangeGroup(int groupPosition, int count) {
        notifyGroupRangeRemoved(groupPosition, count);
    }

    /**
     * 通知多组数据删除，包括组头,组尾和子项
     *
     * @param groupPosition
     */
    public void notifyGroupRangeRemoved(int groupPosition, int count) {
        int index = getPositionForGroupHeader(groupPosition);
        int itemCount = 0;
        if (groupPosition + count <= mStructures.size()) {
            itemCount = countGroupRangeItem(groupPosition, groupPosition + count);
        } else {
            itemCount = countGroupRangeItem(groupPosition, mStructures.size());
        }
        if (index >= 0 && itemCount > 0) {
            notifyItemRangeRemoved(index, itemCount);
            notifyItemRangeChanged(index, getItemCount() - itemCount);
            mStructures.remove(groupPosition);
        }
    }

    /**
     * Use {@link #notifyHeaderRemoved(int)} instead.
     */
    @Deprecated
    public void removeHeader(int groupPosition) {
        notifyHeaderRemoved(groupPosition);
    }

    /**
     * 通知组头删除
     *
     * @param groupPosition
     */
    public void notifyHeaderRemoved(int groupPosition) {
        int index = getPositionForGroupHeader(groupPosition);
        if (index >= 0) {
            GroupStructure structure = mStructures.get(groupPosition);
            notifyItemRemoved(index);
            notifyItemRangeChanged(index, getItemCount() - index);
            structure.setHasHeader(false);
        }
    }

    /**
     * Use {@link #notifyFooterRemoved(int)} instead.
     *
     * @param groupPosition
     */
    @Deprecated
    public void removeFooter(int groupPosition) {
        notifyFooterRemoved(groupPosition);
    }

    /**
     * 通知组尾删除
     *
     * @param groupPosition
     */
    public void notifyFooterRemoved(int groupPosition) {
        //如果index返回-1，则表示没有找到
        int index = getPositionForGroupFooter(groupPosition);
        if (index >= 0) {
            GroupStructure structure = mStructures.get(groupPosition);
            notifyItemRemoved(index);
            if (getItemCount() - index > 0){
                notifyItemRangeChanged(index, getItemCount() - index);
            }
            //这个地方是将footer给移除
            structure.setHasFooter(false);
        }
    }

    /**
     * Use {@link #notifyChildRemoved(int, int)} instead.
     *
     * @param groupPosition
     * @param childPosition
     */
    @Deprecated
    public void removeChild(int groupPosition, int childPosition) {
        if (mStructures.size()>groupPosition){
            notifyChildRemoved(groupPosition, childPosition);
        }
    }

    /**
     * 通知一组里的某个子项删除
     *
     * @param groupPosition
     * @param childPosition
     */
    public void notifyChildRemoved(int groupPosition, int childPosition) {
        int index = getPositionForChild(groupPosition, childPosition);
        if (index >= 0) {
            GroupStructure structure = mStructures.get(groupPosition);
            notifyItemRemoved(index);
            notifyItemRangeChanged(index, getItemCount() - index);
            structure.setChildrenCount(structure.getChildrenCount() - 1);
        }
    }

    /**
     * Use {@link #notifyChildRangeRemoved(int, int, int)} instead.
     *
     * @param groupPosition
     * @param childPosition
     * @param count
     */
    @Deprecated
    public void removeRangeChild(int groupPosition, int childPosition, int count) {
        if (mStructures.size()>groupPosition){
            notifyChildRangeRemoved(groupPosition, childPosition, count);
        }
    }

    /**
     * 通知一组里的多个子项删除
     *
     * @param groupPosition
     * @param childPosition
     * @param count
     */
    public void notifyChildRangeRemoved(int groupPosition, int childPosition, int count) {
        if (groupPosition < mStructures.size()) {
            int index = getPositionForChild(groupPosition, childPosition);
            if (index >= 0) {
                GroupStructure structure = mStructures.get(groupPosition);
                int childCount = structure.getChildrenCount();
                int removeCount = count;
                if (childCount < childPosition + count) {
                    removeCount = childCount - childPosition;
                }
                notifyItemRangeRemoved(index, removeCount);
                notifyItemRangeChanged(index, getItemCount() - removeCount);
                structure.setChildrenCount(childCount - removeCount);
            }
        }
    }

    /**
     * Use {@link #notifyChildrenRemoved(int)} instead.
     *
     * @param groupPosition
     */
    @Deprecated
    public void removeChildren(int groupPosition) {
        if (mStructures.size()>groupPosition){
            notifyChildrenRemoved(groupPosition);
        }
    }

    /**
     * 通知一组里的所有子项删除
     *
     * @param groupPosition
     */
    public void notifyChildrenRemoved(int groupPosition) {
        if (groupPosition < mStructures.size()) {
            int index = getPositionForChild(groupPosition, 0);
            if (index >= 0) {
                GroupStructure structure = mStructures.get(groupPosition);
                int itemCount = structure.getChildrenCount();
                notifyItemRangeRemoved(index, itemCount);
                notifyItemRangeChanged(index, getItemCount() - itemCount);
                structure.setChildrenCount(0);
            }
        }
    }

    //****** 插入操作 *****//

    /**
     * Use {@link #notifyGroupInserted(int)} instead.
     *
     * @param groupPosition
     */
    @Deprecated
    public void insertGroup(int groupPosition) {
        notifyGroupInserted(groupPosition);
    }

    /**
     * 通知一组数据插入
     *
     * @param groupPosition
     */
    public void notifyGroupInserted(int groupPosition) {
        GroupStructure structure = new GroupStructure(hasHeader(groupPosition),
                hasFooter(groupPosition), getChildrenCount(groupPosition));
        if (groupPosition < mStructures.size()) {
            mStructures.add(groupPosition, structure);
        } else {
            mStructures.add(structure);
            groupPosition = mStructures.size() - 1;
        }

        int index = countGroupRangeItem(0, groupPosition);
        int itemCount = countGroupItem(groupPosition);
        if (itemCount > 0) {
            notifyItemRangeInserted(index, itemCount);
            notifyItemRangeChanged(index + itemCount, getItemCount() - index);
        }
    }

    /**
     * Use {@link #notifyGroupRangeInserted(int, int)} instead.
     *
     * @param groupPosition
     * @param count
     */
    @Deprecated
    public void insertRangeGroup(int groupPosition, int count) {
        notifyGroupRangeInserted(groupPosition, count);
    }

    /**
     * 通知多组数据插入
     *
     * @param groupPosition
     * @param count
     */
    public void notifyGroupRangeInserted(int groupPosition, int count) {
        ArrayList<GroupStructure> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GroupStructure structure = new GroupStructure(hasHeader(i), hasFooter(i), getChildrenCount(i));
            list.add(structure);
        }

        if (groupPosition < mStructures.size()) {
            mStructures.addAll(groupPosition, list);
        } else {
            mStructures.addAll(list);
            groupPosition = mStructures.size() - list.size();
        }

        int index = countGroupRangeItem(0, groupPosition);
        int itemCount = countGroupRangeItem(groupPosition, count);
        if (itemCount > 0) {
            notifyItemRangeInserted(index, itemCount);
            notifyItemRangeChanged(index + itemCount, getItemCount() - index);
        }
    }

    /**
     * Use {@link #notifyHeaderInserted(int)} instead.
     *
     * @param groupPosition
     */
    @Deprecated
    public void insertHeader(int groupPosition) {
        notifyHeaderInserted(groupPosition);
    }

    /**
     * 通知组头插入
     *
     * @param groupPosition
     */
    public void notifyHeaderInserted(int groupPosition) {
        if (groupPosition < mStructures.size() && 0 > getPositionForGroupHeader(groupPosition)) {
            GroupStructure structure = mStructures.get(groupPosition);
            structure.setHasHeader(true);
            int index = countGroupRangeItem(0, groupPosition);
            notifyItemInserted(index);
            notifyItemRangeChanged(index + 1, getItemCount() - index);
        }
    }

    /**
     * Use {@link #notifyFooterInserted(int)} instead.
     *
     * @param groupPosition
     */
    @Deprecated
    public void insertFooter(int groupPosition) {
        notifyFooterInserted(groupPosition);
    }

    /**
     * 通知组尾插入
     *
     * @param groupPosition
     */
    public void notifyFooterInserted(int groupPosition) {
        if (groupPosition < mStructures.size() && 0 > getPositionForGroupFooter(groupPosition)) {
            GroupStructure structure = mStructures.get(groupPosition);
            structure.setHasFooter(true);
            int index = countGroupRangeItem(0, groupPosition + 1);
            notifyItemInserted(index);
            notifyItemRangeChanged(index + 1, getItemCount() - index);
        }
    }

    /**
     * Use {@link #notifyChildInserted(int, int)} instead.
     *
     * @param groupPosition
     * @param childPosition
     */
    @Deprecated
    public void insertChild(int groupPosition, int childPosition) {
        notifyChildInserted(groupPosition, childPosition);
    }

    /**
     * 通知一个子项到组里插入
     *
     * @param groupPosition
     * @param childPosition
     */
    public void notifyChildInserted(int groupPosition, int childPosition) {
        if (groupPosition < mStructures.size()) {
            GroupStructure structure = mStructures.get(groupPosition);
            int index = getPositionForChild(groupPosition, childPosition);
            if (index < 0) {
                index = countGroupRangeItem(0, groupPosition);
                index += structure.hasHeader() ? 1 : 0;
                index += structure.getChildrenCount();
            }
            structure.setChildrenCount(structure.getChildrenCount() + 1);
            notifyItemInserted(index);
            notifyItemRangeChanged(index + 1, getItemCount() - index);
        }
    }

    /**
     * Use {@link #notifyChildRangeInserted(int, int, int)} instead.
     *
     * @param groupPosition
     * @param childPosition
     * @param count
     */
    @Deprecated
    public void insertRangeChild(int groupPosition, int childPosition, int count) {
        notifyChildRangeInserted(groupPosition, childPosition, count);
    }

    /**
     * 通知一组里的多个子项插入
     *
     * @param groupPosition
     * @param childPosition
     * @param count
     */
    public void notifyChildRangeInserted(int groupPosition, int childPosition, int count) {
        //判断组的索引要小于组的长度，避免索引越界异常
        if (groupPosition < mStructures.size()) {
            //计算多个组的项的总和
            int index = countGroupRangeItem(0, groupPosition);
            //获取组的数据
            GroupStructure structure = mStructures.get(groupPosition);
            //判断是否组的头部header
            if (structure.hasHeader()) {
                index++;
            }
            //获取组中所有孩子的数量
            if (childPosition < structure.getChildrenCount()) {
                index += childPosition;
            } else {
                index += structure.getChildrenCount();
            }
            //count指的是插入数据的数量
            if (count > 0) {
                //设置所有孩子的数量
                structure.setChildrenCount(structure.getChildrenCount() + count);
                //插入数据刷新
                notifyItemRangeInserted(index, count);
                //保证刷新itemCount的数量必须大于0
                if (getItemCount() - index > 0){
                    notifyItemRangeChanged(index + count, getItemCount() - index);
                }

            }
        }
    }

    /**
     * Use {@link #notifyChildrenInserted(int)} instead.
     *
     * @param groupPosition
     */
    @Deprecated
    public void insertChildren(int groupPosition) {
        notifyChildrenInserted(groupPosition);
    }

    /**
     * 通知一组里的所有子项插入
     *
     * @param groupPosition
     */
    public void notifyChildrenInserted(int groupPosition) {
        if (groupPosition < mStructures.size()) {
            int index = countGroupRangeItem(0, groupPosition);
            GroupStructure structure = mStructures.get(groupPosition);
            if (structure.hasHeader()) {
                index++;
            }
            int itemCount = getChildrenCount(groupPosition);
            if (itemCount > 0) {
                structure.setChildrenCount(itemCount);
                notifyItemRangeInserted(index, itemCount);
                if (getItemCount() - index > 0){
                    notifyItemRangeChanged(index + itemCount, getItemCount() - index);
                }
            }
        }
    }

    //****** 设置点击事件 *****//

    /**
     * 设置组头点击事件
     *
     * @param listener
     */
    public void setOnHeaderClickListener(OnHeaderClickListener listener) {
        mOnHeaderClickListener = listener;
    }

    /**
     * 设置组尾点击事件
     *
     * @param listener
     */
    public void setOnFooterClickListener(OnFooterClickListener listener) {
        mOnFooterClickListener = listener;
    }

    /**
     * 设置子项点击事件
     *
     * @param listener
     */
    public void setOnChildClickListener(OnChildClickListener listener) {
        mOnChildClickListener = listener;
    }

    /*-----------------------------------下面这些是抽象方法-----------------------------------------*/

    public abstract int getGroupCount();

    public abstract int getChildrenCount(int groupPosition);

    public abstract boolean hasHeader(int groupPosition);

    public abstract boolean hasFooter(int groupPosition);

    public abstract int getHeaderLayout(int viewType);

    public abstract int getFooterLayout(int viewType);

    public abstract int getChildLayout(int viewType);

    public abstract void onBindHeaderViewHolder(GroupViewHolder holder, int groupPosition);

    public abstract void onBindFooterViewHolder(GroupViewHolder holder, int groupPosition);

    public abstract void onBindChildViewHolder(GroupViewHolder holder,
                                               int groupPosition, int childPosition);

    class GroupDataObserver extends RecyclerView.AdapterDataObserver {

        @Override
        public void onChanged() {
            isDataChanged = true;
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            isDataChanged = true;
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            onItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            isDataChanged = true;
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            isDataChanged = true;
        }
    }




}