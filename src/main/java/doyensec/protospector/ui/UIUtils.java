package doyensec.protospector.ui;

import java.awt.Color;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.border.Border;

public class UIUtils {
    static public void errorAlert(String message) {
        UIModel.getInstance().getApi().logging().logToError(message);
        JOptionPane.showMessageDialog(suiteFrame(), message, "Protospector Error", JOptionPane.ERROR_MESSAGE);
    }

    static public Frame suiteFrame() {
        return UIModel.getInstance().getApi().userInterface().swingUtils().suiteFrame();
    }

    static public Border createPaddedTitledBorder(String title) {
        Border paddingBorder = BorderFactory.createEmptyBorder(10, 5, 5, 5);
        Border titledBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),
                title);
        return BorderFactory.createCompoundBorder(paddingBorder, titledBorder);
    }
}
