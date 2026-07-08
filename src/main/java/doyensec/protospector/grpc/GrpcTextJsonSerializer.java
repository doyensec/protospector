package doyensec.protospector.grpc;

import java.io.IOException;

import com.google.protobuf.Descriptors.Descriptor;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.utilities.Base64Utils;

public class GrpcTextJsonSerializer implements Serializer {

    private static GrpcTextJsonSerializer instance = null;
    private static Base64Utils base64Utils;

    private final GrpcJsonSerializer grpcJsonSerializer;

    private GrpcTextJsonSerializer(GrpcJsonSerializer grpcJsonSerializer) {
        this.grpcJsonSerializer = grpcJsonSerializer;
    }

    static public void initialize(Base64Utils base64Utils) {
        GrpcTextJsonSerializer.base64Utils = base64Utils;
    }

    static public void extensionUnload() {
        instance = null;
        base64Utils = null;
        GrpcJsonSerializer.extensionUnload();
    }

    static public synchronized GrpcTextJsonSerializer getInstance() {
        if (base64Utils == null) {
            throw new IllegalStateException("GrpcTextJsonSerializer not initialized");
        }
        if (instance == null) {
            instance = new GrpcTextJsonSerializer(GrpcJsonSerializer.getInstance());
        }
        return instance;
    }

    @Override
    public ByteArray getProtoByteArray(ByteArray bytes) throws SerializerException {
        try {
            ByteArray decodedBytes = GrpcUtils.decodeConcatedBase64(bytes.toString(), base64Utils);
            return grpcJsonSerializer.getProtoByteArray(decodedBytes);
        } catch (IOException | IllegalArgumentException e) {
            throw new SerializerException("Couldn't decode base64", e);
        }
    }

    @Override
    public ByteArray serialize(ByteArray bytes, Descriptor typeDescriptor) throws SerializerException {
        ByteArray serialized = grpcJsonSerializer.serialize(bytes, typeDescriptor);
        return base64Utils.encode(serialized);
    }

    @Override
    public ByteArray deserialize(ByteArray bytes, Descriptor typeDescriptor)
            throws SerializerException {
        ByteArray decodedBytes;
        try {
            decodedBytes = GrpcUtils.decodeConcatedBase64(bytes.toString(), base64Utils);
        } catch (IOException | IllegalArgumentException e) {
            throw new SerializerException("Couldn't decode base64", e);
        }
        return grpcJsonSerializer.deserialize(decodedBytes, typeDescriptor);
    }

}
