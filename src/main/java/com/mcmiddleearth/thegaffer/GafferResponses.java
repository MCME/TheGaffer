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
        ALREADY_IN_JOB("%name% is already in another job.", false),
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
        ALREADY_IN_JOB("You are already in another job - use /job leave first.", false),
        NO_PERMISSIONS("You do not have permissions to join %job%.", false),
        NOT_INVITED("%job% is private, and you are not invited.", false),
        NOT_WORKER("%name% is not part of the job.", false),
        NOT_ONLINE("%name% is not online.", false),
        ADD_SUCCESS("Welcome to %job%.", true),
        REMOVE_SUCCESS("%name% removed from job.", true),
        WORKER_BANNED("%name% is banned from %job%", false),
        LEAVE_SUCCESS("You left %job%.",true);

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
        CANNOT_BAN_OWNER("You can't ban the owner of %job%.", false),
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

    /**
     * Carries the concrete radius value so confirmation messages can say
     * "Job area radius set to N (a 2N×2N area)." rather than a generic phrase.
     */
    public static final class SetRadiusResponse implements GafferResponse {

        private final int radius;

        public SetRadiusResponse(int radius) {
            this.radius = radius;
        }

        public int getRadius() {
            return radius;
        }

        @Override
        public String getMessage() {
            int diam = radius * 2;
            return "Job area radius set to " + radius + " (a " + diam + "×" + diam + " area).";
        }

        @Override
        public boolean isSuccessful() {
            return true;
        }
    }

    /** A lightweight failure-only response for validation errors (no enum constant needed). */
    public static final class ValidationFailureResponse implements GafferResponse {

        private final String message;

        public ValidationFailureResponse(String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public boolean isSuccessful() {
            return false;
        }
    }

    /** Success response for the {@code teleport <player>} admin action. */
    public static final class TeleportWorkerResponse implements GafferResponse {

        private final String playerName;

        public TeleportWorkerResponse(String playerName) {
            this.playerName = playerName;
        }

        @Override
        public String getMessage() {
            return "Teleported " + playerName + " to your location.";
        }

        @Override
        public boolean isSuccessful() {
            return true;
        }
    }

    public enum PromoteResponse implements GafferResponse {

        NOT_A_WORKER("%name% is not a worker on %job%.", false),
        ALREADY_HELPER("%name% is already a helper on %job%.", false),
        PROMOTE_SUCCESS("%name% promoted to helper on %job%.", true);

        private final String message;
        private final boolean successful;

        PromoteResponse(String message, boolean wasSuccessful) {
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

    public enum DemoteResponse implements GafferResponse {

        NOT_A_HELPER("%name% is not a helper on %job%.", false),
        DEMOTE_SUCCESS("%name% demoted to worker on %job%.", true);

        private final String message;
        private final boolean successful;

        DemoteResponse(String message, boolean wasSuccessful) {
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

