/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mcmiddleearth.thegaffer.utilities;

import lombok.Getter;

/**
 *
 * @author Eriol_Eandur
 */
public enum BuildProtection {
    
    ALLOWED         ("You are allowed to build."),
    LOC_DENIED      ("You are not allowed to build here."),
    JOB_PAUSED      ("The job is currently paused."),
    OUT_OF_BOUNDS   ("You have gone out of bounds for the job."),
    NOT_IN_JOB      ("You are not part of any job."),
    WORLD_DENIED    ("You are not allowed to build in this world."),
    NO_JOB          ("You are not allowed to build when there are no jobs.");

    @Getter
    private final String message;

    private BuildProtection(String message) {
        this.message = message;
    }
}
