package doyensec.protospector.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat.ParseException;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import doyensec.protospector.Protospector;
import doyensec.protospector.grpc.SerializerException;
import doyensec.protospector.protobuf.MethodNotFoundException;
import doyensec.protospector.protobuf.EditableMethodMap.MethodMap;

public class EditableMethodsPage extends JPanel {
    private final MethodMap methodMap;
    private final DefaultListModel<String> methodListModel;
    private final List<String> allMethodPaths;
    private JList<String> methodList;
    private JTextField methodSearchField;
    private JTextArea requestJsonArea;
    private JTextArea requestSignatureArea;
    private JTextArea responseJsonArea;
    private JTextArea responseSignatureArea;

    EditableMethodsPage() {
        this.methodMap = UIModel.getInstance().getEditableMethodMap();
        this.methodListModel = new DefaultListModel<>();
        this.allMethodPaths = new ArrayList<>();

        setLayout(new GridLayout(1, 3));
        setBorder(UIUtils.createPaddedTitledBorder("Editable Method Descriptors"));

        methodList = new JList<>(methodListModel);
        methodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(createMethodListPanel());

        requestJsonArea = new JTextArea();
        requestSignatureArea = new JTextArea();
        add(createSignaturePanel("Request Descriptor", requestJsonArea, requestSignatureArea, true));

        responseJsonArea = new JTextArea();
        responseSignatureArea = new JTextArea();
        add(createSignaturePanel("Response Descriptor", responseJsonArea, responseSignatureArea, false));

        methodList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleMethodSelection();
            }
        });

        UIModel.getInstance().setOnMethodsChanged(this::refreshMethodList);
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refreshMethodList();
            }
        });
        refreshMethodList();
    }

    private void refreshMethodList() {
        String selectedMethod = methodList.getSelectedValue();
        allMethodPaths.clear();
        allMethodPaths.addAll(methodMap.getMethodPaths());
        filterMethodList(selectedMethod);
    }

    private JPanel createMethodListPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        methodSearchField = new JTextField();
        methodSearchField.putClientProperty("JTextField.placeholderText", "Search methods");
        methodSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterMethodList(methodList.getSelectedValue());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterMethodList(methodList.getSelectedValue());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterMethodList(methodList.getSelectedValue());
            }
        });

        panel.add(methodSearchField, BorderLayout.PAGE_START);
        panel.add(new JScrollPane(methodList), BorderLayout.CENTER);
        return panel; 
    }

    private void filterMethodList(String selectedMethod) {
        String query = methodSearchField.getText().trim().toLowerCase(Locale.ROOT);
        methodListModel.clear();
        for (String methodPath : allMethodPaths) {
            if (query.isEmpty() || methodPath.toLowerCase(Locale.ROOT).contains(query)) {
                methodListModel.addElement(methodPath);
            }
        }

        if (selectedMethod != null && methodListModel.contains(selectedMethod)) {
            methodList.setSelectedValue(selectedMethod, true);
        } else {
            requestJsonArea.setText("");
            requestSignatureArea.setText("");
            responseJsonArea.setText("");
            responseSignatureArea.setText("");
        }
    }

    private JPanel createSignaturePanel(String title, JTextArea jsonArea, JTextArea signatureArea, boolean isRequest) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));

        java.awt.Font monoFont = new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12);

        jsonArea.setEditable(false);
        jsonArea.setLineWrap(false);
        jsonArea.setFont(monoFont);
        JPanel simplePanel = new JPanel(new BorderLayout());
        simplePanel.add(new JScrollPane(jsonArea), BorderLayout.CENTER);
        if (isRequest) {
            jsonArea.setComponentPopupMenu(getSendToRepeaterPopupUI());
        }

        signatureArea.setLineWrap(false);
        signatureArea.setFont(monoFont);
        JPanel advancedPanel = new JPanel(new BorderLayout());
        advancedPanel.add(new JScrollPane(signatureArea), BorderLayout.CENTER);

        JButton saveButton = new JButton("Save " + (isRequest ? "request" : "response") + " descriptor");
        saveButton.addActionListener(e -> saveSignature(isRequest));
        advancedPanel.add(saveButton, BorderLayout.PAGE_END);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Simple", simplePanel);
        tabs.addTab("Advanced", advancedPanel);
        panel.add(tabs, BorderLayout.CENTER);

        return panel;
    }

    private void handleMethodSelection() {
        String methodPath = methodList.getSelectedValue();
        if (methodPath == null || methodPath.isEmpty()) {
            return;
        }

        loadJson(methodPath, requestJsonArea, true);
        loadSignature(methodPath, requestSignatureArea, true);
        loadJson(methodPath, responseJsonArea, false);
        loadSignature(methodPath, responseSignatureArea, false);
    }

    private void loadJson(String methodPath, JTextArea jsonArea, boolean isRequest) {
        try {
            jsonArea.setText(Protospector.getDefaultJson(methodPath, isRequest));
            jsonArea.setCaretPosition(0);
        } catch (MethodNotFoundException | DescriptorValidationException | InvalidProtocolBufferException e) {
            jsonArea.setText("");
        }
    }

    private void loadSignature(String methodPath, JTextArea signatureArea, boolean isRequest) {
        try {
            signatureArea.setText(methodMap.getTypeDescriptorSignature(methodPath, isRequest));
            signatureArea.setCaretPosition(0);
        } catch (MethodNotFoundException e) {
            signatureArea.setText("");
        }
    }

    private void saveSignature(boolean isRequest) {
        String methodPath = methodList.getSelectedValue();
        if (methodPath == null || methodPath.isEmpty()) {
            return;
        }

        JTextArea signatureArea = isRequest ? requestSignatureArea : responseSignatureArea;
        try {
            methodMap.setTypeDescriptorSignature(methodPath, signatureArea.getText(), isRequest);
            loadJson(methodPath, isRequest ? requestJsonArea : responseJsonArea, isRequest);
        } catch (MethodNotFoundException | ParseException | DescriptorValidationException e) {
            UIUtils.errorAlert("Failed to save descriptor signature: " + e.getMessage());
        }
    }

    private JPopupMenu getSendToRepeaterPopupUI() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Send to Repeater");
        MontoyaApi api = UIModel.getInstance().getApi();
        
        menuItem.addActionListener(e -> {
            String methodPath = methodList.getSelectedValue();
            if (api != null && methodPath != null) {
                URI parsedURL;
                HttpService service = null;
                HttpRequest req = null;
                String host = null;
                String path = null;
                try {
                    parsedURL = new URI(UIModel.getInstance().getProtospectorSettings().getRepeaterBaseURL());

                    parsedURL = parsedURL.resolve(methodPath);
                    host = parsedURL.getAuthority();
                    path = parsedURL.getPath();
                    service = HttpService.httpService(parsedURL.toString());
                    req = HttpRequest.httpRequest();
                } catch (Exception e1) {
                    UIUtils.errorAlert("Failed to parse repeater base URL");
                    return;
                }
                
                String contentType = UIModel.getInstance().getProtospectorSettings().getContentType();
                ByteArray serializedMessage;
                try {
                    String jsonRequest = Protospector.getDefaultJson(methodPath, true);
                    serializedMessage = Protospector.serializeMessage(methodPath, ByteArray.byteArray(jsonRequest), contentType, true);
                } catch (InvalidProtocolBufferException | MethodNotFoundException | DescriptorValidationException | SerializerException e1) {
                    UIUtils.errorAlert("Failed to get JSON: " + e1.getMessage());
                    return;
                };

                try {
                    api.repeater().sendToRepeater(
                            req.withService(service).withMethod("POST").withPath(path).withAddedHeader("Host", host)
                                    .withAddedHeader("Content-Type", contentType).withBody(serializedMessage));
                } catch (Exception e1) {
                    UIUtils.errorAlert("Failed to send to repeater: " + e1.getMessage());
                    return;
                }
            }
        });

        popup.add(menuItem);
        return popup;
    }
}
