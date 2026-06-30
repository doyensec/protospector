package doyensec.protospector.protobuf;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.DynamicMessage.Builder;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

public class ProtoMessageHelper {
    public static Message wireBytesToMessage(byte[] wire, Descriptor typeDescriptor)
            throws InvalidProtocolBufferException {
        Message.Builder builder = DynamicMessage.newBuilder(typeDescriptor);
        builder.mergeFrom(wire);
        return builder.build();
    }

    public static String messageToJsonStr(Message message) throws InvalidProtocolBufferException {
        return JsonFormat.printer().includingDefaultValueFields().preservingProtoFieldNames().print(message);
    }

    public static String typeDescriptorToJsonStr(Descriptor typeDescriptor) throws InvalidProtocolBufferException {
        Builder builder = DynamicMessage.newBuilder(typeDescriptor);
        // Avoid nested messages not showing up
        initializeNestedMessages(builder);
        return messageToJsonStr(builder.buildPartial()); 
    }
    
    private static void initializeNestedMessages(Message.Builder builder) {
        Descriptor descriptor = builder.getDescriptorForType();
        
        for (FieldDescriptor field : descriptor.getFields()) {
            if (field.getType() == FieldDescriptor.Type.MESSAGE && !field.isRepeated() && !field.isMapField()) {
                Message.Builder subBuilder = builder.newBuilderForField(field);
                initializeNestedMessages(subBuilder);
                builder.setField(field, subBuilder.buildPartial());
            }
        }
    }

    public static Message JsonStrToMessage(String json, Descriptor typeDescriptor)
            throws InvalidProtocolBufferException {
        Message.Builder builder = DynamicMessage.newBuilder(typeDescriptor);
        JsonFormat.parser().merge(json, builder);
        return builder.build();
    }
}
