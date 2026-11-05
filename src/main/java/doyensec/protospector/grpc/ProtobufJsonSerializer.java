package doyensec.protospector.grpc;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import burp.api.montoya.core.ByteArray;
import doyensec.protospector.protobuf.ProtoMessageHelper;

// Raw protobuf bodies (application/x-protobuf and friends): no gRPC framing
public class ProtobufJsonSerializer implements Serializer {

    private static ProtobufJsonSerializer instance = null;

    static synchronized public ProtobufJsonSerializer getInstance() {
        if (instance == null) {
            instance = new ProtobufJsonSerializer();
        }
        return instance;
    }

    static public void extensionUnload() {
        instance = null;
    }

    @Override
    public ByteArray getProtoByteArray(ByteArray bytes) throws SerializerException {
        return bytes;
    }

    @Override
    public ByteArray serialize(ByteArray bytes, Descriptor typeDescriptor) throws SerializerException {
        Message message;
        try {
            message = ProtoMessageHelper.JsonStrToMessage(bytes.toString(), typeDescriptor);
        } catch (InvalidProtocolBufferException e) {
            throw new SerializerException("Invalid message; the json is likely invalid", e);
        }
        return ByteArray.byteArray(message.toByteArray());
    }

    @Override
    public ByteArray deserialize(ByteArray bytes, Descriptor typeDescriptor) throws SerializerException {
        String jsonStr;
        try {
            Message message = ProtoMessageHelper.wireBytesToMessage(bytes.getBytes(), typeDescriptor);
            jsonStr = ProtoMessageHelper.messageToJsonStr(message);
        } catch (InvalidProtocolBufferException e) {
            throw new SerializerException("Failed to parse message; it's likely an invalid protobuffer wire", e);
        }
        return ByteArray.byteArray(jsonStr);
    }
}
