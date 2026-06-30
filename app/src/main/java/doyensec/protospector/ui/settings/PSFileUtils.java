package doyensec.protospector.ui.settings;

import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JFileChooser;

import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.google.protobuf.Descriptors.DescriptorValidationException;

import doyensec.protospector.protobuf.EditableMethodMap.MethodMap;
import doyensec.protospector.ui.SettingsPage;
import doyensec.protospector.ui.UIUtils;
import doyensec.protospector.ui.UIModel;

public class PSFileUtils {
    private final MethodMap editableMethodMap;

    public PSFileUtils(SettingsPage settingsPage) {
        this.editableMethodMap = UIModel.getInstance().getEditableMethodMap();
    }

    public JPanel getPBFileUtilsUI() {
        JPanel fileUtilsPanel = new JPanel();
        fileUtilsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JButton setFileBtn = new JButton("Load .pb file");

        setFileBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(UIUtils.suiteFrame()) == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                handleLoadPBFile(file);
            }
        });

        fileUtilsPanel.add(setFileBtn);

        JButton saveFileBtn = new JButton("Save .pb file");
        saveFileBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            // Set default file name
            fc.setSelectedFile(new File("descriptor.pb"));
            if (fc.showSaveDialog(UIUtils.suiteFrame()) == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                handleSavePBFile(file);
            }
        });

        fileUtilsPanel.add(saveFileBtn);

        return fileUtilsPanel;
    }

    private void handleLoadPBFile(File file) {
        int response = JOptionPane.showConfirmDialog(
                UIUtils.suiteFrame(),
                "All existing method descriptors will be replaced by the new .pb file. \n" +
                "Please note that the saving and persistence functionality will not work under certain circumstances. \n" +
                "Do you wish to continue?",
                "Confirm Load",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (response != JOptionPane.YES_OPTION) {
            return;
        }

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException, DescriptorValidationException {
                editableMethodMap.loadDescriptorFile(file);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException | ExecutionException e) {
                    UIUtils.errorAlert("Failed to load .pb file: " + e.getMessage());
                    return;
                }
            }
        }.execute();
    }

    private void handleSavePBFile(File file) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                try {
                    editableMethodMap.saveDescriptorFile(file);
                } catch (IOException e) {
                    UIUtils.errorAlert("Failed to save .pb file: " + e.getMessage());
                    return null;
                }
                return null;
            }
        }.execute();
    }
}
