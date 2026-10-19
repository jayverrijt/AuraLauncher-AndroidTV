package com.aura.tvlauncher;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TvProviderAdapter extends RecyclerView.Adapter<TvProviderAdapter.ProviderViewHolder> {

    public interface OnProviderClickListener {
        void onProviderSelected(TvProvider provider);
        void onProviderDefaultChanged(TvProvider provider);
    }

    private final Context context;
    private final List<TvProvider> providers = new ArrayList<>();
    private final OnProviderClickListener listener;
    private String defaultProviderId;

    public TvProviderAdapter(Context context, List<TvProvider> list, String defaultId, OnProviderClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.defaultProviderId = defaultId;
        if (list != null) {
            this.providers.addAll(list);
        }
    }

    public void setDefaultProviderId(String id) {
        this.defaultProviderId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProviderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_provider_card, parent, false);
        return new ProviderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ProviderViewHolder holder, int position) {
        TvProvider provider = providers.get(position);

        holder.tvName.setText(provider.getName());
        holder.tvTagline.setText(provider.getTagline());
        holder.tvIcon.setText(provider.getIconEmoji());
        holder.badgeTag.setText(provider.getBadgeText());

        try {
            int parsedColor = Color.parseColor(provider.getAccentColor());
            holder.cardIconBg.setCardBackgroundColor(parsedColor);
            holder.badgeTag.setTextColor(parsedColor);
        } catch (Exception ignored) {}

        boolean isDefault = provider.getId().equalsIgnoreCase(defaultProviderId);
        holder.llDefaultBadge.setVisibility(isDefault ? View.VISIBLE : View.GONE);

        // Remote focus effect
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.animate().scaleX(1.04f).scaleY(1.04f).translationZ(8f).setDuration(120).start();
                holder.tvName.setSelected(true);
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(120).start();
                holder.tvName.setSelected(false);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProviderSelected(provider);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            defaultProviderId = provider.getId();
            TvProvider.setDefaultProvider(context, provider.getId());
            notifyDataSetChanged();
            Toast.makeText(context, "★ " + provider.getName() + " ingesteld als standaard TV provider", Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onProviderDefaultChanged(provider);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return providers.size();
    }

    public static class ProviderViewHolder extends RecyclerView.ViewHolder {
        final CardView cardIconBg;
        final TextView tvIcon;
        final TextView tvName;
        final TextView tvTagline;
        final TextView badgeTag;
        final LinearLayout llDefaultBadge;

        public ProviderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardIconBg = itemView.findViewById(R.id.card_icon_bg);
            tvIcon = itemView.findViewById(R.id.tv_provider_icon);
            tvName = itemView.findViewById(R.id.tv_provider_name);
            tvTagline = itemView.findViewById(R.id.tv_provider_tagline);
            badgeTag = itemView.findViewById(R.id.badge_provider_tag);
            llDefaultBadge = itemView.findViewById(R.id.ll_default_badge);
        }
    }
}
