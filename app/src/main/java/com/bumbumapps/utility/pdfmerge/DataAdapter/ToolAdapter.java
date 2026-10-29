package com.bumbumapps.utility.pdfmerge.DataAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumbumapps.utility.pdfmerge.R;

/** Home screen tool tiles. */
public class ToolAdapter extends RecyclerView.Adapter<ToolAdapter.Holder> {

    public static final class Tool {
        public final int id, icon, label, bg, fg;

        public Tool(int id, int icon, int label, int bg, int fg) {
            this.id = id;
            this.icon = icon;
            this.label = label;
            this.bg = bg;
            this.fg = fg;
        }
    }

    public interface Listener {
        void onTool(Tool tool);
    }

    private final Tool[] tools;
    private final Listener listener;

    public ToolAdapter(Tool[] tools, Listener listener) {
        this.tools = tools;
        this.listener = listener;
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;

        Holder(View v) {
            super(v);
            icon = v.findViewById(R.id.toolIcon);
            label = v.findViewById(R.id.toolLabel);
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tool, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Tool t = tools[position];
        h.icon.setImageResource(t.icon);
        android.content.Context c = h.itemView.getContext();
        h.icon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(c, t.bg)));
        h.icon.setImageTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(c, t.fg)));
        h.label.setText(t.label);
        h.itemView.setOnClickListener(v -> listener.onTool(t));
    }

    @Override
    public int getItemCount() {
        return tools.length;
    }
}
