package doyensec.protospector.ui;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import doyensec.protospector.ui.settings.EditorSettings;
import doyensec.protospector.ui.settings.PSFileUtils;
import doyensec.protospector.ui.settings.RepeaterSettings;

public class SettingsPage extends JPanel {

    SettingsPage() {

        setLayout(new BorderLayout());

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(UIUtils.createPaddedTitledBorder("Settings"));
        settingsPanel.add(new PSFileUtils(this).getPBFileUtilsUI());
        settingsPanel.add(new RepeaterSettings().getRepeaterSettingsUI());
        settingsPanel.add(new EditorSettings().getEditorSettingsUI());

        add(settingsPanel, BorderLayout.PAGE_START);
    }
}
