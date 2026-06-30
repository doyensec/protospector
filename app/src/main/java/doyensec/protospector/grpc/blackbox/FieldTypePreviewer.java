package doyensec.protospector.grpc.blackbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

public class FieldTypePreviewer {

    private final byte[] rawFieldBytes;

    public FieldTypePreviewer(FieldDescriptor field, Message message) throws IOException {
        if (field == null) {
            throw new IllegalArgumentException("field not found");
        }
        this.rawFieldBytes = extractRawFieldBytes(field, message);
    }

    // Based on WireFormat.readPrimitiveField
    public Object previewFieldType(FieldDescriptor.Type type) throws IOException {
        CodedInputStream input = createCodedInputStream();
        switch (type) {
            case DOUBLE:
                return input.readDouble();
            case FLOAT:
                return input.readFloat();
            case INT64:
                return input.readInt64();
            case UINT64:
                return input.readUInt64();
            case INT32:
                return input.readInt32();
            case FIXED64:
                return input.readFixed64();
            case FIXED32:
                return input.readFixed32();
            case BOOL:
                return input.readBool();
            case BYTES:
                return input.readBytes();
            case UINT32:
                return input.readUInt32();
            case SFIXED32:
                return input.readSFixed32();
            case SFIXED64:
                return input.readSFixed64();
            case SINT32:
                return input.readSInt32();
            case SINT64:
                return input.readSInt64();
            case STRING:
                return input.readString();
            case GROUP:
                throw new IllegalArgumentException("groups are not supported");
            case MESSAGE:
                return "{ ... }"; // Generating message previews is too expensive
            case ENUM:
                return input.readEnum();
        }

        throw new RuntimeException();
    }

    private CodedInputStream createCodedInputStream() {
        return CodedInputStream.newInstance(rawFieldBytes);
    }

    private static byte[] extractRawFieldBytes(FieldDescriptor field, Message message) throws IOException {
        if (field.isRepeated()) {
            if (message.getRepeatedFieldCount(field) == 0) {
                return new byte[0];
            }
            return extractRawFieldNoTag(field.getType(), message.getRepeatedField(field, 0));
        }
    
        Object value = message.getField(field);
        return extractRawFieldNoTag(field.getType(), value);
    }

    // Based on FieldSet.writeElementNoTag
    private static byte[] extractRawFieldNoTag(FieldDescriptor.Type type, Object value) throws IOException {
        ByteArrayOutputStream outputBuff = new ByteArrayOutputStream();
        CodedOutputStream output = CodedOutputStream.newInstance(outputBuff);
        switch (type) {
            case DOUBLE:
                output.writeDoubleNoTag((Double) value);
                break;
            case FLOAT:
                output.writeFloatNoTag((Float) value);
                break;
            case INT64:
                output.writeInt64NoTag((Long) value);
                break;
            case UINT64:
                output.writeUInt64NoTag((Long) value);
                break;
            case INT32:
                output.writeInt32NoTag((Integer) value);
                break;
            case FIXED64:
                output.writeFixed64NoTag((Long) value);
                break;
            case FIXED32:
                output.writeFixed32NoTag((Integer) value);
                break;
            case BOOL:
                output.writeBoolNoTag((Boolean) value);
                break;
            case GROUP:
                output.writeGroupNoTag((Message) value);
                break;
            case MESSAGE:
                output.writeMessageNoTag((Message) value);
                break;
            case STRING:
                if (value instanceof ByteString) {
                    output.writeBytesNoTag((ByteString) value);
                } else {
                    output.writeStringNoTag((String) value);
                }
                break;
            case BYTES:
                if (value instanceof ByteString) {
                    output.writeBytesNoTag((ByteString) value);
                } else {
                    output.writeByteArrayNoTag((byte[]) value);
                }
                break;
            case UINT32:
                output.writeUInt32NoTag((Integer) value);
                break;
            case SFIXED32:
                output.writeSFixed32NoTag((Integer) value);
                break;
            case SFIXED64:
                output.writeSFixed64NoTag((Long) value);
                break;
            case SINT32:
                output.writeSInt32NoTag((Integer) value);
                break;
            case SINT64:
                output.writeSInt64NoTag((Long) value);
                break;
            case ENUM:
                if (value instanceof EnumValueDescriptor) {
                    output.writeEnumNoTag(((EnumValueDescriptor) value).getNumber());
                } else {
                    output.writeEnumNoTag(((Integer) value).intValue());
                }
                break;
        }
        output.flush();
        return outputBuff.toByteArray();
    }
}
