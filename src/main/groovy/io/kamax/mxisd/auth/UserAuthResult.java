package io.kamax.mxisd.auth;

public class UserAuthResult {

    private boolean success;
    private String mxid;
    private String displayName;

    public UserAuthResult failure() {
        success = false;
        mxid = null;
        displayName = null;

        return this;
    }

    public void success(String mxid, String displayName) {
        setSuccess(true);
        setMxid(mxid);
        setDisplayName(displayName);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMxid() {
        return mxid;
    }

    public void setMxid(String mxid) {
        this.mxid = mxid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

}
