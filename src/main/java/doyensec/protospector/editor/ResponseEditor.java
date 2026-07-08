package doyensec.protospector.editor;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import doyensec.protospector.grpc.SerializerFactory;

public class ResponseEditor extends ProtospectorEditor implements ExtensionProvidedHttpResponseEditor {

    public ResponseEditor(MontoyaApi api,
            EditorCreationContext creationContext) {
        super(api, creationContext);
    }

    @Override
    public HttpResponse getResponse() {
        return requestResponse.response();
    }

    @Override
    public void setRequestResponse(HttpRequestResponse requestResponse) {
        String path = requestResponse.request().pathWithoutQuery();
        HttpResponse response = requestResponse.response();
        ByteArray body = response.body();
        String contentType = response.headerValue("content-type");
        super.setRequestResponse(requestResponse, path, body, contentType, false);
    }
    
    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        if (requestResponse.request() == null || requestResponse.request().toByteArray().length() == 0) {
            return false; 
        }

        if (requestResponse.response().hasHeader("Protospector")) {
            return false;
        }
        
        String contentType = requestResponse.response().headerValue("content-type");
        return contentType != null && SerializerFactory.getSerializer(contentType) != null;
    }
}
