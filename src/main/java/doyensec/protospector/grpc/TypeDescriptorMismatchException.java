package doyensec.protospector.grpc;

// The message is valid protobuf, but the saved type descriptor for its path can't decode it
public class TypeDescriptorMismatchException extends SerializerException {
    public TypeDescriptorMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
