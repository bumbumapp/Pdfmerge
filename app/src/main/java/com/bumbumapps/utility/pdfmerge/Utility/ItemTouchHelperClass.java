package com.bumbumapps.utility.pdfmerge.Utility;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

public class ItemTouchHelperClass extends ItemTouchHelper.Callback {

    private ItemTouchHelperAdapter adapter;
    public static boolean isItemSwipe = true;
    int upFlags = 0;
    int swipeFlags = 0;

    public interface ItemTouchHelperAdapter {
        void onItemMoved(int fromPosition, int toPosition);

        void onItemRemoved(int position);
    }

    public ItemTouchHelperClass(ItemTouchHelperAdapter ad, boolean value) {
        adapter = ad;
        if (value) {
            upFlags = ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.START | ItemTouchHelper.END;
            isItemSwipe = false;
        } else {
            upFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            isItemSwipe = true;
        }
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return isItemSwipe;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(upFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        adapter.onItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        adapter.onItemRemoved(viewHolder.getAdapterPosition());
    }
}