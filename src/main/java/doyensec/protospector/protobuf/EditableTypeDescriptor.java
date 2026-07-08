package doyensec.protospector.protobuf;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.io.IOException;

public class EditableTypeDescriptor {
    final FileDescriptor fileDescriptor;
    private final List<Integer> descriptorIndexPath;
    private DescriptorProto.Builder descriptorBuilder;

    public EditableTypeDescriptor(Descriptor descriptor) {
        fileDescriptor = descriptor.getFile();
        descriptorIndexPath = getDescriptorIndexPath(descriptor);
        descriptorBuilder = descriptor.toProto().toBuilder();
    }

    public EditableTypeDescriptor(byte[] bytes) throws IOException {
        FileDescriptorProto fileDescriptorProto = FileDescriptorProto.parseFrom(bytes);
        try {
            fileDescriptor = FileDescriptor.buildFrom(fileDescriptorProto, new FileDescriptor[0]);
        } catch (DescriptorValidationException e) {
            throw new IOException("Failed to build FileDescriptor", e);
        }

        descriptorIndexPath = new ArrayList<>();
        if (fileDescriptorProto.getMessageTypeCount() > 0) {
            descriptorIndexPath.add(0);
        }
        descriptorBuilder = fileDescriptorProto.getMessageType(0).toBuilder();
    }

    public synchronized DescriptorProto.Builder getBuilder() {
        return descriptorBuilder;
    }

    public synchronized String toProtoString() throws DescriptorValidationException {
        Descriptor descriptor = buildDescriptor();
        return descriptor.toProto().toString();
    }

    public synchronized void setSignature(String signature) throws ParseException, DescriptorValidationException {
        DescriptorProto.Builder parsedBuilder = DescriptorProto.newBuilder();
        TextFormat.merge(signature, parsedBuilder);

        DescriptorProto.Builder previousBuilder = descriptorBuilder;
        descriptorBuilder = parsedBuilder;
        try {
            buildDescriptor();
        } catch (DescriptorValidationException e) {
            descriptorBuilder = previousBuilder;
            throw e;
        }
    }

    public synchronized void updateFieldName(List<Integer> containingTypeIndexPath, int fieldIndex, String newName)
            throws DescriptorValidationException {
        DescriptorProto.Builder previousBuilder = descriptorBuilder.clone();
        getDescriptorBuilder(containingTypeIndexPath).getFieldBuilder(fieldIndex).setName(newName);
        validateOrRestore(previousBuilder);
    }

    public synchronized void updateFieldType(List<Integer> containingTypeIndexPath, int fieldIndex, FieldDescriptorProto.Type type)
            throws DescriptorValidationException {
        DescriptorProto.Builder previousBuilder = descriptorBuilder.clone();
        FieldDescriptorProto.Builder fieldBuilder = getDescriptorBuilder(containingTypeIndexPath).getFieldBuilder(fieldIndex);
        fieldBuilder.setType(type);
        if (!typeNeedsTypeName(type)) {
            fieldBuilder.clearTypeName();
        }
        validateOrRestore(previousBuilder);
    }

    private boolean typeNeedsTypeName(FieldDescriptorProto.Type type) {
        return type == FieldDescriptorProto.Type.TYPE_MESSAGE
                || type == FieldDescriptorProto.Type.TYPE_GROUP
                || type == FieldDescriptorProto.Type.TYPE_ENUM;
    }

    private DescriptorProto.Builder getDescriptorBuilder(List<Integer> relativeIndexPath) {
        DescriptorProto.Builder builder = descriptorBuilder;
        for (Integer nestedIndex : relativeIndexPath) {
            builder = builder.getNestedTypeBuilder(nestedIndex);
        }
        return builder;
    }

    private void validateOrRestore(DescriptorProto.Builder previousBuilder) throws DescriptorValidationException {
        try {
            buildDescriptor();
        } catch (DescriptorValidationException e) {
            descriptorBuilder = previousBuilder;
            throw e;
        }
    }

    public synchronized Descriptor buildDescriptor() throws DescriptorValidationException {
        FileDescriptorProto.Builder fileBuilder = fileDescriptor.toProto().toBuilder();
        fileBuilder.clearService();
        replaceMessageType(fileBuilder, descriptorIndexPath, descriptorBuilder.build());

        FileDescriptor rebuiltFile = FileDescriptor.buildFrom(fileBuilder.build(), getDependencies(fileDescriptor));
        return getMessageType(rebuiltFile, descriptorIndexPath);
    }

    private static List<Integer> getDescriptorIndexPath(Descriptor descriptor) {
        LinkedList<Integer> indexPath = new LinkedList<>();
        Descriptor currentDescriptor = descriptor;
        while (currentDescriptor != null) {
            indexPath.addFirst(currentDescriptor.getIndex());
            currentDescriptor = currentDescriptor.getContainingType();
        }
        return new ArrayList<>(indexPath);
    }

    private static void replaceMessageType(FileDescriptorProto.Builder fileBuilder, List<Integer> indexPath,
            DescriptorProto descriptorProto) {
        if (indexPath.size() == 1) {
            fileBuilder.setMessageType(indexPath.get(0), descriptorProto);
            return;
        }

        DescriptorProto.Builder parentBuilder = fileBuilder.getMessageTypeBuilder(indexPath.get(0));
        for (int i = 1; i < indexPath.size() - 1; i++) {
            parentBuilder = parentBuilder.getNestedTypeBuilder(indexPath.get(i));
        }
        parentBuilder.setNestedType(indexPath.get(indexPath.size() - 1), descriptorProto);
    }

    private static Descriptor getMessageType(FileDescriptor fileDescriptor, List<Integer> indexPath) {
        Descriptor currentDescriptor = fileDescriptor.getMessageTypes().get(indexPath.get(0));
        for (int i = 1; i < indexPath.size(); i++) {
            currentDescriptor = currentDescriptor.getNestedTypes().get(indexPath.get(i));
        }
        return currentDescriptor;
    }

    private static FileDescriptor[] getDependencies(FileDescriptor fileDescriptor) {
        List<FileDescriptor> dependencies = fileDescriptor.getDependencies();
        return dependencies.toArray(new FileDescriptor[dependencies.size()]);
    }
}
