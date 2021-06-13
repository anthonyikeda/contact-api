package dev.legacy.tech;

public class ContactAddressAlreadyExistsException extends Exception {
    public ContactAddressAlreadyExistsException(String format) {
    }

    public ContactAddressAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContactAddressAlreadyExistsException(Throwable cause) {
        super(cause);
    }
}
