package com.meggawatts.jobs;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Check extends Thread {

    private final Jobs jobs;
    private final Socket socket;
    private final Logger log = Logger.getLogger("Minecraft");

    public Check(Jobs jobs, Socket socket) {
        this.jobs = jobs;
        this.socket = socket;
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Read the request and handle it.
            handleRequest(socket, reader.readLine());

            // Finally close the socket.
            socket.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "MCMEJobs server thread shutting down", ex);
        }
    }

    private void handleRequest(Socket socket, String request) throws IOException {
        // Handle a query request.
        if (request == null) {
            return;
        }

        // Handle a request, respond in JSON format.
        if (request.equalsIgnoreCase("QUERY")) {
            Jobs j = getJobs();

            // Build the JSON response.
            StringBuilder response = new StringBuilder();
            response.append("{");
            if (j.jobCount() == 0){
              response.append("\"jobCount\":").append(j.jobCount());
            response.append(",");
            response.append("\n");
            response.append("\"jobList\":");
            response.append("[");
            response.append("]");
            response.append("}\n");  
            }
            else {
            response.append("\"jobCount\":").append(j.jobCount());
            response.append(",");
            response.append("\n");
            response.append("\"jobList\":");
            response.append("[");
            String lobList = j.makeJSON(j.getRunning());
            String jobList = j.makeJSON(j.getRunning()).substring(0, lobList.length() -1);
            response.append(jobList);
            response.append("]");
            response.append("}\n");
            }

            // Send the JSON response.
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBytes(response.toString());
        }

        // Different requests may be introduced in the future.
    }

    public Jobs getJobs() {
        return jobs;
    }
}
