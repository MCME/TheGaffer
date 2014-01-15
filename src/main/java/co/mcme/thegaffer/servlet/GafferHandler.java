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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class GafferHandler extends AbstractHandler {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Util.debug("Processing http request for " + target);
        if (target.equals("/list/active")) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            response.getWriter().println(TheGaffer.getJsonMapper().writeValueAsString(JobDatabase.getActiveJobs()));
        } else if (target.equals("/list/inactive")) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            response.getWriter().println(TheGaffer.getJsonMapper().writeValueAsString(JobDatabase.getInactiveJobs()));
        } else if (target.contains("/job/")) {
            target = target.replaceAll("/job/", "");
            if (JobDatabase.getActiveJobs().containsKey(target)) {
                Job job = JobDatabase.getActiveJobs().get(target);
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);
                response.getWriter().println(TheGaffer.getJsonMapper().writeValueAsString(job));
            } else if (JobDatabase.getInactiveJobs().containsKey(target)) {
                Job job = JobDatabase.getInactiveJobs().get(target);
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);
                response.getWriter().println(TheGaffer.getJsonMapper().writeValueAsString(job));
            }
        }
    }

}
