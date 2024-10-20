package com.example.VersionControlPlugin.ui;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.example.VersionControlPlugin.VersionManager;
import com.example.VersionControlPlugin.Config;
import com.example.VersionControlPlugin.objects.Changes;
import com.example.VersionControlPlugin.objects.FileStatus;
import com.example.VersionControlPlugin.objects.FileNode;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.project.Project;
import com.intellij.ui.Gray;
import com.intellij.util.ui.JBUI;

import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;


/*** Main window of the plugin ***/
public class VersionControlUI extends JFrame {

    // Main window
    private JPanel mainPanel;
    private JSplitPane mainSplitPane;

    // Control window
    private JPanel controlPanel;
    private JLabel controlLabel;
    private JSplitPane controlSplitPane;
    // ---- Version Window
    private JList<VersionInfo> versionList;
    private DefaultListModel<VersionInfo> versionListModel;
    private JScrollPane versionScrollPane;
    // ---- FileInfo window
    private JList<FileNode> fileNodeList;
    private DefaultListModel<FileNode> fileNodeListModel;
    private JScrollPane fileInfoScrollPane;

    // Content window
    private JPanel contentPanel;
    private JSplitPane contentSplitPane;
    // ---- Compare window
    private JPanel oldPanel;
    private JPanel newPanel;
    private JLabel oldContentLabel;
    private JLabel newContentLabel;
    private JScrollPane oldContentScrollPane;
    private JScrollPane newContentScrollPane;
    private RSyntaxTextArea oldContentArea;
    private RSyntaxTextArea newContentArea;
    private LineNumberList oldContentLineNumberList;
    private LineNumberList newContentLineNumberList;

    public VersionControlUI(Project project) {
        // mainPanel settings
        setTitle("TJAutoSave");
        setSize(Config.FrameWidth, Config.FrameHeight);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null); // In the middle
        setContentPane(mainPanel);

        // contentPanel settings
        // ---- Initialize old RSyntaxTextArea
        oldContentArea = new RSyntaxTextArea();
        oldContentArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        oldContentArea.setCodeFoldingEnabled(true);
        oldContentArea.setBackground(Gray._50);
        oldContentArea.setForeground(Gray._255);
        oldContentArea.setEditable(false);
        // ---- Initialize new RSyntaxTextArea
        newContentArea = new RSyntaxTextArea();
        newContentArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        newContentArea.setCodeFoldingEnabled(true);
        newContentArea.setBackground(Gray._50);
        newContentArea.setForeground(Gray._255);
        newContentArea.setEditable(false);
        // ---- Add RSyntaxTextArea to window
        oldContentScrollPane.setViewportView(oldContentArea);
        newContentScrollPane.setViewportView(newContentArea);
        // ---- Add LineNumberList
        oldContentLineNumberList = new LineNumberList(oldContentArea);
        newContentLineNumberList = new LineNumberList(newContentArea);
        oldContentScrollPane.setRowHeaderView(oldContentLineNumberList);
        newContentScrollPane.setRowHeaderView(newContentLineNumberList);

        // ControlPanel settings
        // ---- Initialize list & list model
        versionListModel = new DefaultListModel<>();
        fileNodeListModel = new DefaultListModel<>();
        versionList.setModel(versionListModel);
        fileNodeList.setModel(fileNodeListModel);
        versionList.setCellRenderer(new VersionRenderer());
        fileNodeList.setCellRenderer(new FileInfoRenderer());


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
                    fileNodeListModel.clear();
                    for (Map.Entry<Path, FileStatus> entry : VersionManager.getInstance().getChangeDirOfDesVersion(selectedVersion.versionName).entrySet()) {
                        fileNodeListModel.addElement(new FileNode(entry, index));
                    }
                }
            }
        });
        fileNodeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                FileNode selectedFile = fileNodeList.getSelectedValue();
                if (selectedFile != null) {
                    Path filePath = selectedFile.entry.getKey();
                    Integer version = selectedFile.version;
                    String TitleName = selectedFile.toString();
                    VersionManager.FileCompare newContent = VersionManager.getInstance().getFileOfCertainVersion(filePath, version);
                    DiffManager.getInstance().showDiff(project, new SimpleDiffRequest(
                            TitleName,
                            DiffContentFactory.getInstance().create(project, String.join("\n", newContent.before)),
                            DiffContentFactory.getInstance().create(project, String.join("\n", newContent.after)),
                            "Last version",
                            "Current version"
                    ));
                }
            }
        });
    }

    /*** FileInfo list cell renderer ***/
    private static class FileInfoRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            FileNode fileInfo = (FileNode) value;
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
