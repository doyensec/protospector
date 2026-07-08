package doyensec.protospector.grpc.blackbox;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UnknownFieldSet;
import com.google.protobuf.Message;

import doyensec.protospector.grpc.SerializerException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Map;

public class ProtoDescriptorBuilder {
    private static UnknownFieldSet attemptUnknownMessageConstruction(ByteString data)
            throws InvalidProtocolBufferException {
        return UnknownFieldSet.parseFrom(data);
    }

    private static FieldDescriptorProto handleLengthDelField(UnknownFieldSet.Field field,
            FieldDescriptorProto.Builder fieldBuilder, DescriptorProto.Builder parentMessageBuilder, AtomicInteger nestedMessageCounter)
            throws DescriptorValidationException {
        List<ByteString> lenDelList = field.getLengthDelimitedList();

        if (lenDelList.size() > 1) {
            fieldBuilder.setLabel(FieldDescriptorProto.Label.LABEL_REPEATED);
        }

        ByteString firstLenDel = lenDelList.get(0);
        DescriptorProto.Builder subMessageTypeBuilder;
        DescriptorProto subMessageType;
        try {
            UnknownFieldSet subMessageUnknownFields = attemptUnknownMessageConstruction(firstLenDel);
            subMessageTypeBuilder = DescriptorProto.newBuilder();
            subMessageTypeBuilder.setName("ProtospectorSubMessage_" + nestedMessageCounter.incrementAndGet());
            subMessageType = buildOutUnknownFields(subMessageTypeBuilder, subMessageUnknownFields, nestedMessageCounter);
        } catch (InvalidProtocolBufferException | DescriptorValidationException e) {
            if (firstLenDel.isValidUtf8()) {
                fieldBuilder.setType(Type.TYPE_STRING);
            } else {
                fieldBuilder.setType(Type.TYPE_BYTES);
            }
            return fieldBuilder.build();
        }
        fieldBuilder.setType(Type.TYPE_MESSAGE);
        fieldBuilder.setTypeName(subMessageTypeBuilder.getName());
        parentMessageBuilder.addNestedType(subMessageType);
        return fieldBuilder.build();
    }

    private static DescriptorProto buildOutUnknownFields(DescriptorProto.Builder messageTypeBuilder,
            UnknownFieldSet unknownFieldSet, AtomicInteger nestedMessageCounter)
            throws DescriptorValidationException {
        String nestedMessageName = "UNKNOWN_OBJ_" + nestedMessageCounter.get();
        for (Map.Entry<Integer, UnknownFieldSet.Field> entry : unknownFieldSet.asMap().entrySet()) {
            Integer fieldNumber = entry.getKey();
            UnknownFieldSet.Field field = entry.getValue();

            FieldDescriptorProto.Builder fieldBuilder = FieldDescriptorProto.newBuilder()
                    .setName(nestedMessageName + "_FIELD_" + fieldNumber)
                    .setNumber(fieldNumber);
            if (!field.getVarintList().isEmpty()) {
                // int32, int64, uint32, uint64, sint32, sint64, bool, enum
                fieldBuilder.setType(Type.TYPE_INT64);
                if (field.getVarintList().size() > 1) {
                    fieldBuilder.setLabel(FieldDescriptorProto.Label.LABEL_REPEATED);
                }
            } else if (!field.getFixed64List().isEmpty()) {
                // fixed64, sfixed64, double
                fieldBuilder.setType(Type.TYPE_FIXED64);
                if (field.getFixed64List().size() > 1) {
                    fieldBuilder.setLabel(FieldDescriptorProto.Label.LABEL_REPEATED);
                }
            } else if (!field.getLengthDelimitedList().isEmpty()) {
                // string, bytes, embedded messages, packed repeated fields
                FieldDescriptorProto inferredLenDelField = handleLengthDelField(field, fieldBuilder,
                        messageTypeBuilder, nestedMessageCounter);
                if (inferredLenDelField != null) {
                    messageTypeBuilder.addField(inferredLenDelField);
                    continue; // skip builder
                } else {
                    fieldBuilder.setType(Type.TYPE_BYTES);
                }
            } else if (!field.getFixed32List().isEmpty()) {
                // fixed32, sfixed32, float
                fieldBuilder.setType(Type.TYPE_FIXED32);
                if (field.getFixed32List().size() > 1) {
                    fieldBuilder.setLabel(FieldDescriptorProto.Label.LABEL_REPEATED);
                }
            } else {
                fieldBuilder.setType(Type.TYPE_BYTES);
            }
            messageTypeBuilder.addField(fieldBuilder);
        }
        return messageTypeBuilder.build();
    }

    private static Descriptor cloneWithUnknownFields(Descriptor typeDescriptor, UnknownFieldSet unknownFieldSet)
            throws DescriptorValidationException {
        AtomicInteger nestedMessageCounter = new AtomicInteger(0);
        FileDescriptor originalFd = typeDescriptor.getFile();
        FileDescriptorProto.Builder fdBuilder = FileDescriptorProto.newBuilder(originalFd.toProto());
        DescriptorProto newTypeProto = buildOutUnknownFields(typeDescriptor.toProto().toBuilder(), unknownFieldSet, nestedMessageCounter);
        fdBuilder.setMessageType(typeDescriptor.getIndex(), newTypeProto);
        FileDescriptor[] dependencies = originalFd.getDependencies().toArray(new FileDescriptor[0]);
        FileDescriptor newFd = FileDescriptor.buildFrom(fdBuilder.build(), dependencies);
        typeDescriptor.getFields();
        return newFd.findMessageTypeByName(typeDescriptor.getName());
    }

    public static Descriptor buildUnknownDescriptorFromMessage(byte[] protobufBytes) throws InvalidProtocolBufferException, DescriptorValidationException, SerializerException {
        Descriptor typeDescriptor = AugmentedDescriptorBuilder.build();
        Message message = DynamicMessage.parseFrom(typeDescriptor, protobufBytes);
        UnknownFieldSet unknownFields = message.getUnknownFields();
        return ProtoDescriptorBuilder.cloneWithUnknownFields(typeDescriptor, unknownFields);
    }
}