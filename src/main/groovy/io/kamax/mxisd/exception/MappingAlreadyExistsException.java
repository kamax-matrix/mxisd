package io.kamax.mxisd.exception;

public class MappingAlreadyExistsException extends RuntimeException {

    public MappingAlreadyExistsException() {
        super("A mapping already exists for this 3PID");
    }

}
