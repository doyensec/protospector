package doyensec.protospector.ui;

import doyensec.protospector.protobuf.EditableMethodMap.MethodMap;
import doyensec.protospector.settings.ProtospectorProjectSettings;
import burp.api.montoya.MontoyaApi;

public class UIModel {
    private Runnable onMethodsLoaded;
    private ProtospectorProjectSettings protospectorSettings;
    private MontoyaApi api;
    private MethodMap editableMethodMap;

    private static UIModel instance;

    private UIModel(MontoyaApi api, MethodMap editableMethodMap, ProtospectorProjectSettings protospectorSettings) {
        this.api = api;
        this.editableMethodMap = editableMethodMap;
        this.protospectorSettings = protospectorSettings;
    }

    public static UIModel getInstance(MontoyaApi api, MethodMap editableMethodMap, ProtospectorProjectSettings protospectorSettings) {
        if (instance == null) {
            instance = new UIModel(api, editableMethodMap, protospectorSettings);
        }
        return instance;
    }

    public static UIModel getInstance() {
        if (instance == null) {
            throw new IllegalStateException("UIModel not initialized");
        }
        return instance;
    }

    public void extensionUnload() {
        instance = null;
        editableMethodMap = null;
        protospectorSettings = null;
        api = null;
    }

    public MethodMap getEditableMethodMap() {
        return editableMethodMap;
    }

    public ProtospectorProjectSettings getProtospectorSettings() {
        return protospectorSettings;
    }

    public void setProtospectorSettings(ProtospectorProjectSettings protospectorSettings) {
        this.protospectorSettings = protospectorSettings;
    }

    public void setOnMethodsChanged(Runnable onMethodsLoaded) {
        this.onMethodsLoaded = onMethodsLoaded;
    }

    public void notifyMethodsChanged() {
        if (onMethodsLoaded != null) {
            onMethodsLoaded.run();
        }
    }

    public MontoyaApi getApi() {
        return api;
    }

    public void setApi(MontoyaApi api) {
        this.api = api;
    }
}
