/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.thegaffer.utilities;

/**
 *
 * @author Eriol_Eandur
 */
// #11: Messages use plain English with no developer jargon; recovery hints added to the
//      actionable denials (PAUSED, OUT_OF_BOUNDS, NOT_IN_JOB, NO_JOB).
//      Colour is standardised to RED in ProtectionListener (was DARK_RED).
public enum BuildProtection {

    ALLOWED         ("You are allowed to build."),
    LOC_DENIED      ("You are not allowed to build here."),
    JOB_PAUSED      ("This job is paused — building is locked until staff resume it."),
    OUT_OF_BOUNDS   ("You're outside the job area — /job border to see its edge."),
    NOT_IN_JOB      ("You're not in a job here — /job check to find one."),
    WORLD_DENIED    ("You are not allowed to build in this world."),
    NO_JOB          ("You're not in a job here — /job check to find one.");

    private final String message;

    BuildProtection(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
