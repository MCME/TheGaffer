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
package co.mcme.thegaffer.utilities;

import co.mcme.thegaffer.TheGaffer;
import co.mcme.thegaffer.storage.Job;
import co.mcme.thegaffer.storage.JobDatabase;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import lombok.Cleanup;

public class ExportUtil {

    public static String exportJob(Job job) throws SftpException, FileNotFoundException, JSchException {
        File jobFile = job.getFile();
        if (!jobFile.exists()) {
            JobDatabase.saveJobs();
        }
        Util.debug("preparing the host information for sftp.");
        JSch jsch = new JSch();

        @Cleanup("disconnect")
        Session session = jsch.getSession(TheGaffer.getSFTPUSER(), TheGaffer.getSFTPHOST(), TheGaffer.getSFTPPORT());
        session.setPassword(TheGaffer.getSFTPPASS());
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        Util.debug("Host connected.");

        @Cleanup("disconnect")
        Channel channel = session.openChannel("sftp");
        channel.connect();
        Util.debug("sftp channel opened and connected.");
        @Cleanup("disconnect")
        ChannelSftp channelSftp = (ChannelSftp) channel;
        channelSftp.mkdir(TheGaffer.getSFTPWORKINGDIR());
        channelSftp.cd(TheGaffer.getSFTPWORKINGDIR());
        channelSftp.put(new FileInputStream(jobFile), jobFile.getName());
        Util.debug("File transfered successfully to host.");
        return "";
    }
}
