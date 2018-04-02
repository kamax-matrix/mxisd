package io.kamax.mxisd.exception;

import org.apache.http.HttpStatus;

public class RemoteHomeServerException extends HttpMatrixException {

    public RemoteHomeServerException(String error) {
        super(HttpStatus.SC_SERVICE_UNAVAILABLE, "M_REMOTE_HS_ERROR", "Error from remote server: " + error);
    }

}
