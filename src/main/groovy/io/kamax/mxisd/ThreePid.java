package io.kamax.mxisd;

// FIXME this should be in matrix-java-sdk
public class ThreePid {

    private String medium;
    private String address;

    public ThreePid(String medium, String address) {
        this.medium = medium;
        this.address = address;
    }

    public String getMedium() {
        return medium;
    }

    public String getAddress() {
        return address;
    }

}
