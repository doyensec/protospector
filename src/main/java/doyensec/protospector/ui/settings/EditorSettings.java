package doyensec.protospector.ui.settings;

import java.awt.FlowLayout;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import doyensec.protospector.settings.ProtospectorProjectSettings;
import doyensec.protospector.ui.UIModel;

public class EditorSettings {

    private final ProtospectorProjectSettings protospectorSettings;

    public EditorSettings() {
        this.protospectorSettings = UIModel.getInstance().getProtospectorSettings();
    }

    public JPanel getEditorSettingsUI() {
        JPanel editorSettingsPanel = new JPanel();
        editorSettingsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JCheckBox showTypesPreviewTabCheckbox = new JCheckBox(
                "Show field list table in message editors",
                protospectorSettings.isTypesPreviewTabVisible());
        showTypesPreviewTabCheckbox.addActionListener(
                e -> protospectorSettings.setTypesPreviewTabVisible(showTypesPreviewTabCheckbox.isSelected()));
        editorSettingsPanel.add(showTypesPreviewTabCheckbox);

        return editorSettingsPanel;
    }
}
