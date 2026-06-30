package doyensec.protospector.ui;

import javax.swing.JTabbedPane;

public class ProtospectorTab extends JTabbedPane {

    public ProtospectorTab() {
        addTab("Settings", new SettingsPage());
        addTab("Method Descriptors", new EditableMethodsPage());
    }
}
