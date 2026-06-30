package doyensec.protospector.grpc.blackbox;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;

public abstract class AugmentedDescriptorBuilder {
    private static final String FILE_NAME = "protospector_blackbox.proto";
    private static final String MESSAGE_NAME = "ProtospectorBlackboxMessage";

    public static Descriptor build() throws DescriptorValidationException {
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "_");
        String messageName = MESSAGE_NAME + "_" + uuid;

        DescriptorProto messageType = DescriptorProto.newBuilder()
                .setName(messageName)
                .build();
           

        FileDescriptorProto fileType = FileDescriptorProto.newBuilder()
                .setName(FILE_NAME)
                .setSyntax("proto3")
                .addMessageType(messageType)
                .build();

        return FileDescriptor.buildFrom(fileType, new FileDescriptor[0])
                .findMessageTypeByName(messageName);
    }
}
