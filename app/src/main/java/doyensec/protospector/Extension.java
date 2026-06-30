package doyensec.protospector;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import doyensec.protospector.editor.RequestEditor;
import doyensec.protospector.editor.ResponseEditor;
import doyensec.protospector.grpc.GrpcTextJsonSerializer;
import doyensec.protospector.ui.ProtospectorTab;
import doyensec.protospector.ui.UIModel;
import doyensec.protospector.protobuf.EditableMethodMap.MethodMap;
import doyensec.protospector.settings.ProtospectorProjectSettings;

public class Extension implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        api.extension().registerUnloadingHandler(this::extensionUnload);
        
        GrpcTextJsonSerializer.initialize(api.utilities().base64Utils());

        MethodMap editableMethodMap = null;
        try {
            editableMethodMap = new MethodMap(api, true);
        } catch (Exception e) {
            api.logging().logToError("Failed to initialize method map: " + e.getMessage());
            editableMethodMap = new MethodMap(api, false);
            editableMethodMap.clear();
        }

        ProtospectorProjectSettings protospectorSettings = new ProtospectorProjectSettings(api);
        
        UIModel.getInstance(api, editableMethodMap, protospectorSettings);
        ProtospectorTab tab = new ProtospectorTab();

        api.extension().setName("ProtoSpector");
        api.userInterface().registerSuiteTab("ProtoSpector", tab);
        api.userInterface().registerHttpRequestEditorProvider(
                editorCreationContext -> new RequestEditor(api, editorCreationContext));
        api.userInterface().registerHttpResponseEditorProvider(
                editorCreationContext -> new ResponseEditor(api, editorCreationContext));
    }

    public void extensionUnload() {
        try {
            UIModel.getInstance().extensionUnload();
        } catch (IllegalStateException e) {
            // UIModel not initialized
        }
        GrpcTextJsonSerializer.extensionUnload();
    }
}
