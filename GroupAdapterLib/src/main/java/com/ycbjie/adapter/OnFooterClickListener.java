package com.ycbjie.adapter;

/**
 * <pre>
 *     @author yangchong
 *     blog  : https://github.com/yangchong211
 *     time  : 2019/9/18
 *     desc  : footer点击事件
 *     revise: https://github.com/yangchong211/YCGroupAdapter
 * </pre>
 */
public interface OnFooterClickListener {
    /**
     * footer点击事件
     * @param adapter                           adapter适配器
     * @param holder                            holder
     * @param groupPosition                     group索引
     */
    void onFooterClick(AbsGroupedAdapter adapter, GroupViewHolder holder, int groupPosition);
}
