package doyensec.protospector.grpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import burp.api.montoya.core.ByteArray;
import doyensec.protospector.protobuf.ProtoMessageHelper;

public class GrpcJsonSerializer implements Serializer {

    private static GrpcJsonSerializer instance = null;

    private static final byte GRPC_UNCOMPRESSED_DATA_FLAG = 0;
    private static final int GRPC_HEADER_SIZE = 5;

    static synchronized public GrpcJsonSerializer getInstance() {
        if (instance == null) {
            instance = new GrpcJsonSerializer();
        }
        return instance;
    }

    static public void extensionUnload() {
        instance = null;
    }

    @Override
    public ByteArray getProtoByteArray(ByteArray bytes) throws SerializerException {
        if (bytes.length() < GRPC_HEADER_SIZE) {
            throw new SerializerException("gRPC message size too small");
        }

        if (bytes.getByte(0) != GRPC_UNCOMPRESSED_DATA_FLAG) {
            throw new SerializerException("gRPC compression is not supported");
        }

        int messageSize;
        {
            byte[] sizeBytes = bytes.subArray(1, GRPC_HEADER_SIZE).getBytes();
            messageSize = GrpcUtils.intFromBigEndian(sizeBytes);
        }

        if (messageSize > bytes.length() - GRPC_HEADER_SIZE) {
            throw new SerializerException("gRPC message size too large");
        }

        // Get messsage
        int messageEnd = messageSize + GRPC_HEADER_SIZE;
        byte[] dataWireBytes;
        if (messageSize > 0) {
            dataWireBytes = bytes.subArray(GRPC_HEADER_SIZE, messageEnd).getBytes();
        } else {
            dataWireBytes = new byte[0];
        }

        return ByteArray.byteArray(dataWireBytes);
    }

    @Override
    public ByteArray serialize(ByteArray bytes, Descriptor typeDescriptor) throws SerializerException {
        String jsonStr = bytes.toString();
        Message message;
        try {
            message = ProtoMessageHelper.JsonStrToMessage(jsonStr, typeDescriptor);
        } catch (InvalidProtocolBufferException e) {
            throw new SerializerException("Invalid message; the json is likely invalid", e);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        outputStream.write(GRPC_UNCOMPRESSED_DATA_FLAG);

        {
            byte[] serializeSize = GrpcUtils.intToBigEndian(message.getSerializedSize());
            outputStream.write(serializeSize, 0, GRPC_HEADER_SIZE - 1);
        }

        try {
            outputStream.write(message.toByteArray());
        } catch (IOException e) {
            throw new SerializerException("Couldn't serialize message; the json is likely invalid", e);
        }

        return ByteArray.byteArray(outputStream.toByteArray());
    }

    @Override
    public ByteArray deserialize(ByteArray bytes, Descriptor typeDescriptor)
            throws SerializerException {
        byte[] dataWireBytes = getProtoByteArray(bytes).getBytes();

        // Parse message
        String jsonStr;
        try {
            Message message = ProtoMessageHelper.wireBytesToMessage(dataWireBytes, typeDescriptor);
            jsonStr = ProtoMessageHelper.messageToJsonStr(message);
        } catch (InvalidProtocolBufferException e) {
            throw new SerializerException("Failed to parse message; it's likely an invalid protobuffer wire", e);
        }

        return ByteArray.byteArray(jsonStr);
    }
}
