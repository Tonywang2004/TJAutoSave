package com.example.demo;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.branch.GitBrancher;
import git4idea.commands.*;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import javax.swing.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
public class CreateNewBranchAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
    }
    public static void showCreateBranchDialog(Project project) {
        // 获取当前项目中的 Git 仓库
        List<GitRepository> repositories = getRepositories(project);
        // 获取当前分支
        String previousBranch="";
        for (GitRepository repository : repositories) {
            previousBranch= repository.getCurrentBranchName(); // 获取当前分支名作为模拟
        }
        if (repositories.isEmpty()) {
            // 如果没有找到 Git 仓库，弹出提示
            initializeGitRepository(project);
            System.out.println("已经创建本地git仓库");
            return;
        }

        // 弹出新建分支的 UI 界面
        GitBrancher brancher = GitBrancher.getInstance(project);

        // 检查分支名输入框
        LocalDateTime now = LocalDateTime.now();

        // 定义格式：年月日时分秒
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        // 格式化当前时间为字符串
        String branchName = now.format(formatter);

        if (!branchName.trim().isEmpty()) {
            // 创建并切换到新分支
            brancher.checkoutNewBranch(branchName, repositories);
            // 暂存文件
            stageFiles(project, repositories);
            // 进行提交
            commitChanges(project, repositories, "AutoCommit on branch: " + branchName);
            //返回弹窗
            int result = JOptionPane.showOptionDialog(
                    null,
                    "是否切换到原先的分支?",
                    "切换分支",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]{"是", "否"},
                    "Switch to Previous Branch"
            );
            if (result == JOptionPane.YES_OPTION) {
                // 切换到之前的分支
                switchToPreviousBranch(project, repositories,previousBranch);
            } else if (result == JOptionPane.NO_OPTION) {
                // 保持在当前分支，不执行任何操作
                Messages.showInfoMessage("保持在当前分支", "提醒");
            }
        }
    }

    private static List<GitRepository> getRepositories(Project project) {
        GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
        return repositoryManager.getRepositories();
    }

    private static void stageFiles(Project project, List<GitRepository> repositories) {
        for (GitRepository repository : repositories) {
            // 构建 git add . 命令
            GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.ADD);
            handler.setStdoutSuppressed(false);
            handler.addParameters(".");

            GitCommandResult result = Git.getInstance().runCommand(handler);

            if (!result.success()) {
                Messages.showErrorDialog(project, "Error staging files: " + result.getErrorOutputAsJoinedString(), "Error");
            }

        }
    }

    private static void commitChanges(Project project, List<GitRepository> repositories, String commitMessage) {
        // 对每个仓库进行提交
        for (GitRepository repository : repositories) {
            // 提交更改
            GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.COMMIT);
            handler.setStdoutSuppressed(false);
            handler.addParameters("-m",commitMessage);
            GitCommandResult result = Git.getInstance().runCommand(handler);

            if (result.success()) {
                Messages.showInfoMessage(project, "提交成功： " + commitMessage , "Success");
            } else {
                Messages.showErrorDialog(project, "提交失败： " + result.getErrorOutputAsJoinedString(), "Error");
            }
        }
    }

    // 切换到之前的分支
    private static void switchToPreviousBranch(Project project, List<GitRepository> repositories,String lastbranch) {
        for (GitRepository repository : repositories) {
            String previousBranch = repository.getCurrentBranchName(); // 获取当前分支名作为模拟
            // 这里根据逻辑选择之前的分支（可以通过缓存、手动输入或其他方法来确定之前的分支）

            GitBrancher brancher = GitBrancher.getInstance(project);
            brancher.checkout(lastbranch, false, repositories, null);
            Messages.showInfoMessage("切换到: " + lastbranch, "切换");
        }
    }

    // 新建本地仓库
    public static void initializeGitRepository(Project project) {
        // 获取项目目录
        VirtualFile projectRoot = project.getBaseDir();
        if (projectRoot == null) {
            Messages.showErrorDialog("项目目录未找到.", "错误");
            return;
        }

        // 初始化 Git 仓库
        GitLineHandler handler = new GitLineHandler(project, projectRoot, GitCommand.INIT);
        GitCommandResult result = Git.getInstance().runCommand(handler);

        if (result.success()) {
            Messages.showInfoMessage("Git仓库成功建立，请稍等", "成功");
        } else {
            Messages.showErrorDialog("Git仓库创建失败: " + result.getErrorOutputAsJoinedString(), "错误");
        }

        // 刷新文件系统以显示 .git 文件夹
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(projectRoot.getPath()));
    }
}
