package doyensec.protospector.protobuf.EditableMethodMap;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.TextFormat.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.Persistence;
import burp.api.montoya.persistence.PersistedObject;
import doyensec.protospector.grpc.blackbox.AugmentedDescriptorBuilder;
import doyensec.protospector.protobuf.FileDescriptorBuilder;
import doyensec.protospector.protobuf.MethodNotFoundException;

public class MethodMap {
    private static final String PERSISTED_METHOD_MAP_KEY = "methodMap";
    private static final String INPUT_SUFFIX = "::input";
    private static final String OUTPUT_SUFFIX = "::output";

    private ConcurrentSkipListMap<String, MethodTypes> internalMap;
    private PersistedObject persistedObject;
    private final Persistence persistence;

    public MethodMap(MontoyaApi api, boolean loadPersistentDescriptors) {
        this.persistence = api.persistence();
        this.persistedObject = getOrCreatePersistedObject();
        this.internalMap = new ConcurrentSkipListMap<>();

        try {
            if (loadPersistentDescriptors) {
                loadPersistentDescriptors();
            }
        } catch (MethodNotFoundException | DescriptorValidationException | ParseException e) {
            throw new RuntimeException("Failed to load persistent descriptor", e);
        }
    }

    public void clear() {
        internalMap.clear();
        persistence.extensionData().deleteChildObject(PERSISTED_METHOD_MAP_KEY);
        this.persistedObject = PersistedObject.persistedObject();
        persistence.extensionData().setChildObject(PERSISTED_METHOD_MAP_KEY, this.persistedObject);
    }

    public void loadDescriptorFile(File file)
            throws IOException, DescriptorValidationException {
        FileInputStream fs = new FileInputStream(file);
        loadFileDescriptorSet(FileDescriptorSet.parseFrom(fs));
    }

    public void saveDescriptorFile(File file) throws IOException {
        FileDescriptorSet.Builder setBuilder = FileDescriptorSet.newBuilder();
        
        for (String path : internalMap.keySet()) {
            try {
                setBuilder.addFile(internalMap.get(path).buildMethodFileDescriptor(path));
            } catch (MethodNotFoundException e) {
                continue;
            }
        }
        FileOutputStream fs = new FileOutputStream(file);
        setBuilder.build().writeTo(fs);
        fs.close();
    }

    // Always in sorted order
    public List<String> getMethodPaths() {
        return new ArrayList<String>(internalMap.keySet());
    }

    public Descriptor getTypeDescriptor(String path, boolean isInput)
            throws MethodNotFoundException, DescriptorValidationException {
        MethodTypes methodTypes = getMethodTypesByPath(path);
        return isInput ? methodTypes.getInputTypeDescriptor() : methodTypes.getOutputTypeDescriptor();
    }

    public String getTypeDescriptorSignature(String path, boolean isInput) throws MethodNotFoundException {
        MethodTypes methodTypes = getMethodTypesByPath(path);
        return isInput ? methodTypes.getInputTypeDescriptorBuilder().build().toString() : methodTypes.getOutputTypeDescriptorBuilder().build().toString();
    }

    public void setTypeDescriptorSignature(String path, String signature, boolean isInput)
            throws MethodNotFoundException, ParseException, DescriptorValidationException {
        getMethodTypesByPath(path).setTypeDescriptorSignature(signature, isInput);
        try {
            savePersistentDescriptor(path, isInput);
        } catch (MethodNotFoundException | DescriptorValidationException e) {
            throw new RuntimeException("Failed to save persistent descriptor", e);
        }
    }

    public void putTypeDescriptorSignature(String path, String signature, boolean isInput)
            throws MethodNotFoundException, ParseException, DescriptorValidationException {
        Descriptor descriptor = AugmentedDescriptorBuilder.build();
        MethodTypes methodTypes = internalMap.computeIfAbsent(path, unused -> new MethodTypes());
        if (isInput) {
            methodTypes.setInputTypeDescriptor(descriptor);
        } else {
            methodTypes.setOutputTypeDescriptor(descriptor);
        }
        setTypeDescriptorSignature(path, signature, isInput);
    }

    public void putTypeDescriptor(String path, Descriptor descriptor, boolean isInput) {
        MethodTypes methodTypes = internalMap.computeIfAbsent(path, unused -> new MethodTypes());
        if (isInput) {
            methodTypes.setInputTypeDescriptor(descriptor);
        } else {
            methodTypes.setOutputTypeDescriptor(descriptor);
        }
        try {
            savePersistentDescriptor(path, isInput);
        } catch (MethodNotFoundException | DescriptorValidationException e) {
            throw new RuntimeException("Failed to save persistent descriptor", e);
        }
    }

