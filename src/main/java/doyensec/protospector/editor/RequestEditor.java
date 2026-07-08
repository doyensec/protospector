package doyensec.protospector.editor;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import doyensec.protospector.Protospector;
import doyensec.protospector.grpc.SerializerException;
import doyensec.protospector.grpc.SerializerFactory;
import doyensec.protospector.protobuf.MethodNotFoundException;
import doyensec.protospector.ui.UIUtils;

public class RequestEditor extends ProtospectorEditor implements ExtensionProvidedHttpRequestEditor {

    public RequestEditor(MontoyaApi api,
            EditorCreationContext creationContext) {
        super(api, creationContext);
    }

    @Override
    public HttpRequest getRequest() {
        HttpRequest request = requestResponse.request();

        if (!isModified() || readOnly) {
            return request;
        }

        ByteArray serialized;
        try {
            serialized = Protospector.serializeMessage(request.pathWithoutQuery(), burpEditor.getContents(),
                    request.headerValue("content-type"), true);
        } catch (SerializerException | MethodNotFoundException e) {
            UIUtils.errorAlert("Failed to serialize request: " + e.getMessage());
            return request;
        }

        return request.withBody(serialized);
    }

    @Override
    public void setRequestResponse(HttpRequestResponse requestResponse) {
        HttpRequest request = requestResponse.request();
        String path = request.pathWithoutQuery();
        ByteArray body = request.body();
        String contentType = request.headerValue("content-type");
        super.setRequestResponse(requestResponse, path, body, contentType, true);
    }

    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        if (requestResponse.request() == null || requestResponse.request().toByteArray().length() == 0) {
            return false; 
        }

        if (requestResponse.request().hasHeader("Protospector")) {
            return false;
        }
        
        String contentType = requestResponse.request().headerValue("content-type");
        return contentType != null && SerializerFactory.getSerializer(contentType) != null;
    }
}
