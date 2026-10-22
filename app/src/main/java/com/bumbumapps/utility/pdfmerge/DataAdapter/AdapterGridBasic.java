package com.bumbumapps.utility.pdfmerge.DataAdapter;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumbumapps.utility.pdfmerge.R;
import com.bumbumapps.utility.pdfmerge.Utility.ItemTouchHelperClass;
import com.bumbumapps.utility.pdfmerge.Utility.PDFPage;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Page grid of the page organizer. Selection is tracked per page object, so it survives moves. */
public class AdapterGridBasic extends RecyclerView.Adapter<AdapterGridBasic.Holder>
        implements ItemTouchHelperClass.ItemTouchHelperAdapter {

    public interface Renderer {
        /** Renders page {@code index} (0-based) and delivers it on the main thread. */
        void render(int index, Callback cb);

        interface Callback {
            void done(Bitmap bitmap);
        }
    }

    public interface Listener {
        void onRotate(PDFPage page, int position, int degrees);

        void onDelete(PDFPage page, int position);

        void onClick(PDFPage page, int position);

        void onLongClick(PDFPage page, int position);

        void onChanged();
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public final ArrayList<PDFPage> items;
    private final Set<PDFPage> selected = new LinkedHashSet<>();
    private final Renderer renderer;
    private Listener listener;
    private OnStartDragListener dragListener;

    public AdapterGridBasic(ArrayList<PDFPage> items, Renderer renderer) {
        this.items = items;
        this.renderer = renderer;
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void setDragListener(OnStartDragListener l) {
        dragListener = l;
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) Collections.swap(items, i, i + 1);
        } else {
            for (int i = fromPosition; i > toPosition; i--) Collections.swap(items, i, i - 1);
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemRemoved(int position) {
    }

    @Override
    public void onDragFinished() {
        notifyItemRangeChanged(0, items.size(), "order");
        if (listener != null) listener.onChanged();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView image;
        final TextView pageIndex;
        final ImageButton left, right, delete, move;
        PDFPage bound;

        Holder(View v) {
            super(v);
            card = v.findViewById(R.id.lyt_parent);
            image = v.findViewById(R.id.image);
            pageIndex = v.findViewById(R.id.pageIndex);
            left = v.findViewById(R.id.rotatLeft);
            right = v.findViewById(R.id.rotateRight);
            delete = v.findViewById(R.id.delete);
            move = v.findViewById(R.id.extramenu);
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_grid_editor, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            PDFPage page = items.get(position);
            h.pageIndex.setText(String.valueOf(position + 1));
            h.card.setChecked(selected.contains(page));
            if (payloads.contains("rot")) {
                float current = h.image.getRotation();
                float target = page.getRotation();
                while (target - current > 180) target -= 360;
                while (current - target > 180) target += 360;
                h.image.animate().rotation(target).setDuration(180).start();
            }
            return;
        }
        super.onBindViewHolder(h, position, payloads);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final PDFPage page = items.get(position);
        h.bound = page;
        h.pageIndex.setText(String.valueOf(position + 1));
        h.card.setContentDescription(h.itemView.getContext().getString(R.string.page_number, page.getIndex() + 1));
        h.card.setChecked(selected.contains(page));
        h.image.setRotation(page.getRotation());
        h.image.setImageDrawable(null);
        renderer.render(page.getIndex(), bmp -> {
            if (h.bound == page) h.image.setImageBitmap(bmp);
        });
        h.card.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (listener != null && p != RecyclerView.NO_POSITION) listener.onClick(items.get(p), p);
        });
        h.card.setOnLongClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (listener == null || p == RecyclerView.NO_POSITION) return false;
            listener.onLongClick(items.get(p), p);
            return true;
        });
        h.left.setOnClickListener(v -> rotateClick(h, -90));
        h.right.setOnClickListener(v -> rotateClick(h, 90));
        h.delete.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (listener != null && p != RecyclerView.NO_POSITION) listener.onDelete(items.get(p), p);
        });
        h.move.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN && dragListener != null) dragListener.onStartDrag(h);
            return false;
        });
    }

    private void rotateClick(Holder h, int degrees) {
        int p = h.getBindingAdapterPosition();
        if (listener == null || p == RecyclerView.NO_POSITION) return;
        listener.onRotate(items.get(p), p, degrees);
        // Animate relative to the current view rotation for a smooth turn.
        h.image.animate().rotationBy(degrees).setDuration(180).start();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void toggleSelection(int pos) {
        PDFPage p = items.get(pos);
        if (!selected.remove(p)) selected.add(p);
        notifyItemChanged(pos, "sel");
    }

    public void selectWhere(java.util.function.IntPredicate positionTest) {
        for (int i = 0; i < items.size(); i++) if (positionTest.test(i)) selected.add(items.get(i));
        notifyItemRangeChanged(0, items.size(), "sel");
    }

    public void clearSelections() {
        selected.clear();
        notifyItemRangeChanged(0, items.size(), "sel");
    }

    public int getSelectedItemCount() {
        return selected.size();
    }

    public List<PDFPage> getSelectedPages() {
        return new ArrayList<>(selected);
    }

    public void forget(PDFPage page) {
        selected.remove(page);
    }
}
