package doyensec.protospector;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UnknownFieldSet;

import burp.api.montoya.core.ByteArray;
import doyensec.protospector.grpc.blackbox.ProtoDescriptorBuilder;
import doyensec.protospector.grpc.Serializer;
import doyensec.protospector.grpc.SerializerException;
import doyensec.protospector.grpc.SerializerFactory;
import doyensec.protospector.grpc.TypeDescriptorMismatchException;
import doyensec.protospector.protobuf.MethodNotFoundException;
import doyensec.protospector.protobuf.ProtoMessageHelper;
import doyensec.protospector.protobuf.EditableMethodMap.MethodMap;
import doyensec.protospector.ui.UIModel;


public class Protospector {
    public static ByteArray deserializeMessage(String path, ByteArray body, String contentType, boolean isRequest)
            throws SerializerException {
        Serializer serializer = SerializerFactory.getSerializer(contentType);
        Descriptor typeDescriptor = null;
        boolean isSavedType = true;
        MethodMap editableMethodMap = UIModel.getInstance().getEditableMethodMap();
        try {
            typeDescriptor = editableMethodMap.getTypeDescriptor(path, isRequest);
        } catch (MethodNotFoundException | DescriptorValidationException e) {
            isSavedType = false;
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
        try {
            return serializer.deserialize(body, typeDescriptor);
        } catch (SerializerException e) {
            if (isSavedType && isValidProtobuf(serializer, body)) {
                throw new TypeDescriptorMismatchException("The saved type can't decode this message", e);
            }
            throw e;
        }
    }

    // A valid protobuf wire that the saved type rejects means the saved type is wrong, not the message
    private static boolean isValidProtobuf(Serializer serializer, ByteArray body) {
        try {
            UnknownFieldSet.parseFrom(serializer.getProtoByteArray(body).getBytes());
            return true;
        } catch (SerializerException | InvalidProtocolBufferException e) {
            return false;
        }
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
