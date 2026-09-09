package com.careerpilot.exception;

public class DataTransferImportDisabledException extends RuntimeException {

    public DataTransferImportDisabledException() {
        super("Full data import is only available in the CareerPilot Windows app.");
    }
}
