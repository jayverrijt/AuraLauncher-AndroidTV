package com.aura.tvlauncher;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CastDiscoveryManager {
    private static final String TAG = "CastDiscoveryManager";
    private static final String SERVICE_TYPE = "_googlecast._tcp.";
    private static final int CAST_PORT = 8009;

    public interface DiscoveryCallback {
        void onDeviceFound(CastDevice device);
        void onDiscoveryComplete(List<CastDevice> devices);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, CastDevice> discoveredDevices = new ConcurrentHashMap<>();
    private final Queue<NsdServiceInfo> resolveQueue = new LinkedList<>();

    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private WifiManager.MulticastLock multicastLock;
    private ExecutorService subnetExecutor;
    private List<Future<?>> probeFutures = new ArrayList<>();

    private boolean isDiscovering = false;
    private boolean isResolving = false;
    private DiscoveryCallback activeCallback;

    public CastDiscoveryManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized void startDiscovery(DiscoveryCallback callback) {
        if (isDiscovering) {
            stopDiscovery();
        }
        this.activeCallback = callback;
        this.isDiscovering = true;
        this.discoveredDevices.clear();
        this.resolveQueue.clear();
        this.isResolving = false;

        acquireMulticastLock();
        startNsdDiscovery();
        startSubnetScan();

        // Set a timeout to finish discovery if not already stopped
        mainHandler.postDelayed(() -> {
            if (isDiscovering) {
                notifyDiscoveryComplete();
            }
        }, 6000);
    }

    public List<CastDevice> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices.values());
    }

    public synchronized void stopDiscovery() {
        if (!isDiscovering) return;
        isDiscovering = false;

        stopNsdDiscovery();
        stopSubnetScan();
        releaseMulticastLock();
    }

    private void acquireMulticastLock() {
        try {
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                multicastLock = wifi.createMulticastLock("aura_cast_discovery");
                multicastLock.setReferenceCounted(true);
                multicastLock.acquire();
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not acquire MulticastLock: " + e.getMessage());
        }
    }

    private void releaseMulticastLock() {
        try {
            if (multicastLock != null && multicastLock.isHeld()) {
                multicastLock.release();
            }
        } catch (Exception ignored) {}
    }

    private void startNsdDiscovery() {
        try {
            nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
            if (nsdManager == null) return;

            discoveryListener = new NsdManager.DiscoveryListener() {
                @Override
                public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                    Log.e(TAG, "Discovery failed to start: " + errorCode);
                }

                @Override
                public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                    Log.e(TAG, "Discovery failed to stop: " + errorCode);
                }

                @Override
                public void onDiscoveryStarted(String serviceType) {
                    Log.d(TAG, "NSD discovery started for " + serviceType);
                }

                @Override
                public void onDiscoveryStopped(String serviceType) {
                    Log.d(TAG, "NSD discovery stopped");
                }

                @Override
                public void onServiceFound(NsdServiceInfo serviceInfo) {
                    if (serviceInfo.getServiceType() != null && serviceInfo.getServiceType().contains("googlecast")) {
                        synchronized (resolveQueue) {
                            resolveQueue.offer(serviceInfo);
                            processNextResolve();
                        }
                    }
                }

                @Override
                public void onServiceLost(NsdServiceInfo serviceInfo) {
                    // Ignore for transient scan
                }
            };

            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start NSD: " + e.getMessage());
        }
    }

    private void processNextResolve() {
        synchronized (resolveQueue) {
            if (isResolving || resolveQueue.isEmpty() || !isDiscovering || nsdManager == null) {
                return;
            }
            isResolving = true;
            NsdServiceInfo serviceInfo = resolveQueue.poll();
            if (serviceInfo == null) {
                isResolving = false;
                return;
            }

            try {
                nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        synchronized (resolveQueue) {
                            isResolving = false;
                            processNextResolve();
                        }
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo resolvedServiceInfo) {
                        handleResolvedService(resolvedServiceInfo);
                        synchronized (resolveQueue) {
                            isResolving = false;
                            processNextResolve();
                        }
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "resolveService exception: " + e.getMessage());
                isResolving = false;
                processNextResolve();
            }
        }
    }

    private void handleResolvedService(NsdServiceInfo info) {
        if (info == null || info.getHost() == null) return;

        String ip = info.getHost().getHostAddress();
        int port = info.getPort() > 0 ? info.getPort() : CAST_PORT;

        String friendlyName = null;
        String modelName = "Google Cast";
        String roomName = "Woonkamer";

        try {
            Map<String, byte[]> attrs = info.getAttributes();
            if (attrs != null) {
                if (attrs.containsKey("fn")) {
                    byte[] fnBytes = attrs.get("fn");
                    if (fnBytes != null && fnBytes.length > 0) {
                        friendlyName = new String(fnBytes, StandardCharsets.UTF_8).trim();
                    }
                }
                if (attrs.containsKey("md")) {
                    byte[] mdBytes = attrs.get("md");
                    if (mdBytes != null && mdBytes.length > 0) {
                        modelName = new String(mdBytes, StandardCharsets.UTF_8).trim();
                    }
                }
                if (attrs.containsKey("rm")) {
                    byte[] rmBytes = attrs.get("rm");
                    if (rmBytes != null && rmBytes.length > 0) {
                        roomName = new String(rmBytes, StandardCharsets.UTF_8).trim();
                    }
                }
            }
        } catch (Exception ignored) {}

        if (friendlyName == null || friendlyName.isEmpty()) {
            friendlyName = info.getServiceName();
            if (friendlyName != null && friendlyName.startsWith("Google-Cast-")) {
                friendlyName = "Google Cast Speaker";
            }
        }

        CastDevice device = new CastDevice(ip, friendlyName, modelName, roomName, port);
        Log.i(TAG, "Resolved Cast Device: fn=" + friendlyName + ", ip=" + ip + ", model=" + modelName);
        addDiscoveredDevice(device);
    }

    private void startSubnetScan() {
        subnetExecutor = Executors.newFixedThreadPool(16);
        probeFutures = new ArrayList<>();

        String localIp = getLocalDeviceIpAddress();
        if (localIp == null || !localIp.contains(".")) return;

        String prefix = localIp.substring(0, localIp.lastIndexOf('.') + 1);

        // Scan subnet addresses on port 8009
        for (int i = 1; i <= 254; i++) {
            final String targetIp = prefix + i;
            if (targetIp.equals(localIp)) continue;

            Future<?> f = subnetExecutor.submit(() -> {
                if (!isDiscovering) return;
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(targetIp, CAST_PORT), 160);
                    // Port is open! Verify it's a Cast device
                    if (!discoveredDevices.containsKey(targetIp)) {
                        probeCastDeviceViaStatus(targetIp);
                    }
                } catch (Exception ignored) {
                    // Port closed or unreachable
                }
            });
            probeFutures.add(f);
        }
    }

    private void probeCastDeviceViaStatus(String ip) {
        CastController.fetchStatus(ip, new CastController.StatusCallback() {
            @Override
            public void onStatus(CastController.CastMediaInfo info) {
                if (info != null && !discoveredDevices.containsKey(ip)) {
                    String name = info.appName != null && !info.appName.isEmpty() && !"Backdrop".equalsIgnoreCase(info.appName)
                            ? info.appName + " Speaker"
                            : "Google Cast Speaker";
                    CastDevice device = new CastDevice(ip, name, "Google Cast", "Woonkamer", CAST_PORT);
                    addDiscoveredDevice(device);
                }
            }

            @Override
            public void onError(Exception e) {
                // If port 8009 responded but TLS query timed out, still record as generic Cast speaker
                if (!discoveredDevices.containsKey(ip)) {
                    CastDevice device = new CastDevice(ip, "Google Cast (" + ip + ")", "Google Cast", "Woonkamer", CAST_PORT);
                    addDiscoveredDevice(device);
                }
            }
        });
    }

    private void addDiscoveredDevice(CastDevice device) {
        if (device == null || device.getIp() == null) return;
        discoveredDevices.put(device.getIp(), device);
        Log.i(TAG, "addDiscoveredDevice: " + device.getName() + " -> " + device.getIp());
        mainHandler.post(() -> {
            if (activeCallback != null) {
                activeCallback.onDeviceFound(device);
            }
        });
    }

    private void notifyDiscoveryComplete() {
        mainHandler.post(() -> {
            if (activeCallback != null) {
                List<CastDevice> list = new ArrayList<>(discoveredDevices.values());
                activeCallback.onDiscoveryComplete(list);
            }
        });
    }

    private void stopNsdDiscovery() {
        try {
            if (nsdManager != null && discoveryListener != null) {
                nsdManager.stopServiceDiscovery(discoveryListener);
            }
        } catch (Exception ignored) {}
        discoveryListener = null;
    }

    private void stopSubnetScan() {
        if (probeFutures != null) {
            for (Future<?> f : probeFutures) {
                f.cancel(true);
            }
            probeFutures.clear();
        }
        if (subnetExecutor != null) {
            subnetExecutor.shutdownNow();
            subnetExecutor = null;
        }
    }

    public static String getLocalDeviceIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.isLoopback() || !intf.isUp()) continue;
                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
