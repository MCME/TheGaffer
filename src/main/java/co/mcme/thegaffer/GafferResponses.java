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
package co.mcme.thegaffer;

import lombok.Getter;

public class GafferResponses {

    public enum HelperResponse implements GafferResponse {

        ALREADY_HELPER("%name% is already a helper on %job%.", false),
        NO_PERMISSIONS("%name% does not have the proper permissions.", false),
        NOT_ONLINE("%name% is not online.", false),
        NOT_HELPER("%name% is not a helper on %job%.", false),
        ADD_SUCCESS("%name% added as helper to %job%", true),
        REMOVE_SUCCESS("%name% removed as helper from %job%", true);

        @Getter
        private final String message;
        @Getter
        private final boolean successful;

        private HelperResponse(String message, boolean wasSuccessful) {
            this.message = message;
            this.successful = wasSuccessful;
        }
    }

    public enum WorkerResponse implements GafferResponse {

        ALREADY_WORKER("You are already a part of %job%.", false),
        NO_PERMISSIONS("You do not have permissions to join %job%.", false),
        NOT_INVITED("%job% is private, and you are not invited.", false),
        NOT_WORKER("%name% is not part of the job.", false),
        NOT_ONLINE("%name% is not online.", false),
        ADD_SUCCESS("Welcome to %job%.", true),
        REMOVE_SUCCESS("%name% removed from job.", true);

        @Getter
        private final String message;
        @Getter
        private final boolean successful;

        private WorkerResponse(String message, boolean wasSuccessful) {
            this.message = message;
            this.successful = wasSuccessful;
        }
    }

    public enum InviteResponse implements GafferResponse {

        ALREADY_INVITED("%name% is already invited to %job%.", false),
        NOT_INVITED("%name% is already not invited to %job%.", false),
        NO_PERMISSIONS("%name% does not have the permissions to be invited to %job%.", false),
        NOT_ONLINE("%name% is not online.", false),
        ADD_SUCCESS("%name% invited to %job%.", true),
        REMOVE_SUCCESS("%name% uninvited from %job%.", true);

        @Getter
        private final String message;
        @Getter
        private final boolean successful;

        private InviteResponse(String message, boolean wasSuccessful) {
            this.message = message;
            this.successful = wasSuccessful;
        }
    }

    public enum BanWorkerResponse implements GafferResponse {

        ALREADY_BANNED("%name% is already banned from %job%.", false),
        ALREADY_UNBANNED("%name% was not banned from %job%.", false),
        BAN_SUCCESS("Successfully banned %name%.", false),
        UNBAN_SUCCESS("Successfully unbanned %name%.", false);

        @Getter
        private final String message;
        @Getter
        private final boolean successful;

        private BanWorkerResponse(String message, boolean wasSuccessful) {
            this.message = message;
            this.successful = wasSuccessful;
        }
    }

    public enum KickWorkerResponse implements GafferResponse {

        NOT_IN_JOB("%name% is not part of %job%.", false),
        KICK_SUCCESS("Successfully kicked %name%.", false);

        @Getter
        private final String message;
        @Getter
        private final boolean successful;

        private KickWorkerResponse(String message, boolean wasSuccessful) {
            this.message = message;
            this.successful = wasSuccessful;
        }
    }

    public interface GafferResponse {

        public abstract String getMessage();

        public abstract boolean isSuccessful();
    }
}
