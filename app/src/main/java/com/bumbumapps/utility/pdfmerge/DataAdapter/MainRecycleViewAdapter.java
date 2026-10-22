package com.bumbumapps.utility.pdfmerge.DataAdapter;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumbumapps.utility.pdfmerge.R;
import com.bumbumapps.utility.pdfmerge.Ui.FileActions;
import com.bumbumapps.utility.pdfmerge.Ui.Thumbs;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** "My PDFs" list on the home screen: thumbnail, name, pages · size · date, overflow menu. */
public class MainRecycleViewAdapter extends RecyclerView.Adapter<MainRecycleViewAdapter.Holder> {

    public interface OnItemClickListener {
        void onItemClick(View view, File value, int position);

        void onItemLongClick(View view, File obj, int pos);
    }

    private final List<File> items;
    private final SparseBooleanArray selected = new SparseBooleanArray();
    private final Context ctx;
    private final int thumbWidth;
    private OnItemClickListener listener;

    public MainRecycleViewAdapter(Context context, List<File> items) {
        this.items = items;
        this.ctx = context;
        this.thumbWidth = (int) (96 * context.getResources().getDisplayMetrics().density);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    static class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView image;
        final TextView name;
        final TextView brief;
        final ImageButton more;

        Holder(View v) {
            super(v);
            card = v.findViewById(R.id.lyt_parent);
            image = v.findViewById(R.id.image);
            name = v.findViewById(R.id.name);
            brief = v.findViewById(R.id.brief);
            more = v.findViewById(R.id.extramenu);
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grid_image, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final File f = items.get(position);
        h.name.setText(f.getName());
        h.brief.setText(FileActions.meta(ctx, f, 0, false));
        h.image.setScaleType(ImageView.ScaleType.CENTER);
        h.image.setImageResource(R.drawable.ic_m_pdf);
        Thumbs.load(ctx, h.image, f, Thumbs.key(f), thumbWidth, info -> {
            if (info.bitmap != null) {
                h.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                h.image.setImageBitmap(info.bitmap);
            } else if (info.locked) {
                h.image.setImageResource(R.drawable.ic_m_lock);
            }
            h.brief.setText(FileActions.meta(ctx, f, info.pages, info.locked));
        });
        h.card.setChecked(selected.get(position, false));
        h.card.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (listener != null && p != RecyclerView.NO_POSITION) listener.onItemClick(v, items.get(p), p);
        });
        h.card.setOnLongClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (listener == null || p == RecyclerView.NO_POSITION) return false;
            listener.onItemLongClick(v, items.get(p), p);
            return true;
        });
        h.more.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (listener != null && p != RecyclerView.NO_POSITION) listener.onItemClick(v, items.get(p), p);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void toggleSelection(int pos) {
        if (selected.get(pos, false)) selected.delete(pos);
        else selected.put(pos, true);
        notifyItemChanged(pos);
    }

    public int getSelectedItemCount() {
        return selected.size();
    }

    public void selectAll() {
        for (int i = 0; i < items.size(); i++) selected.put(i, true);
        notifyItemRangeChanged(0, items.size());
    }

    @android.annotation.SuppressLint("NotifyDataSetChanged")
    public void clearSelections() {
        selected.clear();
        notifyDataSetChanged();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> out = new ArrayList<>(selected.size());
        for (int i = 0; i < selected.size(); i++) out.add(selected.keyAt(i));
        return out;
    }
}