    public void removeTypeDescriptor(String path, boolean isInput) {
        MethodTypes methodTypes = internalMap.get(path);
        if (methodTypes != null) {
            methodTypes.removeTypeDescriptor(isInput);
        }
        this.persistedObject = getOrCreatePersistedObject();
        persistedObject.deleteString(path + (isInput ? INPUT_SUFFIX : OUTPUT_SUFFIX));
        this.persistence.extensionData().setChildObject(PERSISTED_METHOD_MAP_KEY, this.persistedObject);
    }

    public void updateFieldName(String path, List<Integer> containingTypeIndexPath, int fieldIndex, String newName,
            boolean isInput) throws MethodNotFoundException, DescriptorValidationException {
        getMethodTypesByPath(path).updateFieldName(containingTypeIndexPath, fieldIndex, newName, isInput);
        try {
            savePersistentDescriptor(path, isInput);
        } catch (MethodNotFoundException | DescriptorValidationException e) {
            throw new RuntimeException("Failed to save persistent descriptor", e);
        }
    }

    public void updateFieldType(String path, List<Integer> containingTypeIndexPath, int fieldIndex,
            FieldDescriptorProto.Type type, boolean isInput)
            throws MethodNotFoundException, DescriptorValidationException {
        getMethodTypesByPath(path).updateFieldType(containingTypeIndexPath, fieldIndex, type, isInput);
        try {
            savePersistentDescriptor(path, isInput);
        } catch (MethodNotFoundException | DescriptorValidationException e) {
            throw new RuntimeException("Failed to save persistent descriptor", e);
        }
    }

    private MethodTypes getMethodTypesByPath(String path) throws MethodNotFoundException {
        MethodTypes result = internalMap.get(path);
        if (result == null) {
            throw new MethodNotFoundException(path);
        }
        return result;
    }

    private void loadFileDescriptorSet(FileDescriptorSet fdSet) throws DescriptorValidationException {
        clear();
        FileDescriptorBuilder builder = FileDescriptorBuilder.fromFileDescriptorSet(fdSet);
        for (FileDescriptor fd : builder.buildAllFileDescriptors()) {
            for (ServiceDescriptor service : fd.getServices()) {
                for (MethodDescriptor method : service.getMethods()) {
                    putMethod(method);
                }
            }
        }
    }

    public void putMethod(MethodDescriptor methodDescriptor) {
        String methodName = methodDescriptor.getFullName();
        String methodPath = methodNameToPath(methodName);
        internalMap.put(methodPath, new MethodTypes(methodDescriptor));
        try {
            savePersistentDescriptor(methodPath, true);
            savePersistentDescriptor(methodPath, false);
        } catch (MethodNotFoundException | DescriptorValidationException e) {
            throw new RuntimeException("Failed to save persistent descriptor", e);
        }
    }

    private PersistedObject getOrCreatePersistedObject() {
        PersistedObject object = persistence.extensionData().getChildObject(PERSISTED_METHOD_MAP_KEY);
        if (object == null) {
            object = PersistedObject.persistedObject();
            persistence.extensionData().setChildObject(PERSISTED_METHOD_MAP_KEY, object);
        }
        return object;
    }

    private void loadPersistentDescriptors()
            throws MethodNotFoundException, DescriptorValidationException, ParseException {

        for (String key : persistedObject.stringKeys()) {
            if (key.endsWith(INPUT_SUFFIX)) {
                String path = key.substring(0, key.length() - INPUT_SUFFIX.length());
                putTypeDescriptorSignature(path, persistedObject.getString(key), true);
            } else if (key.endsWith(OUTPUT_SUFFIX)) {
                String path = key.substring(0, key.length() - OUTPUT_SUFFIX.length());
                putTypeDescriptorSignature(path, persistedObject.getString(key), false);
            }
        }
    }

    private void savePersistentDescriptor(String path, Boolean isInput) throws MethodNotFoundException, DescriptorValidationException {
        this.persistedObject = getOrCreatePersistedObject();
        persistedObject.setString(path + (isInput ? INPUT_SUFFIX : OUTPUT_SUFFIX),
                internalMap.get(path).getDescriptorSignature(isInput));
        this.persistence.extensionData().setChildObject(PERSISTED_METHOD_MAP_KEY, this.persistedObject);
    }

    static private String methodNameToPath(String name) {
        StringBuffer sb = new StringBuffer(name);
        int lastDotI = name.lastIndexOf(".");
        sb.replace(lastDotI, lastDotI + 1, "/");
        sb.insert(0, "/");
        return sb.toString();
    }
}