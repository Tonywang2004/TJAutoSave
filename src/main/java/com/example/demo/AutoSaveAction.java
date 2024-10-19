package com.example.demo;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.WindowManager;
import git4idea.branch.GitBrancher;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import java.util.List;
public class AutoSaveAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        // 获取当前的项目
        Project project = e.getProject();

        // 创建并显示自定义弹出窗口
        if (project != null) {
            CreateNewBranchAction.showCreateBranchDialog(project);
        }
    }
}
