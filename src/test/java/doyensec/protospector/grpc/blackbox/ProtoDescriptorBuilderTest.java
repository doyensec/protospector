package doyensec.protospector.grpc.blackbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;

import doyensec.protospector.grpc.blackbox.ProtoDescriptorBuilder;

import com.google.protobuf.UnknownFieldSet;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class ProtoDescriptorBuilderTest {

    @Test
    void testNestedMessage() throws Exception {
        Descriptor descriptor = ProtoDescriptorBuilder.buildUnknownDescriptorFromMessage(rawMessageBytes());

        assertField(descriptor, "UNKNOWN_OBJ_0_FIELD_1", 1, Type.INT64, false);
        assertField(descriptor, "UNKNOWN_OBJ_0_FIELD_2", 2, Type.STRING, false);

        assertField(descriptor, "UNKNOWN_OBJ_0_FIELD_3", 3, Type.MESSAGE, false);
        FieldDescriptor nestedField = assertField(descriptor, "UNKNOWN_OBJ_0_FIELD_3", 3, Type.MESSAGE, false);
        assertEquals("ProtospectorSubMessage_1", nestedField.getMessageType().getName());
        assertField(nestedField.getMessageType(), "UNKNOWN_OBJ_1_FIELD_1", 1, Type.INT64, false);
    }

    @Test
    void testRepeatedFields() throws Exception {
        Descriptor descriptor = ProtoDescriptorBuilder.buildUnknownDescriptorFromMessage(rawMessageBytes());


        assertField(descriptor, "UNKNOWN_OBJ_0_FIELD_1", 1, Type.INT64, false);
        assertField(descriptor, "UNKNOWN_OBJ_0_FIELD_2", 2, Type.STRING, false);
        assertField(descriptor, "UNKNOWN_OBJ_0_FIELD_3", 3, Type.MESSAGE, false);

        assertField(descriptor, "UNKNOWN_OBJ_0_FIELD_4", 4, Type.MESSAGE, true);
        FieldDescriptor repeatedField = assertField(descriptor, "UNKNOWN_OBJ_0_FIELD_4", 4, Type.MESSAGE, true);
        assertEquals("ProtospectorSubMessage_2", repeatedField.getMessageType().getName());
        assertField(repeatedField.getMessageType(), "UNKNOWN_OBJ_2_FIELD_1", 1, Type.INT64, true);
    }

    private static FieldDescriptor assertField(Descriptor descriptor, String name, int number, Type type,
            boolean repeated) {
        FieldDescriptor field = descriptor.findFieldByName(name);

        assertNotNull(field);
        assertEquals(number, field.getNumber());
        assertEquals(type, field.getType());
        assertEquals(repeated, field.isRepeated());
        return field;
    }

    private static byte[] rawMessageBytes() throws IOException {
        UnknownFieldSet unknownFields = UnknownFieldSet.newBuilder()
            .addField(1, UnknownFieldSet.Field.newBuilder()
                .addVarint(42L)
                .build())
            .addField(2, UnknownFieldSet.Field.newBuilder()
                .addLengthDelimited(ByteString.copyFromUtf8("test"))
                .build())
            .addField(3, UnknownFieldSet.Field.newBuilder()
                // nested message: a single field with varint 99
                .addLengthDelimited(UnknownFieldSet.newBuilder()
                    .addField(1, UnknownFieldSet.Field.newBuilder()
                        .addVarint(99L)
                        .build())
                    .build().toByteString())
                .build())
            // New field: repeated message, each containing a repeated int
            .addField(4, UnknownFieldSet.Field.newBuilder()
                // First message in the repeated field
                .addLengthDelimited(UnknownFieldSet.newBuilder()
                    .addField(1, UnknownFieldSet.Field.newBuilder()
                        .addVarint(100L)
                        .addVarint(101L)
                        .build())
                    .build().toByteString())
                // Second message in the repeated field
                .addLengthDelimited(UnknownFieldSet.newBuilder()
                    .addField(1, UnknownFieldSet.Field.newBuilder()
                        .addVarint(200L)
                        .addVarint(201L)
                        .addVarint(202L)
                        .build())
                    .build().toByteString())
                .build())
            .build();
    
    
        return unknownFields.toByteArray();
    }
}
