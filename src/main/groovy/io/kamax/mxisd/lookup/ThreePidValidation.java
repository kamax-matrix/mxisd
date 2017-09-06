package io.kamax.mxisd.lookup;

import io.kamax.mxisd.ThreePid;

import java.time.Instant;

public class ThreePidValidation extends ThreePid {

    private Instant validation;

    public ThreePidValidation(String medium, String address, Instant validation) {
        super(medium, address);
        this.validation = validation;
    }

    public Instant getValidation() {
        return validation;
    }

}
