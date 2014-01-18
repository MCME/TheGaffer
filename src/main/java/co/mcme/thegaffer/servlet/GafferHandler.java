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
package co.mcme.thegaffer.servlet;

import co.mcme.thegaffer.TheGaffer;
import co.mcme.thegaffer.storage.Job;
import co.mcme.thegaffer.storage.JobDatabase;
import co.mcme.thegaffer.utilities.Util;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class GafferHandler extends AbstractHandler {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Util.debug("Processing http request for " + target);
        String[] targets = target.split("/");
        response.setHeader("Server", "TheGaffer v" + TheGaffer.getPluginInstance().getDescription().getVersion());
        response.setHeader("Access-Control-Allow-Origin", "*");
        if (targets.length < 3) {
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            response.getWriter().println("TheGaffer v" + TheGaffer.getPluginInstance().getDescription().getVersion());
            return;
        }
        if (targets.length >= 3) {
            if (targets[1].equalsIgnoreCase("list")) {
                Map data = null;
                if (targets[2].equalsIgnoreCase("active")) {
                    data = JobDatabase.getActiveJobs();
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                }
                if (targets[2].equalsIgnoreCase("inactive")) {
                    data = JobDatabase.getInactiveJobs();
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                }
                if (targets.length == 4 && targets[3].equalsIgnoreCase("namesonly")) {
                    if (data != null) {
                        response.setContentType("application/json");
                        response.getWriter().println(TheGaffer.getJsonMapper().writeValueAsString(data.keySet()));
                    } else {
                        response.setStatus(404);
                    }
                } else {
                    if (data != null) {
                        response.setContentType("application/json");
                        response.getWriter().println(TheGaffer.getJsonMapper().writeValueAsString(data));
                    } else {
                        response.setStatus(404);
                    }
                }
            } else if (targets[1].equalsIgnoreCase("job")) {
                if (JobDatabase.getActiveJobs().containsKey(targets[2])) {
                    Job job = JobDatabase.getActiveJobs().get(targets[2]);
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    response.getWriter().println(TheGaffer.getJsonMapper().writeValueAsString(job));
                } else if (JobDatabase.getInactiveJobs().containsKey(targets[2])) {
                    Job job = JobDatabase.getInactiveJobs().get(targets[2]);
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    response.getWriter().println(TheGaffer.getJsonMapper().writeValueAsString(job));
                }
            }
        }
    }

}
