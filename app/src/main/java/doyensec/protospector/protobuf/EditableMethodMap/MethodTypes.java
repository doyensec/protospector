package doyensec.protospector.protobuf.EditableMethodMap;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.TextFormat.ParseException;
import com.google.protobuf.Descriptors.MethodDescriptor;

import doyensec.protospector.protobuf.EditableTypeDescriptor;
import doyensec.protospector.protobuf.MethodNotFoundException;

import java.util.List;

public class MethodTypes {
    EditableTypeDescriptor inputTypeDescriptor;
    EditableTypeDescriptor outputTypeDescriptor;

    MethodTypes() {
    }

    MethodTypes(MethodDescriptor methodDescriptor) {
        setInputTypeDescriptor(methodDescriptor.getInputType());
        setOutputTypeDescriptor(methodDescriptor.getOutputType());
    }

    synchronized Descriptor getInputTypeDescriptor() throws MethodNotFoundException, DescriptorValidationException {
        if (inputTypeDescriptor == null) {
            throw new MethodNotFoundException("input descriptor");
        }
        return inputTypeDescriptor.buildDescriptor();
    }

    synchronized Descriptor getOutputTypeDescriptor() throws MethodNotFoundException, DescriptorValidationException {
        if (outputTypeDescriptor == null) {
            throw new MethodNotFoundException("output descriptor");
        }
        return outputTypeDescriptor.buildDescriptor();
    }

    synchronized DescriptorProto.Builder getInputTypeDescriptorBuilder() throws MethodNotFoundException {
        if (inputTypeDescriptor == null) {
            throw new MethodNotFoundException("input descriptor");
        }
        return inputTypeDescriptor.getBuilder();
    }

    synchronized DescriptorProto.Builder getOutputTypeDescriptorBuilder() throws MethodNotFoundException {
        if (outputTypeDescriptor == null) {
            throw new MethodNotFoundException("output descriptor");
        }
        return outputTypeDescriptor.getBuilder();
    }

    synchronized void setInputTypeDescriptor(Descriptor descriptor) {
        inputTypeDescriptor = new EditableTypeDescriptor(descriptor);
    }

    synchronized void setOutputTypeDescriptor(Descriptor descriptor) {
        outputTypeDescriptor = new EditableTypeDescriptor(descriptor);
    }

    synchronized void setTypeDescriptorSignature(String signature, boolean isInput)
            throws MethodNotFoundException, ParseException, DescriptorValidationException {
        if (isInput) {
            if (inputTypeDescriptor == null) {
                throw new MethodNotFoundException("input descriptor");
            }
            inputTypeDescriptor.setSignature(signature);
        } else {
            if (outputTypeDescriptor == null) {
                throw new MethodNotFoundException("output descriptor");
            }
            outputTypeDescriptor.setSignature(signature);
        }
    }

    synchronized void updateFieldName(List<Integer> containingTypeIndexPath, int fieldIndex, String newName,
            boolean isInput) throws MethodNotFoundException, DescriptorValidationException {
        getEditableTypeDescriptor(isInput).updateFieldName(containingTypeIndexPath, fieldIndex, newName);
    }

    synchronized void updateFieldType(List<Integer> containingTypeIndexPath, int fieldIndex,
            FieldDescriptorProto.Type type, boolean isInput)
            throws MethodNotFoundException, DescriptorValidationException {
        getEditableTypeDescriptor(isInput).updateFieldType(containingTypeIndexPath, fieldIndex, type);
    }

    private EditableTypeDescriptor getEditableTypeDescriptor(boolean isInput) throws MethodNotFoundException {
        EditableTypeDescriptor typeDescriptor = isInput ? inputTypeDescriptor : outputTypeDescriptor;
        if (typeDescriptor == null) {
            throw new MethodNotFoundException(isInput ? "input descriptor" : "output descriptor");
        }
        return typeDescriptor;
    }

    synchronized String getDescriptorSignature(boolean isInput) throws MethodNotFoundException, DescriptorValidationException {
        return getEditableTypeDescriptor(isInput).toProtoString();
    }

    synchronized FileDescriptorProto buildMethodFileDescriptor(String path) throws MethodNotFoundException {
        String[] pathParts = path.split("/", 3);
        if (pathParts.length != 3 || !pathParts[0].isEmpty() || pathParts[1].isEmpty() || pathParts[2].isEmpty()) {
            throw new MethodNotFoundException(path);
        }

        String serviceFullName = pathParts[1];
        String methodName = pathParts[2];
        int packageSeparator = serviceFullName.lastIndexOf(".");
        String packageName = packageSeparator == -1 ? "" : serviceFullName.substring(0, packageSeparator);
        String serviceName = packageSeparator == -1 ? serviceFullName : serviceFullName.substring(packageSeparator + 1);

        DescriptorProto inputType = getInputTypeDescriptorBuilder().build();
        DescriptorProto outputType = getOutputTypeDescriptorBuilder().build();

        MethodDescriptorProto method = MethodDescriptorProto.newBuilder()
                .setName(methodName)
                .setInputType(qualifiedTypeName(packageName, inputType.getName()))
                .setOutputType(qualifiedTypeName(packageName, outputType.getName()))
                .build();

        ServiceDescriptorProto service = ServiceDescriptorProto.newBuilder()
                .setName(serviceName)
                .addMethod(method)
                .build();

        FileDescriptorProto.Builder builder = FileDescriptorProto.newBuilder()
                .setName(fileNameForPath(path))
                .setSyntax("proto3")
                .addMessageType(inputType)
                .addMessageType(outputType)
                .addService(service);
        if (!packageName.isEmpty()) {
            builder.setPackage(packageName);
        }
        return builder.build();
    }

    private static String qualifiedTypeName(String packageName, String typeName) {
        if (packageName.isEmpty()) {
            return "." + typeName;
        }
        return "." + packageName + "." + typeName;
    }

    private static String fileNameForPath(String path) {
        return path.replaceAll("[^A-Za-z0-9_]", "_") + ".proto";
    }
}
