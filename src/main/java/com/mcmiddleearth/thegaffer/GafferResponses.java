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
package com.mcmiddleearth.thegaffer;

public class GafferResponses {

    public enum HelperResponse implements GafferResponse {

        ALREADY_HELPER("%name% is already a helper on %job%.", false),
        NO_PERMISSIONS("%name% does not have the proper permissions.", false),
        NOT_ONLINE("%name% is not online.", false),
        NOT_HELPER("%name% is not a helper on %job%.", false),
        ADD_SUCCESS("%name% added as helper to %job%", true),
        REMOVE_SUCCESS("%name% removed as helper from %job%", true);

        private final String message;
        private final boolean successful;

        HelperResponse(String message, boolean wasSuccessful) {
            this.message = message;
            this.successful = wasSuccessful;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public boolean isSuccessful() {
            return successful;
        }
    }

    public enum WorkerResponse implements GafferResponse {

        ALREADY_WORKER("You are already a part of %job%.", false),
        NO_PERMISSIONS("You do not have permissions to join %job%.", false),
        NOT_INVITED("%job% is private, and you are not invited.", false),
        NOT_WORKER("%name% is not part of the job.", false),
        NOT_ONLINE("%name% is not online.", false),
        ADD_SUCCESS("Welcome to %job%.", true),
        REMOVE_SUCCESS("%name% removed from job.", true),
        WORKER_BANNED("%name is banned from %job%", false);

        private final String message;
        private final boolean successful;

        WorkerResponse(String message, boolean wasSuccessful) {
            this.message = message;
            this.successful = wasSuccessful;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public boolean isSuccessful() {
            return successful;
        }
    }

    public enum InviteResponse implements GafferResponse {

        ALREADY_INVITED("%name% is already invited to %job%.", false),
        NOT_INVITED("%name% is already not invited to %job%.", false),
        NO_PERMISSIONS("%name% does not have the permissions to be invited to %job%.", false),
        NOT_ONLINE("%name% is not online.", false),
        ADD_SUCCESS("%name% invited to %job%.", true),
        REMOVE_SUCCESS("%name% uninvited from %job%.", true),
        WORKER_BANNED("%name% is banned from %job%", false);

        private final String message;
        private final boolean successful;

        InviteResponse(String message, boolean wasSuccessful) {
            this.message = message;
            this.successful = wasSuccessful;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public boolean isSuccessful() {
            return successful;
        }
    }

    public enum BanWorkerResponse implements GafferResponse {

        ALREADY_BANNED("%name% is already banned from %job%.", false),
        ALREADY_UNBANNED("%name% was not banned from %job%.", false),
        BAN_SUCCESS("Successfully banned %name%.", true),
        UNBAN_SUCCESS("Successfully unbanned %name%.", true);

        private final String message;
        private final boolean successful;

        BanWorkerResponse(String message, boolean wasSuccessful) {
            this.message = message;
            this.successful = wasSuccessful;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public boolean isSuccessful() {
            return successful;
        }
    }

    public enum KickWorkerResponse implements GafferResponse {

        NOT_IN_JOB("%name% is not part of %job%.", false),
        KICK_SUCCESS("Successfully kicked %name%.", true);

        private final String message;
        private final boolean successful;

        KickWorkerResponse(String message, boolean wasSuccessful) {
            this.message = message;
            this.successful = wasSuccessful;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public boolean isSuccessful() {
            return successful;
        }
    }

    public enum GenericResponse implements GafferResponse {

        FAILURE("Generic Failure", false),
        SUCCESS("Generic Success", true);
        private final String message;
        private final boolean successful;

        GenericResponse(String message, boolean wasSuccessful) {
            this.message = message;
            this.successful = wasSuccessful;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public boolean isSuccessful() {
            return successful;
        }
    }

    public interface GafferResponse {

        String getMessage();

        boolean isSuccessful();
    }

}

