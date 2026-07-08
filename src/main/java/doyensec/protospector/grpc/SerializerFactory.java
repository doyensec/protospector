package doyensec.protospector.grpc;

public class SerializerFactory {
    static public Serializer getSerializer(String contentType) {
        if (contentType == null){
            return null;
        }
        if (contentType.startsWith("application/grpc-web-text")) {
            return GrpcTextJsonSerializer.getInstance();
        } else if (contentType.startsWith("application/grpc-web+proto")){
            return GrpcJsonSerializer.getInstance();
        }
        return null;
    }

}
