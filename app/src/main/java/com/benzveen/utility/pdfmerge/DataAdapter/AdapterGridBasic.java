package com.benzveen.utility.pdfmerge.DataAdapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.benzveen.utility.pdfmerge.R;
import com.benzveen.utility.pdfmerge.Utility.ItemTouchHelperClass;
import com.benzveen.utility.pdfmerge.Utility.PDFPage;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdapterGridBasic extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemTouchHelperClass.ItemTouchHelperAdapter {

    public ArrayList<PDFPage> items;
    private SparseBooleanArray selected_items;
    private int current_selected_idx = -1;
    private OnStartDragListener mDragStartListener = null;

    private Context ctx;
    private OnItemClickListener mOnItemClickListener;

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(items, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(items, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemRemoved(int position) {

    }

    public interface OnItemClickListener {
        void onItemClick(View view, PDFPage obj, int position);

        void onItemLongClick(View view, PDFPage obj, int pos);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    public void setDragListener(OnStartDragListener dragStartListener) {
        this.mDragStartListener = dragStartListener;
    }

    public AdapterGridBasic(Context context, ArrayList<PDFPage> items) {
        this.items = items;
        ctx = context;
        selected_items = new SparseBooleanArray();
    }

    public class OriginalViewHolder extends RecyclerView.ViewHolder {
        public ImageView image;
        public View lyt_parent;
        public AppCompatImageButton bt_move;
        public AppCompatImageButton bt_rotateLeft;
        public AppCompatImageButton bt_rotateRight;
        public AppCompatImageButton bt_delete;
        public TextView pageIndex;

        public OriginalViewHolder(View v) {
            super(v);
            image = v.findViewById(R.id.image);
            lyt_parent = v.findViewById(R.id.lyt_parent);
            bt_move = v.findViewById(R.id.extramenu);
            pageIndex = v.findViewById(R.id.pageIndex);
            bt_rotateLeft = v.findViewById(R.id.rotatLeft);
            bt_rotateRight = v.findViewById(R.id.rotateRight);
            bt_delete = v.findViewById(R.id.delete);

        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_grid_editor, parent, false);
        vh = new OriginalViewHolder(v);
        return vh;
    }


    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof OriginalViewHolder) {
            final OriginalViewHolder view = (OriginalViewHolder) holder;
            final PDFPage page = items.get(position);
            Integer index = page.getIndex() + 1;
            view.pageIndex.setText(index.toString());

            new AsyncTask<Object, Object, Bitmap>(

            ) {
                @Override
                protected Bitmap doInBackground(Object[] objects) {
                    return page.getBitmap();
                }

                @Override
                protected void onPostExecute(Bitmap result) {
                    displayImageOriginal(ctx, view.image, result);
                }
            }.execute();


            view.image.setRotation(items.get(holder.getAdapterPosition()).getRotation());
            view.lyt_parent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(view, items.get(position), holder.getAdapterPosition());
                    }
                }
            });
            view.bt_rotateLeft.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(view, items.get(position), holder.getAdapterPosition());
                    }
                }
            });
            view.bt_rotateRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(view, items.get(position), holder.getAdapterPosition());
                    }
                }
            });
            view.bt_delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(view, items.get(position), holder.getAdapterPosition());
                    }
                }
            });
            view.lyt_parent.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mOnItemClickListener == null) return false;
                    mOnItemClickListener.onItemLongClick(v, items.get(position), holder.getAdapterPosition());
                    return true;
                }
            });
            view.bt_move.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN && mDragStartListener != null) {
                        mDragStartListener.onStartDrag(holder);
                    }
                    return false;
                }
            });
            toggleCheckedIcon(view, position);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void toggleCheckedIcon(OriginalViewHolder holder, int position) {
        if (selected_items.get(position, false)) {
            holder.lyt_parent.setBackgroundColor(Color.parseColor("#1DF44336"));
            if (current_selected_idx == position) resetCurrentIndex();
        } else {
            holder.lyt_parent.setBackgroundColor(Color.parseColor("#ffffff"));
            if (current_selected_idx == position) resetCurrentIndex();
        }
    }

    public void selectAll() {
        for (int i = 0; i < items.size(); i++) {
            selected_items.put(i, true);
            notifyItemChanged(i);
        }
    }

    public void selectEven() {
        for (int i = 0; i < items.size(); i++) {
            if ((i + 1) % 2 == 0) {
                selected_items.put(i, true);
                notifyItemChanged(i);
            }
        }
    }

    public void selectOdd() {
        for (int i = 0; i < items.size(); i++) {
            if ((i + 1) % 2 != 0) {
                selected_items.put(i, true);
                notifyItemChanged(i);
            }
        }
    }

    public void selectPotrait(Boolean value) {
        for (int i = 0; i < items.size(); i++) {
            PDFPage page = items.get(i);
            if (value) {
                if (page.isPortrait()) {
                    selected_items.put(i, true);
                    notifyItemChanged(i);
                }
            } else if (!page.isPortrait()) {
                selected_items.put(i, true);
                notifyItemChanged(i);
            }
        }
    }

    public void removeData(int position) {
        items.remove(position);
        resetCurrentIndex();
    }

    public void clearSelections() {
        selected_items.clear();
        notifyDataSetChanged();
    }

    public void toggleSelection(int pos) {
        current_selected_idx = pos;
        if (selected_items.get(pos, false)) {
            selected_items.delete(pos);
        } else {
            selected_items.put(pos, true);
        }
        notifyItemChanged(pos);
    }

    private void resetCurrentIndex() {
        current_selected_idx = -1;
    }

    public int getSelectedItemCount() {
        return selected_items.size();
    }

    public static void displayImageOriginal(Context ctx, ImageView img, Bitmap bitmap) {
        try {
            Glide.with(ctx).load(bitmap)
                    .diskCacheStrategy(DiskCacheStrategy.NONE).thumbnail()
                    .into(img);
        } catch (Exception e) {
        }
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<>(selected_items.size());
        for (int i = 0; i < selected_items.size(); i++) {
            items.add(selected_items.keyAt(i));
        }
        return items;
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public PDFPage getItem(int position) {
        return items.get(position);
    }
}