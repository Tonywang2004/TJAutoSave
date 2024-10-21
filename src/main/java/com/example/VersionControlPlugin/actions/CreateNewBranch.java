package com.example.VersionControlPlugin.actions;

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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CreateNewBranch extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        // 获取当前的项目
        Project project = e.getProject();

        // 创建并显示自定义弹出窗口
        if (project != null) {
            showCreateBranchDialog(project);
        }
    }

    private void showCreateBranchDialog(Project project) {
        // 获取当前项目中的 Git 仓库
        List<GitRepository> repositories = getRepositories(project);
        // 获取当前分支
        String previousBranch = "";
        for (GitRepository repository : repositories) {
            previousBranch = repository.getCurrentBranchName(); // 获取当前分支名作为模拟
        }
        if (repositories.isEmpty()) {
            // 如果没有找到 Git 仓库，弹出提示
            initializeGitRepository(project);
            System.out.println("已经创建本地git仓库");
            return;
        }

        // 检查并创建主分支
        int main_branch = checkAndCreateMainBranch(project, repositories);

        // 弹出新建分支的 UI 界面
        GitBrancher brancher = GitBrancher.getInstance(project);

        // 定义分支名
        String branchName;
        if (main_branch == 0) {
            // 当 main_branch 为 0 时，分支名为日期时间格式
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            branchName = now.format(formatter);
        } else {
            // 当 main_branch 为 1 时，分支名为 "main"
            branchName = "main";
        }

        if (!branchName.trim().isEmpty()) {
            // 创建并切换到新分支
            brancher.checkoutNewBranch(branchName, repositories);
            // 暂存文件
            stageFiles(project, repositories);
            // 进行提交
            commitChanges(project, repositories, "AutoCommit on branch: " + branchName);

            // 合并提示
            int result2 = Messages.showYesNoDialog(project, "Do you want to merge the fine-grained branch into main?", "Merge Branch", Messages.getQuestionIcon());
            if (result2 == Messages.YES) {
                consolidateAndApplyCommits(project, repositories, branchName);
            }

            // 返回弹窗
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
                switchToPreviousBranch(project, repositories, previousBranch);
            } else if (result == JOptionPane.NO_OPTION) {
                // 保持在当前分支，不执行任何操作
                Messages.showInfoMessage("保持在当前分支", "提醒");
            }
        }
    }

    //暂存文件
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

    //提交操作
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

    // 合并到主分支（带冲突检查）
    private void consolidateAndApplyCommits(Project project, List<GitRepository> repositories, String newBranchName) {
        for (GitRepository repository : repositories) {
            Git git = Git.getInstance();
            try {
                GitBrancher brancher = GitBrancher.getInstance(project);

                // 切换到主分支
                brancher.checkout("main", false, Collections.singletonList(repository), null);

                // 执行合并操作
                GitLineHandler mergeHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.MERGE);
                mergeHandler.addParameters(newBranchName);

                GitCommandResult result = git.runCommand(mergeHandler);

                if (result.success()) {
                    Messages.showMessageDialog(project, "细粒度提交成功合并到主分支.", "Success", Messages.getInformationIcon());
                } else {
                    String errorOutput = result.getErrorOutputAsJoinedString();
                    if (errorOutput.contains("CONFLICT")) {
                        Messages.showErrorDialog(project, "合并失败，发生冲突: " + errorOutput, "Merge Conflict");
                    } else {
                        Messages.showErrorDialog(project, "合并失败: " + errorOutput, "Error");
                    }
                }

            } catch (Exception ex) {
                Messages.showMessageDialog(project, "合并时发生错误: " + ex.getMessage(), "Error", Messages.getErrorIcon());
            }
        }
    }

    // 切换到之前的分支（带未提交更改检查）
    private static void switchToPreviousBranch(Project project, List<GitRepository> repositories, String lastBranch) {
        for (GitRepository repository : repositories) {
            try {
                GitBrancher brancher = GitBrancher.getInstance(project);

                // 检查是否有未暂存的更改
                GitLineHandler statusHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.STATUS);
                GitCommandResult statusResult = Git.getInstance().runCommand(statusHandler);
                if (!statusResult.getOutputAsJoinedString().contains("nothing to commit")) {
                    Messages.showErrorDialog(project, "工作区有未提交的更改，请先提交。", "Error");
                    return;
                }

                // 切换到之前的分支
                brancher.checkout(lastBranch, false, repositories, null);
                Messages.showInfoMessage("切换回: " + lastBranch, "切换成功");

            } catch (Exception ex) {
                Messages.showErrorDialog(project, "切换分支时发生错误: " + ex.getMessage(), "Error");
            }
        }
    }


    //检查是否有主分支，没有则创建一个
    private static int checkAndCreateMainBranch(Project project, List<GitRepository> repositories) {
        for (GitRepository repository : repositories) {
            // 获取所有分支
            List<String> branches = repository.getBranches().getLocalBranches().stream()
                    .map(branch -> branch.getName())
                    .collect(Collectors.toList());

            // 检查是否有名为 'main' 的分支
            if (!branches.contains("main")) {
                Messages.showInfoMessage("未检测到 'main' 分支，正在创建...", "信息");

                // 获取 GitBrancher 实例
                GitBrancher brancher = GitBrancher.getInstance(project);
                // 创建 'main' 分支并切换到该分支
                brancher.checkoutNewBranch("main", repositories);

                Messages.showInfoMessage("成功创建并切换到 'main' 分支", "成功");
                return 1;
            } else {
                Messages.showInfoMessage("'main' 分支已存在", "信息");
                return 0;
            }
        }
        return 0;
    }

    //获取当前仓库
    private static List<GitRepository> getRepositories(Project project) {
        GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
        return repositoryManager.getRepositories();
    }

    //初始化新仓库
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