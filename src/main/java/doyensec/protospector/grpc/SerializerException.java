package doyensec.protospector.grpc;

public class SerializerException extends Exception {
    public SerializerException(String message) {
        super(message);
    }

    public SerializerException(String message, Throwable cause) {
        super(message, cause);
    }
}
