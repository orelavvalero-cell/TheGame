package com.valeriy.thegame;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class NetworkGame {
    interface StatusSink {
        void setStatus(String text);
    }

    private static final int TCP_PORT = 45455;
    private static final int UDP_PORT = 45456;
    private static final String FIND = "THEGAME_FIND";
    private static final String HOST = "THEGAME_HOST";

    private final Context context;
    private final GameWorld world;
    private final InputState localInput;
    private final StatusSink status;
    private final AtomicBoolean alive = new AtomicBoolean(false);
    private final AtomicInteger session = new AtomicInteger();

    private Thread serverThread;
    private Thread beaconThread;
    private Thread clientThread;
    private ServerSocket serverSocket;
    private Socket socket;
    private DatagramSocket beaconSocket;
    private DatagramSocket discoverySocket;
    private WifiManager.MulticastLock multicastLock;

    NetworkGame(Context context, GameWorld world, InputState localInput, StatusSink status) {
        this.context = context.getApplicationContext();
        this.world = world;
        this.localInput = localInput;
        this.status = status;
    }

    void startHost() {
        stop();
        int token = session.incrementAndGet();
        alive.set(true);
        acquireMulticastLock();
        status.setStatus("HOST: waiting for player 2...");
        beaconThread = new Thread(() -> beaconLoop(token), "TankUdpBeacon");
        serverThread = new Thread(() -> serverLoop(token), "TankTcpHost");
        beaconThread.start();
        serverThread.start();
    }

    void startClient() {
        stop();
        int token = session.incrementAndGet();
        alive.set(true);
        acquireMulticastLock();
        status.setStatus("FIND: searching Wi-Fi host...");
        clientThread = new Thread(() -> clientLoop(token), "TankTcpClient");
        clientThread.start();
    }

    void stop() {
        session.incrementAndGet();
        alive.set(false);
        closeQuietly(discoverySocket);
        closeQuietly(beaconSocket);
        closeQuietly(socket);
        closeQuietly(serverSocket);
        discoverySocket = null;
        beaconSocket = null;
        socket = null;
        serverSocket = null;
        releaseMulticastLock();
    }

    private void serverLoop(int token) {
        try {
            ServerSocket localServer = new ServerSocket();
            localServer.setReuseAddress(true);
            localServer.bind(new InetSocketAddress(TCP_PORT));
            serverSocket = localServer;
            socket = localServer.accept();
            if (!active(token)) {
                return;
            }
            status.setStatus("HOST: player 2 connected");
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Thread reader = new Thread(() -> readClientInput(in, token), "TankHostInput");
            reader.start();
            while (active(token) && !socket.isClosed()) {
                out.println(world.snapshot());
                Thread.sleep(66);
            }
        } catch (Exception ex) {
            if (active(token)) {
                status.setStatus("HOST error: " + shortError(ex));
            }
        } finally {
            closeQuietly(socket);
            closeQuietly(serverSocket);
        }
    }

    private void readClientInput(BufferedReader in, int token) {
        try {
            String line;
            while (active(token) && (line = in.readLine()) != null) {
                InputState input = InputState.fromWire(line);
                if (input != null) {
                    world.setInput(2, input);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void beaconLoop(int token) {
        try (DatagramSocket udp = new DatagramSocket(null)) {
            beaconSocket = udp;
            udp.setReuseAddress(true);
            udp.bind(new InetSocketAddress(UDP_PORT));
            udp.setBroadcast(true);
            udp.setSoTimeout(500);
            byte[] buffer = new byte[96];
            while (active(token)) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    udp.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    if (FIND.equals(msg)) {
                        byte[] answer = HOST.getBytes(StandardCharsets.UTF_8);
                        DatagramPacket response = new DatagramPacket(answer, answer.length, packet.getAddress(), packet.getPort());
                        udp.send(response);
                    }
                } catch (SocketTimeoutException ignored) {
                }
            }
        } catch (IOException ex) {
            if (active(token)) {
                status.setStatus("UDP host error: " + shortError(ex));
            }
        } finally {
            if (active(token)) {
                beaconSocket = null;
            }
        }
    }

    private void clientLoop(int token) {
        try {
            InetAddress host = discoverHost(token);
            if (host == null) {
                if (active(token)) {
                    status.setStatus("FIND: host not found");
                }
                return;
            }
            status.setStatus("CLIENT: connecting " + host.getHostAddress());
            socket = new Socket(host, TCP_PORT);
            if (!active(token)) {
                return;
            }
            status.setStatus("CLIENT: connected");
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Thread writer = new Thread(() -> sendInputLoop(out, token), "TankClientInput");
            writer.start();
            String line;
            while (active(token) && (line = in.readLine()) != null) {
                world.applySnapshot(line);
            }
        } catch (Exception ex) {
            if (active(token)) {
                status.setStatus("CLIENT error: " + shortError(ex));
            }
        } finally {
            closeQuietly(socket);
        }
    }

    private InetAddress discoverHost(int token) throws IOException {
        long end = System.currentTimeMillis() + 8000;
        try (DatagramSocket udp = new DatagramSocket()) {
            discoverySocket = udp;
            udp.setBroadcast(true);
            udp.setSoTimeout(700);
            byte[] ask = FIND.getBytes(StandardCharsets.UTF_8);
            byte[] buffer = new byte[96];
            while (active(token) && System.currentTimeMillis() < end) {
                DatagramPacket request = new DatagramPacket(ask, ask.length, InetAddress.getByName("255.255.255.255"), UDP_PORT);
                udp.send(request);
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                try {
                    udp.receive(response);
                    String msg = new String(response.getData(), 0, response.getLength(), StandardCharsets.UTF_8);
                    if (HOST.equals(msg)) {
                        return response.getAddress();
                    }
                } catch (SocketTimeoutException ignored) {
                    if (active(token)) {
                        status.setStatus("FIND: still searching...");
                    }
                }
            }
        } finally {
            if (active(token)) {
                discoverySocket = null;
            }
        }
        return null;
    }

    private void sendInputLoop(PrintWriter out, int token) {
        while (active(token)) {
            out.println(localInput.toWire());
            try {
                Thread.sleep(33);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private boolean active(int token) {
        return alive.get() && session.get() == token;
    }

    private void acquireMulticastLock() {
        try {
            WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                multicastLock = wifi.createMulticastLock("TheGameWifiDiscovery");
                multicastLock.setReferenceCounted(false);
                multicastLock.acquire();
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void releaseMulticastLock() {
        try {
            if (multicastLock != null && multicastLock.isHeld()) {
                multicastLock.release();
            }
        } catch (RuntimeException ignored) {
        }
        multicastLock = null;
    }

    private static String shortError(Exception ex) {
        String message = ex.getMessage();
        return message == null ? ex.getClass().getSimpleName() : message;
    }

    private static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void closeQuietly(ServerSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void closeQuietly(DatagramSocket socket) {
        if (socket != null) {
            socket.close();
        }
    }
}
