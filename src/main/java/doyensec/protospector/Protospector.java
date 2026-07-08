package doyensec.protospector;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.InvalidProtocolBufferException;

import burp.api.montoya.core.ByteArray;
import doyensec.protospector.grpc.blackbox.ProtoDescriptorBuilder;
import doyensec.protospector.grpc.Serializer;
import doyensec.protospector.grpc.SerializerException;
import doyensec.protospector.grpc.SerializerFactory;
import doyensec.protospector.protobuf.MethodNotFoundException;
import doyensec.protospector.protobuf.ProtoMessageHelper;
import doyensec.protospector.protobuf.EditableMethodMap.MethodMap;
import doyensec.protospector.ui.UIModel;


public class Protospector {
    public static ByteArray deserializeMessage(String path, ByteArray body, String contentType, boolean isRequest)
            throws SerializerException {
        Serializer serializer = SerializerFactory.getSerializer(contentType);
        Descriptor typeDescriptor = null;
        MethodMap editableMethodMap = UIModel.getInstance().getEditableMethodMap();
        try {
            typeDescriptor = editableMethodMap.getTypeDescriptor(path, isRequest);
        } catch (MethodNotFoundException | DescriptorValidationException e) {
            // Infer types and create a typeDescriptor for this message
            try {
                ByteArray protobufBytes = serializer.getProtoByteArray(body);
                typeDescriptor = ProtoDescriptorBuilder.buildUnknownDescriptorFromMessage(protobufBytes.getBytes());
                if (isRequest) {
                    editableMethodMap.putTypeDescriptor(path, typeDescriptor, true);
                } else {
                    editableMethodMap.putTypeDescriptor(path, typeDescriptor, false);
                }
            }
            catch (InvalidProtocolBufferException | DescriptorValidationException ex) {
                throw new SerializerException("Couldn't deserialize message", ex);
            }
        }
        return serializer.deserialize(body, typeDescriptor);
    }

    public static ByteArray serializeMessage(String path, ByteArray body, String contentType, boolean isRequest)
            throws SerializerException, MethodNotFoundException {
        Descriptor typeDescriptor = null;
        MethodMap editableMethodMap = UIModel.getInstance().getEditableMethodMap();
        try {
            typeDescriptor = editableMethodMap.getTypeDescriptor(path, isRequest);
        } catch (DescriptorValidationException e) {
            throw new SerializerException("Couldn't get type descriptor", e);
        }
        Serializer serializer = SerializerFactory.getSerializer(contentType);
        return serializer.serialize(body, typeDescriptor);
    }

    public static String getDefaultJson(String path, boolean isRequest)
            throws MethodNotFoundException, DescriptorValidationException, InvalidProtocolBufferException {
        MethodMap editableMethodMap = UIModel.getInstance().getEditableMethodMap();
        Descriptor typeDescriptor = editableMethodMap.getTypeDescriptor(path, isRequest);
        return ProtoMessageHelper.typeDescriptorToJsonStr(typeDescriptor);
    }
}
