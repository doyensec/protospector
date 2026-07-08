package doyensec.protospector.protobuf;

public class MethodNotFoundException extends Exception {
    public MethodNotFoundException(String path) {
        super("Method not found for path: " + path);
    }
}
