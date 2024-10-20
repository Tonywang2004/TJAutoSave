package com.example.VersionControlPlugin.ui;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.example.VersionControlPlugin.VersionManager;
import com.example.VersionControlPlugin.enums.changeTypeEnum;
import com.example.VersionControlPlugin.Config;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.Gray;
import com.intellij.util.ui.JBUI;

import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;


/*** Main window of the plugin ***/
public class VersionControlUI extends JFrame{

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
    private JList<FileInfo> fileInfoList;
    private DefaultListModel<FileInfo> fileInfoListModel;
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

    public VersionControlUI() {
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
        fileInfoListModel = new DefaultListModel<>();
        versionList.setModel(versionListModel);
        fileInfoList.setModel(fileInfoListModel);
        versionList.setCellRenderer(new VersionRenderer());
        fileInfoList.setCellRenderer(new FileInfoRenderer());
        versionListModel.addElement(new VersionInfo("Version 1", "2024-01-01 01:01"));
        fileInfoListModel.addElement(new FileInfo("D:\\IDEAProjects\\MyFirstJavaProgram\\src\\Main.java", "Main.java", "2024-01-01 01:01", changeTypeEnum.Changed));
        // ---- List selection event listener
        versionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                VersionInfo selectedVersion = (VersionInfo) versionList.getSelectedValue();
                if (selectedVersion != null) {
                    // Get Version Content

                    // Set File List
                    // listFilesInVersion();
                }
            }
        });
        fileInfoList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                FileInfo selectedFile = fileInfoList.getSelectedValue();
                if (selectedFile != null) {
                    // Get file content
                    String oldContent = getFileContent(selectedFile.filePath(), selectedFile.fileName(), "old");
                    String newContent = getFileContent(selectedFile.filePath(), selectedFile.fileName(), "new");
                    // Set contentPane
                    if (oldContent != null && newContent != null) {
                        oldContentArea.setText(oldContent);
                        newContentArea.setText(newContent);

                        oldPanel.revalidate();
                        oldPanel.repaint();
                        newPanel.revalidate();
                        newPanel.repaint();
                    } else {
                        System.out.println("File not found: oldContent or newContent.\n");
                    }
                }
            }
        });
    }

    /*** Initialize fileInfo list ***/
    private void listFilesInVersion(Project project) {
        Map<String, changeTypeEnum> changedFilesInfo = VersionManager.getInstance().checkUpdate(project);
        for (var changeFileInfo : changedFilesInfo.entrySet()) {
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(changeFileInfo.getKey());
            if (file != null) {
                fileInfoListModel.addElement(new FileInfo(file.getPath(), file.getName(), new java.util.Date(file.getTimeStamp()).toString(), changeFileInfo.getValue()));
            }
        }
    }

    /*** Get file content & return string ***/
    private String getFileContent(String filePath, String fileName, final String OldOrNew) {
        // Read .java files only
        if (fileName.endsWith(".java")) {
            try {
                return new String(Files.readAllBytes(Paths.get(filePath)));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return "\nThis is not a Java file.\n\n";
        }
    }

    /*** FileInfo list cell renderer ***/
    private static class FileInfoRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            FileInfo fileInfo = (FileInfo) value;
            JLabel label = new JLabel();

            // text label per cell
            label.setText("<html><font size='5'>Modify:  <b>" + fileInfo.fileName() + "</b></font><br><font size='3'>" + fileInfo.modifyTime() + "</font></html>");
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

    /*** File info in FileInfo list ***/
    private record FileInfo(String filePath, String fileName, String modifyTime, changeTypeEnum changeType) {

        @Override
        public String toString() {
            return fileName + " (" + changeType + ":" + modifyTime + ")";
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
