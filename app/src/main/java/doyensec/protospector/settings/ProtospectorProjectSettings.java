package doyensec.protospector.settings;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import burp.api.montoya.persistence.Persistence;
import burp.api.montoya.MontoyaApi;

public class ProtospectorProjectSettings {
    private static final String TYPES_PREVIEW_TAB_VISIBLE_KEY = "typesPreviewTabVisible";

    private String repeaterBaseURL;
    private String contentType;
    private boolean typesPreviewTabVisible;
    private final Persistence persistence;
    private final List<Runnable> typesPreviewTabVisibilityChangeListeners = new CopyOnWriteArrayList<>();

    public ProtospectorProjectSettings(MontoyaApi api) {
        this.persistence = api.persistence();
        this.repeaterBaseURL = persistence.extensionData().getString("repeaterBaseURL");
        this.contentType = persistence.extensionData().getString("contentType");
        if (this.contentType == null) {
            this.contentType = "application/grpc-web-text+proto";
        }
        String typesPreviewTabVisibleValue = persistence.extensionData().getString(TYPES_PREVIEW_TAB_VISIBLE_KEY);
        this.typesPreviewTabVisible = typesPreviewTabVisibleValue == null
                || Boolean.parseBoolean(typesPreviewTabVisibleValue);
    }

    public String getRepeaterBaseURL() {
        return repeaterBaseURL;
    }

    public void setRepeaterBaseURL(String repeaterBaseURL) {
        this.repeaterBaseURL = repeaterBaseURL;
        persistence.extensionData().setString("repeaterBaseURL", repeaterBaseURL);
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
        persistence.extensionData().setString("contentType", contentType);
    }

    public boolean isTypesPreviewTabVisible() {
        return typesPreviewTabVisible;
    }

    public void setTypesPreviewTabVisible(boolean typesPreviewTabVisible) {
        if (this.typesPreviewTabVisible == typesPreviewTabVisible) {
            return;
        }
        this.typesPreviewTabVisible = typesPreviewTabVisible;
        persistence.extensionData().setString(TYPES_PREVIEW_TAB_VISIBLE_KEY, Boolean.toString(typesPreviewTabVisible));
        typesPreviewTabVisibilityChangeListeners.forEach(Runnable::run);
    }

    public void addTypesPreviewTabVisibilityListener(Runnable listener) {
        typesPreviewTabVisibilityChangeListeners.add(listener);
    }
}
