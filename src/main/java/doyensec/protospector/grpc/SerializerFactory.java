package doyensec.protospector.grpc;

import java.util.Locale;

public class SerializerFactory {
    private static final String[] RAW_PROTOBUF_CONTENT_TYPES = {
            "application/x-protobuf",
            "application/protobuf",
            "application/x-google-protobuf",
            "application/vnd.google.protobuf",
    };

    static public Serializer getSerializer(String contentType) {
        if (contentType == null){
            return null;
        }
        contentType = contentType.trim().toLowerCase(Locale.ROOT);
        if (contentType.startsWith("application/grpc-web-text")) {
            return GrpcTextJsonSerializer.getInstance();
        } else if (contentType.startsWith("application/grpc-web+proto")){
            return GrpcJsonSerializer.getInstance();
        }
        for (String rawContentType : RAW_PROTOBUF_CONTENT_TYPES) {
            if (contentType.startsWith(rawContentType)) {
                return ProtobufJsonSerializer.getInstance();
            }
        }
        return null;
    }

}
