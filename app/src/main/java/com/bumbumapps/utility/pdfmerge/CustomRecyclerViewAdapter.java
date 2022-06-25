package com.bumbumapps.utility.pdfmerge;

import android.graphics.Color;

import androidx.core.view.MotionEventCompat;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumbumapps.utility.pdfmerge.Utility.ItemTouchHelperClass;
import com.bumbumapps.utility.pdfmerge.Utility.PDFDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CustomRecyclerViewAdapter extends RecyclerView.Adapter<CustomRecyclerViewAdapter.MyViewHolder> implements ItemTouchHelperClass.ItemTouchHelperAdapter {
    public List<PDFDocument> mContacts;
    private OnClickListener onClickListener = null;
    private OnStartDragListener mDragStartListener = null;
    private SparseBooleanArray selected_items;
    private int current_selected_idx = -1;

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setDragListener(OnStartDragListener dragStartListener) {
        this.mDragStartListener = dragStartListener;
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mContacts, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mContacts, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemRemoved(int position) {
        mContacts.get(position).deleteFile();
        mContacts.remove(position);
        notifyItemRemoved(position);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public TextView secondaryText;
        public ImageView imageView;
        public View lyt_parent;
        public AppCompatImageButton bt_move;

        public MyViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.fileItemTextview);
            secondaryText = (TextView) v.findViewById(R.id.sizeItemTimeTextView);
            imageView = (ImageView) v.findViewById(R.id.fileImageView);
            lyt_parent = (View) v.findViewById(R.id.listItemLinearLayout);
            bt_move = (AppCompatImageButton) v.findViewById(R.id.itemSelector);
        }
    }

    public CustomRecyclerViewAdapter(List<PDFDocument> myDataset) {
        mContacts = myDataset;
        selected_items = new SparseBooleanArray();
    }

    @Override
    public CustomRecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                                     int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }


    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        if (mContacts.size() > 0) {
            final PDFDocument item = mContacts.get(position);
            if (item != null) {
                TextView textView = holder.mTextView;
                String fileName = item.getFileName();
                if (fileName != null)
                    textView.setText(fileName);
                String size = GetSize(item.getSize());
                if (size != null)
                    holder.secondaryText.setText(size);
                if (fileName != null && !fileName.equals("")) {
                    int index = fileName.lastIndexOf(".");
                    if (index >= 0) {
                        String extension = fileName.substring(index);
                        switch (extension.toLowerCase()) {
                            case ".jpeg":
                            case ".jpg":
                                holder.imageView.setImageResource(R.drawable.ic_jpeg);
                                break;
                            case ".png":
                                holder.imageView.setImageResource(R.drawable.ic_png);
                                break;
                            case ".pdf":
                                holder.imageView.setImageResource(R.drawable.ic_iconfinder_24_2133056);
                                break;
                        }
                    }
                }
            }
            holder.lyt_parent.setActivated(selected_items.get(position, false));
            holder.lyt_parent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onClickListener == null) return;
                    onClickListener.onItemClick(v, item, holder.getAdapterPosition());
                }
            });

            holder.lyt_parent.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (onClickListener == null) return false;
                    onClickListener.onItemLongClick(v, item, holder.getAdapterPosition());
                    return true;
                }
            });

            holder.bt_move.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN && mDragStartListener != null) {
                        mDragStartListener.onStartDrag(holder);
                    }
                    return false;
                }
            });
            toggleCheckedIcon(holder, position);
        }
    }

    public  String GetSize(long bytes) {
        boolean si = true;
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public int getSelectedItemCount() {
        return selected_items.size();
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

    public void clearSelections() {
        selected_items.clear();
        notifyDataSetChanged();
    }

    public PDFDocument getItem(int position) {
        return mContacts.get(position);
    }

    private void toggleCheckedIcon(MyViewHolder holder, int position) {
        if (selected_items.get(position, false)) {
            holder.lyt_parent.setBackgroundColor(Color.parseColor("#455A64"));
            if (current_selected_idx == position) resetCurrentIndex();
        } else {
            holder.lyt_parent.setBackgroundColor(Color.parseColor("#373553"));
            if (current_selected_idx == position) resetCurrentIndex();
        }
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }


    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<>(selected_items.size());
        for (int i = 0; i < selected_items.size(); i++) {
            items.add(selected_items.keyAt(i));
        }
        return items;
    }

    public void removeData(int position) {
        mContacts.remove(position);
        resetCurrentIndex();
    }

    public void selectAll() {
        for (int i = 0; i < mContacts.size(); i++) {
            selected_items.put(i, true);
            notifyItemChanged(i);
        }
    }

    private void resetCurrentIndex() {
        current_selected_idx = -1;
    }

    //On draw helper interface
    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    //Onclick helper interface
    public interface OnClickListener {
        void onItemClick(View view, PDFDocument obj, int pos);

        void onItemLongClick(View view, PDFDocument obj, int pos);
    }
}

