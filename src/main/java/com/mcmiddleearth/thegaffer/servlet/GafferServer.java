/*  This file is part of TheGaffer.
 * 
 *  TheGaffer is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  TheGaffer is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with TheGaffer.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mcmiddleearth.thegaffer.servlet;

import com.mcmiddleearth.thegaffer.utilities.Util;
import lombok.Getter;
import org.eclipse.jetty.server.Server;

public class GafferServer {

    @Getter
    Server server;
    @Getter
    int port;

    public GafferServer(int port) {
        this.port = port;
        server = new Server(port);
        server.setHandler(new GafferHandler());
    }

    public void startServer() throws Exception {
        try {
            if (server != null) {
                server.start();
            }
        } catch (Exception e) {
            Util.severe("Failed to start servlet on port: " + port + " : " + e.getMessage());
        }
    }

    public void stopServer() throws Exception {
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Exception e) {
            Util.severe("Failed to stop servlet on port: " + port + " : " + e.getMessage());
        }
    }
}
