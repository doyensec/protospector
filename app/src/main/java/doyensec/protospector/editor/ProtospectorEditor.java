package doyensec.protospector.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.EditorMode;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedEditor;

import doyensec.protospector.Protospector;
import doyensec.protospector.grpc.Serializer;
import doyensec.protospector.grpc.SerializerFactory;
import doyensec.protospector.protobuf.ProtoMessageHelper;
import doyensec.protospector.protobuf.EditableMethodMap.MethodMap;
import doyensec.protospector.ui.UIModel;
import doyensec.protospector.ui.UIUtils;

public abstract class ProtospectorEditor implements ExtensionProvidedEditor {

    protected final RawEditor burpEditor;
    protected boolean readOnly;

    private final JPanel rootPanel;
    private final ProtospectorEditorPanel editorPanel;
    private final ProtospectorEditorSchemaTable schemaTable;
    private final JSplitPane splitPane;
    private final JButton typesPreviewToggleButton;

    private final ProtospectorEditorSchemaTable.FieldMutationHandler fieldMutationHandler;

    protected HttpRequestResponse requestResponse;
    private String currentPath;
    private ByteArray currentBody;
    private String currentContentType;
    private boolean currentIsRequest;

    private static final double DEFAULT_DIVIDER_PROPORTION = 0.5;

    private Boolean typesPreviewOverride;
    private double savedDividerProportion = DEFAULT_DIVIDER_PROPORTION;
    private int savedDividerSize = -1;
    private boolean wasShowing;
    private boolean dividerPositionInitialized;
    private boolean programmaticDividerChange;

