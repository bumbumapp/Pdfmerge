package com.bumbumapps.utility.pdfmerge.Utility;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/** Drag to reorder (via handle) and optional swipe to remove, with a "lifted" look while dragging. */
public class ItemTouchHelperClass extends ItemTouchHelper.Callback {

    public interface ItemTouchHelperAdapter {
        void onItemMoved(int fromPosition, int toPosition);

        void onItemRemoved(int position);

        default void onDragFinished() {
        }
    }

    private final ItemTouchHelperAdapter adapter;
    private final int dragFlags;
    private final int swipeFlags;
    private boolean swipeEnabled;
    private boolean dragged;

    /** @param grid true for grids (drag in all directions, no swipe), false for lists (vertical drag + swipe). */
    public ItemTouchHelperClass(ItemTouchHelperAdapter ad, boolean grid) {
        adapter = ad;
        if (grid) {
            dragFlags = ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.START | ItemTouchHelper.END;
            swipeFlags = 0;
            swipeEnabled = false;
        } else {
            dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            swipeEnabled = true;
        }
    }

    public void setSwipeEnabled(boolean enabled) {
        swipeEnabled = enabled;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return swipeEnabled;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        int from = viewHolder.getBindingAdapterPosition();
        int to = target.getBindingAdapterPosition();
        if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false;
        adapter.onItemMoved(from, to);
        dragged = true;
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        adapter.onItemRemoved(viewHolder.getBindingAdapterPosition());
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            viewHolder.itemView.animate().scaleX(1.03f).scaleY(1.03f).translationZ(12f).setDuration(120).start();
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        // Reset directly: the item animator may cancel a running view animation on rebind.
        viewHolder.itemView.animate().cancel();
        viewHolder.itemView.setScaleX(1f);
        viewHolder.itemView.setScaleY(1f);
        viewHolder.itemView.setTranslationZ(0f);
        if (dragged) {
            dragged = false;
            adapter.onDragFinished();
        }
    }
}
