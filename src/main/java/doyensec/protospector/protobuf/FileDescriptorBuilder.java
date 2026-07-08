
package doyensec.protospector.protobuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;

public class FileDescriptorBuilder {

   private Map<String, FileDescriptorProto> fdProtoMap;
   private Map<String, FileDescriptor> fdMap;

   public FileDescriptorBuilder(Map<String, FileDescriptorProto> fdProtoMap) {
      this.fdProtoMap = fdProtoMap;
      this.fdMap = new HashMap<>();
   }

   public static FileDescriptorBuilder fromFileDescriptorSet(FileDescriptorSet fdSet) {
      List<FileDescriptorProto> fdProtoList = fdSet.getFileList();
      Map<String, FileDescriptorProto> fdProtoMap = new HashMap<>();
      for (FileDescriptorProto fd : fdProtoList) {
         fdProtoMap.put(fd.getName(), fd);
      }
      return new FileDescriptorBuilder(fdProtoMap);
   }

   public List<FileDescriptor> buildAllFileDescriptors() throws DescriptorValidationException {
      List<FileDescriptor> result = new ArrayList<>();
      for (FileDescriptorProto fdProto : fdProtoMap.values()) {
         FileDescriptor fd = buildFileDescriptor(fdProto.getName());
         result.add(fd);
      }
      return result;
   }

   public FileDescriptor buildFileDescriptor(String currName) throws DescriptorValidationException {
      FileDescriptor lookup = fdMap.get(currName);
      if (lookup != null) {
         return lookup;
      }

      FileDescriptorProto curr = fdProtoMap.get(currName);
      List<String> dependencyNames = curr.getDependencyList();
      FileDescriptor[] dependencyFds = new FileDescriptor[dependencyNames.size()];
      for (int i = 0; i < dependencyNames.size(); i++) {
         dependencyFds[i] = buildFileDescriptor(dependencyNames.get(i));
      }

      FileDescriptor fd = FileDescriptor.buildFrom(curr, dependencyFds);
      fdMap.put(currName, fd);
      return fd;
   }
}
