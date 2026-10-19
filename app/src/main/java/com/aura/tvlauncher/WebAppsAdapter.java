package com.aura.tvlauncher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class WebAppsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ADD = 0;
    private static final int TYPE_APP = 1;

    public interface OnWebAppActionListener {
        void onAddClicked();
        void onDeleted(WebAppInfo webApp);
    }

    private final Context context;
    private final List<WebAppInfo> webApps = new ArrayList<>();
    private final OnWebAppActionListener actionListener;

    public WebAppsAdapter(Context context, OnWebAppActionListener listener) {
        this.context = context;
        this.actionListener = listener;
    }

    public void setWebApps(List<WebAppInfo> list) {
        webApps.clear();
        if (list != null) {
            webApps.addAll(list);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_ADD : TYPE_APP;
    }

    @Override
    public int getItemCount() {
        return webApps.size() + 1; // +1 for the "+ Pinnen" card
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_add_web_card, parent, false);
            return new AddViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_web_card, parent, false);
            return new WebViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // TV remote focus scale animation
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.animate().scaleX(1.08f).scaleY(1.08f).translationZ(10f).setDuration(150).start();
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(150).start();
            }
        });

        if (holder instanceof AddViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onAddClicked();
                }
            });
        } else if (holder instanceof WebViewHolder) {
            WebViewHolder webHolder = (WebViewHolder) holder;
            WebAppInfo app = webApps.get(position - 1);

            webHolder.tvTitle.setText(app.getTitle());
            webHolder.tvUrl.setText(app.getDomain());
            webHolder.tvMonogram.setText(app.getLetter());

            // Monogram circle background
            try {
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(Color.parseColor(app.getColorHex()));
                webHolder.tvMonogram.setBackground(bg);
            } catch (Exception ignored) {
                // Fallback default color
            }

            // Click: Seamless Fullscreen TV Web Viewer!
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, WebActivity.class);
                intent.putExtra(WebActivity.EXTRA_URL, app.getUrl());
                intent.putExtra(WebActivity.EXTRA_TITLE, app.getTitle());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            });

            // Long click: Context menu (Delete or open in external browser)
            holder.itemView.setOnLongClickListener(v -> {
                showWebContextMenu(app);
                return true;
            });
        }
    }

    private void showWebContextMenu(WebAppInfo app) {
        String[] options = {
                "Openen in Fullscreen TV Viewer",
                "Openen in Externe Browser",
                "Verwijder Website Pinnen"
        };

        new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(app.getTitle())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Intent intent = new Intent(context, WebActivity.class);
                            intent.putExtra(WebActivity.EXTRA_URL, app.getUrl());
                            intent.putExtra(WebActivity.EXTRA_TITLE, app.getTitle());
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                            break;
                        case 1:
                            try {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(app.getUrl()));
                                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(browserIntent);
                            } catch (Exception e) {
                                Toast.makeText(context, "Geen externe browser gevonden", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 2:
                            if (actionListener != null) {
                                actionListener.onDeleted(app);
                            }
                            break;
                    }
                })
                .show();
    }

    static class AddViewHolder extends RecyclerView.ViewHolder {
        public AddViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class WebViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvUrl;
        final TextView tvMonogram;

        public WebViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_web_title);
            tvUrl = itemView.findViewById(R.id.tv_web_url);
            tvMonogram = itemView.findViewById(R.id.tv_monogram);
        }
    }
}
