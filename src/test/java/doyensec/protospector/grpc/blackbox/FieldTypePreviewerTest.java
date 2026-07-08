package doyensec.protospector.grpc.blackbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;

import doyensec.protospector.grpc.blackbox.FieldTypePreviewer;
import doyensec.protospector.grpc.blackbox.ProtoDescriptorBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

class FieldTypePreviewerTest {

    @Test
    void previewsScalarAndBytesFields() throws Exception {
        Message message = testMessageAllPrimitives();

        assertEquals(100, preview(message, "UNKNOWN_OBJ_0_FIELD_1", Type.INT32));
        assertEquals(200L, preview(message, "UNKNOWN_OBJ_0_FIELD_2", Type.INT64));
        assertEquals(300, preview(message, "UNKNOWN_OBJ_0_FIELD_3", Type.UINT32));
        assertEquals(400L, preview(message, "UNKNOWN_OBJ_0_FIELD_4", Type.UINT64));
        assertEquals(500, preview(message, "UNKNOWN_OBJ_0_FIELD_5", Type.SINT32));
        assertEquals(600L, preview(message, "UNKNOWN_OBJ_0_FIELD_6", Type.SINT64));
        assertEquals(700, preview(message, "UNKNOWN_OBJ_0_FIELD_7", Type.FIXED32));
        assertEquals(800L, preview(message, "UNKNOWN_OBJ_0_FIELD_8", Type.FIXED64));
        assertEquals(900, preview(message, "UNKNOWN_OBJ_0_FIELD_9", Type.SFIXED32));
        assertEquals(1000L, preview(message, "UNKNOWN_OBJ_0_FIELD_10", Type.SFIXED64));
        assertEquals(1.5f, preview(message, "UNKNOWN_OBJ_0_FIELD_11", Type.FLOAT));
        assertEquals(2.5d, preview(message, "UNKNOWN_OBJ_0_FIELD_12", Type.DOUBLE));
        assertEquals(true, preview(message, "UNKNOWN_OBJ_0_FIELD_13", Type.BOOL));
        assertEquals("string", preview(message, "UNKNOWN_OBJ_0_FIELD_14", Type.STRING));
        assertEquals(ByteString.copyFromUtf8("bytes"), preview(message, "UNKNOWN_OBJ_0_FIELD_15", Type.BYTES));
        assertEquals(7, preview(message, "UNKNOWN_OBJ_0_FIELD_16", Type.ENUM));
    }

    @Disabled // Previewing repeated fields is sometimes failing in the current implementation
    void previewsFirstRepeatedFieldValue() throws Exception {
        Message message = testMessage();

        assertEquals(5, preview(message, "UNKNOWN_OBJ_0_FIELD_1", Type.INT32));
    }

    @Test
    void previewsMessageField() throws Exception {
        Message message = testMessage();

        assertEquals("{ ... }", preview(message, "UNKNOWN_OBJ_0_FIELD_2", Type.MESSAGE));
    }

    

    private static Message testMessageAllPrimitives() throws Exception {
        Descriptor descriptor = testDescriptorAllPrimitives();

        return DynamicMessage.newBuilder(descriptor)
                .setField(descriptor.findFieldByName("int32_value"), 100)
                .setField(descriptor.findFieldByName("int64_value"), 200L)
                .setField(descriptor.findFieldByName("uint32_value"), 300)
                .setField(descriptor.findFieldByName("uint64_value"), 400L)
                .setField(descriptor.findFieldByName("sint32_value"), 500)
                .setField(descriptor.findFieldByName("sint64_value"), 600L)
                .setField(descriptor.findFieldByName("fixed32_value"), 700)
                .setField(descriptor.findFieldByName("fixed64_value"), 800L)
                .setField(descriptor.findFieldByName("sfixed32_value"), 900)
                .setField(descriptor.findFieldByName("sfixed64_value"), 1000L)
                .setField(descriptor.findFieldByName("float_value"), 1.5f)
                .setField(descriptor.findFieldByName("double_value"), 2.5d)
                .setField(descriptor.findFieldByName("bool_value"), true)
                .setField(descriptor.findFieldByName("string_value"), "string")
                .setField(descriptor.findFieldByName("bytes_value"), ByteString.copyFromUtf8("bytes"))
                .setField(descriptor.findFieldByName("enum_value"), descriptor.getFile().findEnumTypeByName("TestEnum").findValueByName("STARTED"))
                .build();
    }