    ProtospectorEditor(MontoyaApi api, EditorCreationContext creationContext) {
        readOnly = creationContext.editorMode() == EditorMode.READ_ONLY;
        burpEditor = readOnly
                ? api.userInterface().createRawEditor(EditorOptions.READ_ONLY)
                : api.userInterface().createRawEditor();

        fieldMutationHandler = new ProtospectorEditorSchemaTable.FieldMutationHandler() {
            @Override
            public void updateFieldName(List<Integer> containingTypeIndexPath, int fieldIndex, String newName)
                    throws Exception {
                MethodMap editableMethodMap = UIModel.getInstance().getEditableMethodMap();
                editableMethodMap.updateFieldName(currentPath, containingTypeIndexPath, fieldIndex, newName,
                        currentIsRequest);
                refreshCurrentMessage();
            }

            @Override
            public void updateFieldType(List<Integer> containingTypeIndexPath, int fieldIndex,
                    FieldDescriptorProto.Type type) throws Exception {
                MethodMap editableMethodMap = UIModel.getInstance().getEditableMethodMap();
                editableMethodMap.updateFieldType(currentPath, containingTypeIndexPath, fieldIndex, type,
                        currentIsRequest);
                refreshCurrentMessage();
            }
        };

        editorPanel = new ProtospectorEditorPanel(burpEditor.uiComponent());
        schemaTable = new ProtospectorEditorSchemaTable(fieldMutationHandler);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorPanel, schemaTable);
        splitPane.setResizeWeight(DEFAULT_DIVIDER_PROPORTION);
        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            if (programmaticDividerChange || !dividerPositionInitialized || !isTypesPreviewVisible()) {
                return;
            }
            int height = splitPane.getHeight();
            if (height > 0) {
                double proportion = (double) splitPane.getDividerLocation() / height;
                if (proportion > 0 && proportion < 1) {
                    savedDividerProportion = proportion;
                }
            }
        });

        typesPreviewToggleButton = new JButton();
        typesPreviewToggleButton.addActionListener(e -> toggleTypesPreview());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(typesPreviewToggleButton);

        rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(toolbar, BorderLayout.NORTH);
        rootPanel.add(splitPane, BorderLayout.CENTER);

        applyTypesPreviewVisibility();
        UIModel.getInstance().getProtospectorSettings()
                .addTypesPreviewTabVisibilityListener(this::onGlobalTypesPreviewSettingChanged);

        wasShowing = splitPane.isShowing();
        splitPane.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }
            boolean isShowing = splitPane.isShowing();
            if (isShowing && !wasShowing) {
                refreshCurrentMessageWhenShown();
            }
            wasShowing = isShowing;
        });
    }

    public void setRequestResponse(HttpRequestResponse requestResponse, String path, ByteArray body,
            String contentType, boolean isRequest) {
        this.requestResponse = requestResponse;
        currentPath = path;
        currentBody = body;
        currentContentType = contentType;
        currentIsRequest = isRequest;
        burpEditor.setContents(ByteArray.byteArrayOfLength(0));
        schemaTable.setSchema(null, null);
        new SwingWorker<EditorLoadResult, Void>() {
            @Override
            protected EditorLoadResult doInBackground() throws Exception {
                ByteArray deserialized = Protospector.deserializeMessage(path, body, contentType, isRequest);
                MethodMap editableMethodMap = UIModel.getInstance().getEditableMethodMap();
                Descriptor typeDescriptor = editableMethodMap.getTypeDescriptor(path, isRequest);
                Serializer serializer = SerializerFactory.getSerializer(contentType);
                Message message = ProtoMessageHelper.wireBytesToMessage(serializer.getProtoByteArray(body).getBytes(),
                        typeDescriptor);
                return new EditorLoadResult(deserialized, typeDescriptor, message);
            }

            @Override
            protected void done() {
                EditorLoadResult result;
                try {
                    result = get();
                } catch (Exception e) {
                    UIUtils.errorAlert("Failed to load editor: " + e.getMessage());
                    return;
                }
                burpEditor.setContents(result.deserialized);
                schemaTable.setSchema(result.typeDescriptor, result.message);
            }
        }.execute();
    }

    @Override
    public String caption() {
        return "ProtoSpector";
    }

    @Override
    public Component uiComponent() {
        return rootPanel;
    }

    @Override
    public Selection selectedData() {
        return burpEditor.selection().isPresent() ? burpEditor.selection().get() : null;
    }

    @Override
    public boolean isModified() {
        return burpEditor.isModified();
    }

    private void refreshCurrentMessage() {
        if (requestResponse == null || currentPath == null || currentBody == null) {
            return;
        }
        setRequestResponse(requestResponse, currentPath, currentBody, currentContentType, currentIsRequest);
    }

    private void refreshCurrentMessageWhenShown() {
        if (isModified()) {
            return;
        }
        refreshCurrentMessage();
    }

    private void onGlobalTypesPreviewSettingChanged() {
        typesPreviewOverride = null;
        applyTypesPreviewVisibility();
    }

    private void toggleTypesPreview() {
        typesPreviewOverride = !isTypesPreviewVisible();
        applyTypesPreviewVisibility();
    }

    private boolean isTypesPreviewVisible() {
        if (typesPreviewOverride != null) {
            return typesPreviewOverride;
        }
        return UIModel.getInstance().getProtospectorSettings().isTypesPreviewTabVisible();
    }

    private void applyTypesPreviewVisibility() {
        boolean visible = isTypesPreviewVisible();
        if (visible) {
            schemaTable.setVisible(true);
            if (savedDividerSize < 0) {
                savedDividerSize = splitPane.getDividerSize();
            }
            splitPane.setDividerSize(savedDividerSize > 0 ? savedDividerSize : 5);
            restoreDividerLocation();
        } else {
            saveDividerLocation();
            savedDividerSize = splitPane.getDividerSize();
            setDividerProportion(1.0);
            splitPane.setDividerSize(0);
            schemaTable.setVisible(false);
        }
        typesPreviewToggleButton.setText(visible ? "Hide field list" : "Show field list");
        splitPane.revalidate();
        splitPane.repaint();
    }

    private void saveDividerLocation() {
        int height = splitPane.getHeight();
        if (height > 0) {
            double proportion = (double) splitPane.getDividerLocation() / height;
            if (proportion > 0 && proportion < 1) {
                savedDividerProportion = proportion;
            }
        }
    }

    private void restoreDividerLocation() {
        setDividerProportion(savedDividerProportion);
    }

    private void setDividerProportion(double proportion) {
        Runnable apply = () -> {
            if (splitPane.getHeight() <= 0) {
                return;
            }
            programmaticDividerChange = true;
            try {
                splitPane.setDividerLocation(proportion);
                dividerPositionInitialized = true;
            } finally {
                programmaticDividerChange = false;
            }
        };
        if (splitPane.getHeight() > 0) {
            apply.run();
        } else {
            splitPane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    if (splitPane.getHeight() > 0) {
                        splitPane.removeComponentListener(this);
                        apply.run();
                    }
                }
            });
        }
    }

    private static final class EditorLoadResult {
        private final ByteArray deserialized;
        private final Descriptor typeDescriptor;
        private final Message message;

        EditorLoadResult(ByteArray deserialized, Descriptor typeDescriptor, Message message) {
            this.deserialized = deserialized;
            this.typeDescriptor = typeDescriptor;
            this.message = message;
        }
    }
}
