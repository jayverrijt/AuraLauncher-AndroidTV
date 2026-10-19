package com.aura.tvlauncher;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CastDeviceAdapter extends RecyclerView.Adapter<CastDeviceAdapter.ViewHolder> {

    public interface OnDeviceClickListener {
        void onDeviceClick(CastDevice device);
    }

    private final Context context;
    private final List<CastDevice> devices = new ArrayList<>();
    private String selectedIp = "";
    private final OnDeviceClickListener listener;

    public CastDeviceAdapter(Context context, OnDeviceClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setDevices(List<CastDevice> newDevices) {
        this.devices.clear();
        if (newDevices != null) {
            this.devices.addAll(newDevices);
        }
        notifyDataSetChanged();
    }

    public void addDevice(CastDevice device) {
        if (device == null || device.getIp() == null) return;
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getIp().equalsIgnoreCase(device.getIp())) {
                devices.set(i, device);
                notifyItemChanged(i);
                return;
            }
        }
        devices.add(device);
        notifyDataSetChanged();
    }

    public void setSelectedIp(String ip) {
        this.selectedIp = ip != null ? ip : "";
        notifyDataSetChanged();
    }

    public String getSelectedIp() {
        return selectedIp;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_discovered_speaker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CastDevice device = devices.get(position);
        holder.tvName.setText(device.getName());

        String details = device.getIp();
        if (device.getModel() != null && !device.getModel().isEmpty()) {
            details += " • " + device.getModel();
        }
        if (device.getRoom() != null && !device.getRoom().isEmpty()) {
            details += " • " + device.getRoom();
        }
        holder.tvDetails.setText(details);

        boolean isSelected = device.getIp().equalsIgnoreCase(selectedIp);
        if (isSelected) {
            holder.tvStatus.setText("✓ Gekoppeld");
            holder.tvStatus.setTextColor(Color.parseColor("#34D399"));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge);
            holder.itemView.setAlpha(1.0f);
        } else {
            holder.tvStatus.setText("Koppelen");
            holder.tvStatus.setTextColor(Color.parseColor("#38BDF8"));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_button_selector);
            holder.itemView.setAlpha(0.9f);
        }

        holder.itemView.setOnClickListener(v -> {
            selectedIp = device.getIp();
            notifyDataSetChanged();
            if (listener != null) {
                listener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon;
        TextView tvName;
        TextView tvDetails;
        TextView tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tv_speaker_item_icon);
            tvName = itemView.findViewById(R.id.tv_speaker_item_name);
            tvDetails = itemView.findViewById(R.id.tv_speaker_item_details);
            tvStatus = itemView.findViewById(R.id.tv_speaker_item_status);
        }
    }
}
