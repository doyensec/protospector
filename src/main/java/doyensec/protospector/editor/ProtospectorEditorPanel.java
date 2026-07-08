package doyensec.protospector.editor;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;

class ProtospectorEditorPanel extends JPanel {

    ProtospectorEditorPanel(Component editorComponent) {
        super(new BorderLayout());
        add(editorComponent, BorderLayout.CENTER);
    }
}
