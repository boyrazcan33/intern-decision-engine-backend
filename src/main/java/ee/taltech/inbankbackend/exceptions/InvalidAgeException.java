package ee.taltech.inbankbackend.exceptions;
/**
 * Thrown when a customer does not meet the age requirements for receiving a loan.
 */

public class InvalidAgeException extends RuntimeException {
    public InvalidAgeException(String message) {
        super(message);
    }
}
