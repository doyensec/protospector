package doyensec.protospector.grpc;

import com.google.protobuf.Descriptors.Descriptor;

import burp.api.montoya.core.ByteArray;

public interface Serializer {
    public ByteArray serialize(ByteArray bytes, Descriptor typeDescriptor) throws SerializerException;
    public ByteArray deserialize(ByteArray bytes, Descriptor typeDescriptor) throws SerializerException;
    public ByteArray getProtoByteArray(ByteArray bytes) throws SerializerException;
}