    private static Descriptor testDescriptorAllPrimitives() throws Exception {
        DescriptorProto testMessage = DescriptorProto.newBuilder()
                .setName("TestAllPrimitives")
                .addField(field("int32_value", 1, FieldDescriptorProto.Type.TYPE_INT32))
                .addField(field("int64_value", 2, FieldDescriptorProto.Type.TYPE_INT64))
                .addField(field("uint32_value", 3, FieldDescriptorProto.Type.TYPE_UINT32))
                .addField(field("uint64_value", 4, FieldDescriptorProto.Type.TYPE_UINT64))
                .addField(field("sint32_value", 5, FieldDescriptorProto.Type.TYPE_SINT32))
                .addField(field("sint64_value", 6, FieldDescriptorProto.Type.TYPE_SINT64))
                .addField(field("fixed32_value", 7, FieldDescriptorProto.Type.TYPE_FIXED32))
                .addField(field("fixed64_value", 8, FieldDescriptorProto.Type.TYPE_FIXED64))
                .addField(field("sfixed32_value", 9, FieldDescriptorProto.Type.TYPE_SFIXED32))
                .addField(field("sfixed64_value", 10, FieldDescriptorProto.Type.TYPE_SFIXED64))
                .addField(field("float_value", 11, FieldDescriptorProto.Type.TYPE_FLOAT))
                .addField(field("double_value", 12, FieldDescriptorProto.Type.TYPE_DOUBLE))
                .addField(field("bool_value", 13, FieldDescriptorProto.Type.TYPE_BOOL))
                .addField(field("string_value", 14, FieldDescriptorProto.Type.TYPE_STRING))
                .addField(field("bytes_value", 15, FieldDescriptorProto.Type.TYPE_BYTES))
                .addField(field("enum_value", 16, FieldDescriptorProto.Type.TYPE_ENUM)
                        .toBuilder()
                        .setTypeName("TestEnum")
                        .build())
                .build();

        FileDescriptorProto file = FileDescriptorProto.newBuilder()
                .setName("field_type_previewer_all_primitives.proto")
                .addMessageType(testMessage)
                .addEnumType(EnumDescriptorProto.newBuilder()
                        .setName("TestEnum")
                        .addValue(EnumValueDescriptorProto.newBuilder()
                                .setName("UNKNOWN")
                                .setNumber(0))
                        .addValue(EnumValueDescriptorProto.newBuilder()
                                .setName("STARTED")
                                .setNumber(7)))
                .build();

        FileDescriptor[] dependencies = new FileDescriptor[0];
        FileDescriptor fd = FileDescriptor.buildFrom(file, dependencies);

        return fd.findMessageTypeByName("TestAllPrimitives");
    }

    private static Message testMessage() throws Exception {
        Descriptor descriptor = testDescriptor();
        Descriptor nestedDescriptor = descriptor.getFile().findMessageTypeByName("NestedMessage");

        Message nestedMessage = DynamicMessage.newBuilder(nestedDescriptor)
                .setField(nestedDescriptor.findFieldByName("nested_value"), 11)
                .build();

        return DynamicMessage.newBuilder(descriptor)
        .addRepeatedField(descriptor.findFieldByName("repeated_int32"), 5)
        .addRepeatedField(descriptor.findFieldByName("repeated_int32"), 6)
        .addRepeatedField(descriptor.findFieldByName("repeated_int32"), 7)
                .setField(descriptor.findFieldByName("nested_message"), nestedMessage)
                .build();
    }

    private static Descriptor testDescriptor() throws Exception {
        DescriptorProto nestedMessage = DescriptorProto.newBuilder()
                .setName("NestedMessage")
                .addField(field("nested_value", 3, FieldDescriptorProto.Type.TYPE_INT32))
                .build();

        DescriptorProto testMessage = DescriptorProto.newBuilder()
                .setName("TestMessage")
                .addField(field("repeated_int32", 1, FieldDescriptorProto.Type.TYPE_INT32)
                        .toBuilder()
                        .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)
                        .build())
                .addField(field("nested_message", 2, FieldDescriptorProto.Type.TYPE_MESSAGE)
                        .toBuilder()
                        .setTypeName("NestedMessage")
                        .build())
                .build();

        FileDescriptorProto file = FileDescriptorProto.newBuilder()
                .setName("field_type_previewer_test.proto")
                .setSyntax("proto3")
                .addMessageType(nestedMessage)
                .addMessageType(testMessage)
                .build();

        return FileDescriptor.buildFrom(file, new FileDescriptor[0]).findMessageTypeByName("TestMessage");
    }


    private static Object preview(Message message, String fieldName, FieldDescriptor.Type type) throws Exception {
        Descriptor descriptor = ProtoDescriptorBuilder.buildUnknownDescriptorFromMessage(message.toByteArray());
        Message unknownMessage = DynamicMessage.parseFrom(descriptor, message.toByteArray());
        FieldDescriptor field = descriptor.findFieldByName(fieldName);
        
        return new FieldTypePreviewer(field, unknownMessage).previewFieldType(type);
    }

    private static FieldDescriptorProto field(String name, int number, FieldDescriptorProto.Type type) {
        return FieldDescriptorProto.newBuilder()
                .setName(name)
                .setNumber(number)
                .setType(type)
                .build();
    }
}
