package io.kamax.mxisd.lookup;

import java.time.Instant;

public class ThreePid {

    private String medium;
    private String address;
    private Instant validation;

    public ThreePid(String medium, String address, Instant validation) {
        this.medium = medium;
        this.address = address;
        this.validation = validation;
    }

    public String getMedium() {
        return medium;
    }

    public String getAddress() {
        return address;
    }

    public Instant getValidation() {
        return validation;
    }

}
