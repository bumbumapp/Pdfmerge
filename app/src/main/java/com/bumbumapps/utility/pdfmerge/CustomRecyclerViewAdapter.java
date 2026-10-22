package com.bumbumapps.utility.pdfmerge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumbumapps.utility.pdfmerge.Ui.Thumbs;
import com.bumbumapps.utility.pdfmerge.Utility.ItemTouchHelperClass;
import com.bumbumapps.utility.pdfmerge.Utility.PDFDocument;
import com.bumbumapps.utility.pdfmerge.Utility.PdfNames;
import com.bumptech.glide.Glide;

import java.util.Collections;
import java.util.List;

/** Files queued for merging: order badge, thumbnail, name, meta, remove and drag handle. */
public class CustomRecyclerViewAdapter extends RecyclerView.Adapter<CustomRecyclerViewAdapter.MyViewHolder>
        implements ItemTouchHelperClass.ItemTouchHelperAdapter {

    public interface OnClickListener {
        void onItemClick(View view, PDFDocument obj, int pos);

        void onRemove(PDFDocument obj, int pos);
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public final List<PDFDocument> mContacts;
    private OnClickListener onClickListener;
    private OnStartDragListener mDragStartListener;
    private Runnable onOrderChanged;

    public CustomRecyclerViewAdapter(List<PDFDocument> data) {
        mContacts = data;
    }

    public void setOnClickListener(OnClickListener l) {
        onClickListener = l;
    }

    public void setDragListener(OnStartDragListener l) {
        mDragStartListener = l;
    }

    public void setOnOrderChanged(Runnable r) {
        onOrderChanged = r;
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        if (fromPosition < 0 || toPosition < 0) return;
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) Collections.swap(mContacts, i, i + 1);
        } else {
            for (int i = fromPosition; i > toPosition; i--) Collections.swap(mContacts, i, i - 1);
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    /** Swipe to remove. */
    @Override
    public void onItemRemoved(int position) {
        if (position < 0 || position >= mContacts.size()) return;
        if (onClickListener != null) onClickListener.onRemove(mContacts.get(position), position);
    }

    /** Re-numbers the badges after a drag finished. */
    @SuppressLint("NotifyDataSetChanged")
    public void onDragFinished() {
        notifyItemRangeChanged(0, mContacts.size(), "order");
        if (onOrderChanged != null) onOrderChanged.run();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        final TextView name, meta, badge;
        final ImageView thumb;
        final ImageButton remove, drag;

        MyViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.fileItemTextview);
            meta = v.findViewById(R.id.sizeItemTimeTextView);
            thumb = v.findViewById(R.id.fileImageView);
            badge = v.findViewById(R.id.orderBadge);
            remove = v.findViewById(R.id.removeButton);
            drag = v.findViewById(R.id.itemSelector);
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder h, int position, @NonNull List<Object> payloads) {
        if (payloads.contains("order")) {
            h.badge.setText(String.valueOf(position + 1));
            return;
        }
        super.onBindViewHolder(h, position, payloads);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder h, int position) {
        final PDFDocument item = mContacts.get(position);
        final Context c = h.itemView.getContext();
        h.badge.setText(String.valueOf(position + 1));
        h.name.setText(item.getFileName());
        final String size = PdfNames.readableSize(item.getSize());
        h.meta.setText(size);
        Glide.with(c).clear(h.thumb);
        if (item.isPdf()) {
            h.thumb.setScaleType(ImageView.ScaleType.CENTER);
            h.thumb.setImageResource(R.drawable.ic_m_pdf);
            int w = (int) (96 * c.getResources().getDisplayMetrics().density);
            Object src = item.source();
            String key = String.valueOf(src) + "|" + item.getSize();
            Thumbs.load(c, h.thumb, src, key, w, info -> {
                if (info.bitmap != null) {
                    h.thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    h.thumb.setImageBitmap(info.bitmap);
                } else if (info.locked) {
                    h.thumb.setImageResource(R.drawable.ic_m_lock);
                }
                String pages = info.locked ? c.getString(R.string.locked)
                        : info.pages > 0 ? c.getResources().getQuantityString(R.plurals.page_count, info.pages, info.pages) : null;
                h.meta.setText(pages == null ? "PDF · " + size : "PDF · " + pages + " · " + size);
            });
        } else {
            h.thumb.setTag(R.id.thumb_key, null);
            h.thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(c).load(item.source()).centerCrop().placeholder(R.drawable.ic_m_image).into(h.thumb);
            h.meta.setText(c.getString(R.string.meta_sep, c.getString(R.string.image_label), size));
        }
        h.itemView.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (onClickListener != null && p != RecyclerView.NO_POSITION) onClickListener.onItemClick(v, mContacts.get(p), p);
        });
        h.remove.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (onClickListener != null && p != RecyclerView.NO_POSITION) onClickListener.onRemove(mContacts.get(p), p);
        });
        h.drag.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && mDragStartListener != null) {
                mDragStartListener.onStartDrag(h);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }

    public PDFDocument getItem(int position) {
        return mContacts.get(position);
    }
}
