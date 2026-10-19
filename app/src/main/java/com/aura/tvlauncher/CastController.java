package com.aura.tvlauncher;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class CastController {
    private static final String TAG = "CastController";
    public static final String DEFAULT_SPEAKER_IP = BuildConfig.SPEAKER_IP;
    private static final int CAST_PORT = 8009;
    private static final int TIMEOUT_MS = 3000;

    private static final String NS_CONN = "urn:x-cast:com.google.cast.tp.connection";
    private static final String NS_RECV = "urn:x-cast:com.google.cast.receiver";
    private static final String NS_MEDIA = "urn:x-cast:com.google.cast.media";

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final AtomicInteger reqIdGen = new AtomicInteger(1);

    public static class CastMediaInfo {
        public boolean isConnected = false;
        public String appName = "";
        public String statusText = "";
        public float volumeLevel = 0.5f;
        public boolean isMuted = false;
        public String sessionId = null;
        public String transportId = null;

        public int mediaSessionId = -1;
        public String playerState = "STANDBY"; // PLAYING, PAUSED, IDLE, STANDBY
        public String title = null;
        public String artist = null;
        public String album = null;
        public String imageUrl = null;

        public boolean isPlaying() {
            return "PLAYING".equalsIgnoreCase(playerState);
        }

        public boolean hasMedia() {
            return title != null && !title.isEmpty();
        }
    }

    public interface StatusCallback {
        void onStatus(CastMediaInfo info);
        void onError(Exception e);
    }

    public interface ActionCallback {
        void onSuccess(CastMediaInfo info);
        void onError(Exception e);
    }

    public static void fetchStatus(String ip, StatusCallback callback) {
        executor.execute(() -> {
            try {
                CastMediaInfo info = queryStatusInternal(ip);
                mainHandler.post(() -> callback.onStatus(info));
            } catch (Exception e) {
                Log.e(TAG, "fetchStatus failed: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public static void togglePlayPause(String ip, ActionCallback callback) {
        executor.execute(() -> {
            try {
                CastMediaInfo info = queryStatusInternal(ip);
                if (info.transportId != null && info.mediaSessionId != -1) {
                    String cmd = info.isPlaying() ? "PAUSE" : "PLAY";
                    executeMediaCommand(ip, info.transportId, info.mediaSessionId, cmd);
                } else if (info.transportId != null) {
                    // Try PLAY with mediaSessionId 1
                    executeMediaCommand(ip, info.transportId, 1, "PLAY");
                }
                CastMediaInfo updated = queryStatusInternal(ip);
                mainHandler.post(() -> callback.onSuccess(updated));
            } catch (Exception e) {
                Log.e(TAG, "togglePlayPause failed: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public static void next(String ip, ActionCallback callback) {
        executor.execute(() -> {
            try {
                CastMediaInfo info = queryStatusInternal(ip);
                if (info.transportId != null && info.mediaSessionId != -1) {
                    executeMediaCommand(ip, info.transportId, info.mediaSessionId, "QUEUE_NEXT");
                } else if (info.transportId != null) {
                    executeMediaCommand(ip, info.transportId, 1, "QUEUE_NEXT");
                }
                Thread.sleep(600);
                CastMediaInfo updated = queryStatusInternal(ip);
                mainHandler.post(() -> callback.onSuccess(updated));
            } catch (Exception e) {
                Log.e(TAG, "next failed: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public static void prev(String ip, ActionCallback callback) {
        executor.execute(() -> {
            try {
                CastMediaInfo info = queryStatusInternal(ip);
                if (info.transportId != null && info.mediaSessionId != -1) {
                    executeMediaCommand(ip, info.transportId, info.mediaSessionId, "QUEUE_PREV");
                } else if (info.transportId != null) {
                    executeMediaCommand(ip, info.transportId, 1, "QUEUE_PREV");
                }
                Thread.sleep(600);
                CastMediaInfo updated = queryStatusInternal(ip);
                mainHandler.post(() -> callback.onSuccess(updated));
            } catch (Exception e) {
                Log.e(TAG, "prev failed: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public static void stop(String ip, ActionCallback callback) {
        executor.execute(() -> {
            try {
                CastMediaInfo info = queryStatusInternal(ip);
                if (info.sessionId != null) {
                    executeReceiverCommand(ip, "{\"type\":\"STOP\",\"sessionId\":\"" + info.sessionId + "\",\"requestId\":" + reqIdGen.incrementAndGet() + "}");
                }
                CastMediaInfo updated = queryStatusInternal(ip);
                mainHandler.post(() -> callback.onSuccess(updated));
            } catch (Exception e) {
                Log.e(TAG, "stop failed: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public static void setVolume(String ip, float level, ActionCallback callback) {
        executor.execute(() -> {
            try {
                float target = Math.max(0.0f, Math.min(1.0f, level));
                JSONObject payload = new JSONObject();
                payload.put("type", "SET_VOLUME");
                payload.put("requestId", reqIdGen.incrementAndGet());
                JSONObject vol = new JSONObject();
                vol.put("level", target);
                payload.put("volume", vol);

                executeReceiverCommand(ip, payload.toString());
                CastMediaInfo updated = queryStatusInternal(ip);
                mainHandler.post(() -> callback.onSuccess(updated));
            } catch (Exception e) {
                Log.e(TAG, "setVolume failed: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public static void adjustVolume(String ip, float delta, ActionCallback callback) {
        executor.execute(() -> {
            try {
                CastMediaInfo info = queryStatusInternal(ip);
                float newLevel = Math.max(0.0f, Math.min(1.0f, info.volumeLevel + delta));
                JSONObject payload = new JSONObject();
                payload.put("type", "SET_VOLUME");
                payload.put("requestId", reqIdGen.incrementAndGet());
                JSONObject vol = new JSONObject();
                vol.put("level", newLevel);
                payload.put("volume", vol);

                executeReceiverCommand(ip, payload.toString());
                CastMediaInfo updated = queryStatusInternal(ip);
                mainHandler.post(() -> callback.onSuccess(updated));
            } catch (Exception e) {
                Log.e(TAG, "adjustVolume failed: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public static void loadImage(String urlString, ImageView targetView) {
        if (urlString == null || urlString.isEmpty() || targetView == null) return;
        executor.execute(() -> {
            try {
                URL url = new URL(urlString);
                InputStream in = url.openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                if (bitmap != null) {
                    mainHandler.post(() -> targetView.setImageBitmap(bitmap));
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to load album art: " + e.getMessage());
            }
        });
    }

    // ================= TLS Socket Communication =================

    private static SSLSocket createSocket(String ip) throws Exception {
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] c, String a) {}
            public void checkServerTrusted(X509Certificate[] c, String a) {}
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }}, new SecureRandom());

        SSLSocket socket = (SSLSocket) sc.getSocketFactory().createSocket();
        socket.connect(new InetSocketAddress(ip, CAST_PORT), TIMEOUT_MS);
        socket.setSoTimeout(TIMEOUT_MS);
        return socket;
    }

    private static CastMediaInfo queryStatusInternal(String ip) throws Exception {
        CastMediaInfo info = new CastMediaInfo();
        SSLSocket socket = null;
        try {
            socket = createSocket(ip);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            sendMessage(out, "sender-0", "receiver-0", NS_CONN, "{\"type\":\"CONNECT\"}");
            sendMessage(out, "sender-0", "receiver-0", NS_RECV, "{\"type\":\"GET_STATUS\",\"requestId\":" + reqIdGen.incrementAndGet() + "}");

            CastMessage recvMsg = readMessage(in);
            if (recvMsg != null && recvMsg.payloadUtf8 != null) {
                info.isConnected = true;
                JSONObject root = new JSONObject(recvMsg.payloadUtf8);
                JSONObject status = root.optJSONObject("status");
                if (status != null) {
                    JSONObject vol = status.optJSONObject("volume");
                    if (vol != null) {
                        info.volumeLevel = (float) vol.optDouble("level", 0.5);
                        info.isMuted = vol.optBoolean("muted", false);
                    }
                    JSONArray apps = status.optJSONArray("applications");
                    if (apps != null && apps.length() > 0) {
                        JSONObject app = apps.getJSONObject(0);
                        info.appName = app.optString("displayName", "");
                        info.statusText = app.optString("statusText", "");
                        info.sessionId = app.optString("sessionId", null);
                        info.transportId = app.optString("transportId", null);
                    }
                }
            }

            // If app is active and has transportId (e.g. Spotify), fetch media status
            if (info.transportId != null && !info.transportId.isEmpty()) {
                sendMessage(out, "sender-0", info.transportId, NS_CONN, "{\"type\":\"CONNECT\"}");
                sendMessage(out, "sender-0", info.transportId, NS_MEDIA, "{\"type\":\"GET_STATUS\",\"requestId\":" + reqIdGen.incrementAndGet() + "}");

                for (int i = 0; i < 3; i++) {
                    CastMessage m = readMessage(in);
                    if (m != null && NS_MEDIA.equals(m.namespace) && m.payloadUtf8 != null) {
                        JSONObject mRoot = new JSONObject(m.payloadUtf8);
                        JSONArray mStatuses = mRoot.optJSONArray("status");
                        if (mStatuses != null && mStatuses.length() > 0) {
                            JSONObject ms = mStatuses.getJSONObject(0);
                            info.mediaSessionId = ms.optInt("mediaSessionId", 1);
                            info.playerState = ms.optString("playerState", "UNKNOWN");

                            JSONObject media = ms.optJSONObject("media");
                            if (media != null) {
                                JSONObject meta = media.optJSONObject("metadata");
                                if (meta != null) {
                                    info.title = meta.optString("title", null);
                                    info.artist = meta.optString("artist", null);
                                    info.album = meta.optString("albumName", null);

                                    JSONArray images = meta.optJSONArray("images");
                                    if (images != null && images.length() > 0) {
                                        info.imageUrl = images.getJSONObject(0).optString("url", null);
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }

            // Fallback status text title parsing if media status didn't return title
            if (!info.hasMedia() && info.statusText != null && !info.statusText.isEmpty()) {
                String st = info.statusText;
                if (st.startsWith("Casten: ")) st = st.substring(8);
                if (!st.isEmpty() && !"Spotify".equalsIgnoreCase(st)) {
                    info.title = st;
                    info.artist = info.appName;
                }
            }

        } finally {
            if (socket != null) {
                try { socket.close(); } catch (Exception ignored) {}
            }
        }
        return info;
    }

    private static void executeMediaCommand(String ip, String transportId, int mediaSessionId, String cmdType) throws Exception {
        SSLSocket socket = null;
        try {
            socket = createSocket(ip);
            OutputStream out = socket.getOutputStream();
            sendMessage(out, "sender-0", transportId, NS_CONN, "{\"type\":\"CONNECT\"}");

            JSONObject payload = new JSONObject();
            payload.put("type", cmdType);
            payload.put("mediaSessionId", mediaSessionId);
            payload.put("requestId", reqIdGen.incrementAndGet());

            sendMessage(out, "sender-0", transportId, NS_MEDIA, payload.toString());
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (Exception ignored) {}
            }
        }
    }

    private static void executeReceiverCommand(String ip, String payload) throws Exception {
        SSLSocket socket = null;
        try {
            socket = createSocket(ip);
            OutputStream out = socket.getOutputStream();
            sendMessage(out, "sender-0", "receiver-0", NS_CONN, "{\"type\":\"CONNECT\"}");
            sendMessage(out, "sender-0", "receiver-0", NS_RECV, payload);
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (Exception ignored) {}
            }
        }
    }

    // ================= Protobuf Wire Format =================

    private static void sendMessage(OutputStream out, String src, String dest, String ns, String payload) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeVarintField(body, 1, 0); // protocol_version = 0
        writeStringField(body, 2, src);
        writeStringField(body, 3, dest);
        writeStringField(body, 4, ns);
        writeVarintField(body, 5, 0); // payload_type = 0 (STRING)
        writeStringField(body, 6, payload);

        byte[] bodyBytes = body.toByteArray();
        DataOutputStream dataOut = new DataOutputStream(out);
        dataOut.writeInt(bodyBytes.length);
        dataOut.write(bodyBytes);
        dataOut.flush();
    }

    private static void writeVarint(OutputStream out, long value) throws IOException {
        while (true) {
            if ((value & ~0x7FL) == 0) {
                out.write((int) value);
                return;
            } else {
                out.write((int) ((value & 0x7F) | 0x80));
                value >>>= 7;
            }
        }
    }

    private static void writeVarintField(OutputStream out, int fieldNum, long value) throws IOException {
        int tag = (fieldNum << 3) | 0;
        writeVarint(out, tag);
        writeVarint(out, value);
    }

    private static void writeStringField(OutputStream out, int fieldNum, String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        int tag = (fieldNum << 3) | 2;
        writeVarint(out, tag);
        writeVarint(out, bytes.length);
        out.write(bytes);
    }

    private static CastMessage readMessage(InputStream in) throws IOException {
        DataInputStream dataIn = new DataInputStream(in);
        int len = dataIn.readInt();
        byte[] buf = new byte[len];
        dataIn.readFully(buf);

        CastMessage msg = new CastMessage();
        int idx = 0;
        int[] indexRef = new int[1];

        while (idx < len) {
            indexRef[0] = idx;
            int tag = (int) readVarintFromBuf(buf, indexRef);
            idx = indexRef[0];
            int fieldNum = tag >>> 3;
            int wireType = tag & 0x07;

            if (wireType == 0) {
                readVarintFromBuf(buf, indexRef);
                idx = indexRef[0];
            } else if (wireType == 2) {
                int fieldLen = (int) readVarintFromBuf(buf, indexRef);
                idx = indexRef[0];
                String str = new String(buf, idx, fieldLen, StandardCharsets.UTF_8);
                idx += fieldLen;
                if (fieldNum == 2) msg.sourceId = str;
                else if (fieldNum == 3) msg.destinationId = str;
                else if (fieldNum == 4) msg.namespace = str;
                else if (fieldNum == 6) msg.payloadUtf8 = str;
            } else {
                break;
            }
        }
        return msg;
    }

    private static long readVarintFromBuf(byte[] buf, int[] indexRef) {
        long value = 0;
        int shift = 0;
        int idx = indexRef[0];
        while (idx < buf.length) {
            byte b = buf[idx++];
            value |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
        }
        indexRef[0] = idx;
        return value;
    }

    private static class CastMessage {
        String sourceId;
        String destinationId;
        String namespace;
        String payloadUtf8;
    }
}
