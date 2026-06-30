package doyensec.protospector.ui.settings;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.FlowLayout;

import doyensec.protospector.settings.ProtospectorProjectSettings;
import doyensec.protospector.ui.UIModel;

public class RepeaterSettings {

    private JTextField repeaterTextField;
    private final ProtospectorProjectSettings protospectorSettings;

    public RepeaterSettings() {
        this.protospectorSettings = UIModel.getInstance().getProtospectorSettings();
    }

    public JPanel getRepeaterSettingsUI() {
        JPanel updateRepeaterPanel = new JPanel();
        updateRepeaterPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        updateRepeaterPanel.add(new JLabel("'Send to Repeater' base URL: "));
        repeaterTextField = new JTextField(protospectorSettings.getRepeaterBaseURL());
        repeaterTextField.putClientProperty("JTextField.placeholderText", "https://example.com/");
   
        repeaterTextField.setColumns(15);
        repeaterTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateRepeaterBaseUrl();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateRepeaterBaseUrl();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateRepeaterBaseUrl();
            }
        });

        updateRepeaterPanel.add(repeaterTextField);

        return updateRepeaterPanel;
    }

    private void updateRepeaterBaseUrl() {
        protospectorSettings.setRepeaterBaseURL(repeaterTextField.getText());
    }

}
