package com.aura.tvlauncher;

import java.util.Objects;

public class CastDevice {
    private final String ip;
    private final String name;
    private final String model;
    private final String room;
    private final int port;

    public CastDevice(String ip, String name, String model, String room, int port) {
        this.ip = ip;
        this.name = name != null && !name.trim().isEmpty() ? name : "Google Cast Speaker";
        this.model = model != null && !model.trim().isEmpty() ? model : "Google Cast";
        this.room = room != null && !room.trim().isEmpty() ? room : "Woonkamer";
        this.port = port > 0 ? port : 8009;
    }

    public String getIp() {
        return ip;
    }

    public String getName() {
        return name;
    }

    public String getModel() {
        return model;
    }

    public String getRoom() {
        return room;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CastDevice that = (CastDevice) o;
        return Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip);
    }

    @Override
    public String toString() {
        return name + " (" + ip + ")";
    }
}
