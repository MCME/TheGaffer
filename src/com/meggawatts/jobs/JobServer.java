package com.meggawatts.jobs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public final class JobServer extends Thread {

    private final Jobs jobs;
    private final String host;
    private final int port;
    private final ServerSocket listener;
    private final Logger log = Logger.getLogger("Minecraft");

    public JobServer(Jobs jobs, String host, int port) throws IOException {
        this.jobs = jobs;
        this.host = host;
        this.port = port;

        // Initialize the listener.
        InetSocketAddress address;
        log.info("Starting MCMEJobs query server on *:" + Integer.toString(port));
        address = new InetSocketAddress(port);
        listener = new ServerSocket();
        listener.bind(address);
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Wait for and accept all incoming connections.
                Socket socket = getListener().accept();

                // Create a new thread to handle the request.
                (new Thread(new Check(getJobs(), socket))).start();
            }
        } catch (IOException ex) {
            log.info("Stopping MCMEJobs query server");
        }
    }

    public Jobs getJobs() {
        return jobs;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public ServerSocket getListener() {
        return listener;
    }
}
