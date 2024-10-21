package com.example.TJAutoSave.ui;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import com.example.TJAutoSave.VersionManager;
import com.example.TJAutoSave.objects.FileStatus;
import com.example.TJAutoSave.objects.FileCompare;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;


/*** Main window of the plugin ***/
public class VersionControlUI {

    // Main window
    private JPanel mainPanel;

    private JLabel controlLabel;
    private JSplitPane controlSplitPane;
    // ---- Version Window
    private JList<VersionInfo> versionList;
    private DefaultListModel<VersionInfo> versionListModel;
    private JScrollPane versionScrollPane;
    // ---- FileInfo window
    private JList<FileInfo> fileInfoList;
    private DefaultListModel<FileInfo> fileInfoListModel;
    private JScrollPane fileInfoScrollPane;

    public VersionControlUI(Project project) {
        // Initialize list & list model
        versionListModel = new DefaultListModel<>();
        fileInfoListModel = new DefaultListModel<>();
        versionList.setModel(versionListModel);
        fileInfoList.setModel(fileInfoListModel);
        versionList.setCellRenderer(new VersionRenderer());
        fileInfoList.setCellRenderer(new FileInfoRenderer());


        // get saved versions
        try {
            var versionNodeInfoList = VersionManager.getInstance().getProjectVersionInfo();
            for (var map : versionNodeInfoList) {
                versionListModel.addElement(new VersionInfo("Version " + map.get("version"), map.get("time")));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // ---- List selection event listener
        versionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                VersionInfo selectedVersion = versionList.getSelectedValue();
                int index = versionList.getSelectedIndex() + 1;

                if (selectedVersion != null) {
                    // Get Version Content
                    fileInfoListModel.clear();
                    for (Map.Entry<Path, FileStatus> entry : VersionManager.getInstance().getChangeDirOfDesVersion(selectedVersion.versionName).entrySet()) {
                        fileInfoListModel.addElement(new FileInfo(entry, index));
                    }
                }
            }
        });
        fileInfoList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                FileInfo selectedFile = fileInfoList.getSelectedValue();
                if (selectedFile != null) {
                    Path filePath = selectedFile.entry.getKey();
                    Integer version = selectedFile.version;
                    String TitleName = selectedFile.toString();
                    FileCompare newContent = VersionManager.getInstance().getFileOfCertainVersion(filePath, version);
                    SimpleDiffRequest request = new SimpleDiffRequest(
                            TitleName,
                            DiffContentFactory.getInstance().create(project, String.join("\n", newContent.before)),
                            DiffContentFactory.getInstance().create(project, String.join("\n", newContent.after)),
                            "Last version",
                            "Current version"
                    );

                    // Show Diff
                    DiffManager.getInstance().showDiff(project, request);
                }
            }
        });
    }

    /*** Return mainPanel ***/
    public JComponent getComponent() {
        return mainPanel;
    }

    /*** FileInfo list cell renderer ***/
    private static class FileInfoRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            FileInfo fileInfo = (FileInfo) value;
            JLabel label = new JLabel();

            // text label per cell
            label.setText("<html><font size='5'>"
                    + fileInfo.entry.getValue().getStatus()
                    + ":  <b>" + fileInfo.entry.getKey().getFileName() + "</b></font></html>");
            label.setBorder(JBUI.Borders.empty(8, 12));
            label.setOpaque(true);
            label.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            return label;
        }
    }

    /*** File info in File list ***/
    private record FileInfo(Map.Entry<Path, FileStatus> entry, Integer version) {

        @Override
        public String toString() {
            return entry.getValue().getStatus()
                    + "-" + entry.getKey().getFileName().toString()
                    + "-" + entry.getValue().getTimestamp();
        }
    }

    /*** Version list cell renderer ***/
    private static class VersionRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            VersionInfo versionInfo = (VersionInfo) value;
            JLabel label = new JLabel();

            // text label per cell
            label.setText("<html><font size='5'>Version:  <b>" + versionInfo.versionName() + "</b></font><br><font size='3'>" + versionInfo.modifyTime() + "</font></html>");
            label.setBorder(JBUI.Borders.empty(10, 15));
            label.setOpaque(true);
            label.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            return label;
        }
    }

    /*** Version info in Version list ***/
    private record VersionInfo(String versionName, String modifyTime) {

        @Override
        public String toString() {
            return versionName + " (" + modifyTime + ")";
        }
    }
}
