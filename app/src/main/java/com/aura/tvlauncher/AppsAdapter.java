package com.aura.tvlauncher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.AppViewHolder> {

    public interface OnFavoriteChangeListener {
        void onFavoriteChanged(AppInfo app);
    }

    private final Context context;
    private final List<AppInfo> apps = new ArrayList<>();
    private final OnFavoriteChangeListener favoriteChangeListener;

    public AppsAdapter(Context context, OnFavoriteChangeListener listener) {
        this.context = context;
        this.favoriteChangeListener = listener;
    }

    public void setApps(List<AppInfo> newApps) {
        apps.clear();
        if (newApps != null) {
            apps.addAll(newApps);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app_card, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = apps.get(position);
        holder.tvAppName.setText(app.getLabel());
        holder.ivAppIcon.setImageDrawable(app.getIcon());

        // Focus animation for TV Remote D-Pad
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.animate().scaleX(1.08f).scaleY(1.08f).translationZ(10f).setDuration(150).start();
                holder.tvAppName.setSelected(true); // marquee if text is long
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(150).start();
                holder.tvAppName.setSelected(false);
            }
        });

        // Click to launch app
        holder.itemView.setOnClickListener(v -> {
            try {
                Intent launchIntent = context.getPackageManager().getLeanbackLaunchIntentForPackage(app.getPackageName());
                if (launchIntent == null) {
                    launchIntent = context.getPackageManager().getLaunchIntentForPackage(app.getPackageName());
                }
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(launchIntent);
                } else {
                    Toast.makeText(context, "Kan app niet openen: " + app.getLabel(), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(context, "Fout bij starten app: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Long click to show Context Menu
        holder.itemView.setOnLongClickListener(v -> {
            showAppContextMenu(app);
            return true;
        });
    }

    private void showAppContextMenu(AppInfo app) {
        String favAction = app.isFavorite() ? "Verwijder uit Favorieten" : "Voeg toe aan Favorieten";
        String[] options = {favAction, "App Info & Geheugen", "App Deïnstalleren"};

        new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(app.getLabel())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Toggle favorite
                            app.setFavorite(!app.isFavorite());
                            if (favoriteChangeListener != null) {
                                favoriteChangeListener.onFavoriteChanged(app);
                            }
                            notifyDataSetChanged();
                            break;
                        case 1: // App info
                            try {
                                Intent infoIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                infoIntent.setData(Uri.parse("package:" + app.getPackageName()));
                                infoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(infoIntent);
                            } catch (Exception e) {
                                Toast.makeText(context, "Kon App Info niet openen", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 2: // Uninstall
                            try {
                                Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                                uninstallIntent.setData(Uri.parse("package:" + app.getPackageName()));
                                uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(uninstallIntent);
                            } catch (Exception e) {
                                Toast.makeText(context, "Kon deïnstallatie niet starten", Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivAppIcon;
        final TextView tvAppName;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.iv_app_icon);
            tvAppName = itemView.findViewById(R.id.tv_app_name);
        }
    }
}
