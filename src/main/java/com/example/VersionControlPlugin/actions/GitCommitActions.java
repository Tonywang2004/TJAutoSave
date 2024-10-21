package com.example.VersionControlPlugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFileManager;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.branch.GitBrancher;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

public class GitCommitActions extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // 获取当前项目的Git仓库
        GitRepository repository = GitRepositoryManager.getInstance(project).getRepositoryForFileQuick(project.getBaseDir());

        if (repository == null) {
            // 如果不存在Git仓库，初始化一个新的Git仓库
            Git.getInstance().init(project, project.getBaseDir());
            Messages.showMessageDialog(project, "Git repository initialized.", "Info", Messages.getInformationIcon());

            // 重新获取Git仓库
            repository = GitRepositoryManager.getInstance(project).getRepositoryForFileQuick(project.getBaseDir());

        } else {
            // 如果存在Git仓库，直接显示信息
            Messages.showMessageDialog(project, "Git repository already exists.", "Info", Messages.getInformationIcon());
        }

        // 检查并创建细粒度分支
        ensureMainBranchExists(project, repository);
    }

    // 检查主分支是否存在，如果不存在则创建
    private void ensureMainBranchExists(Project project, GitRepository repository) {
        if (repository == null) return;

        GitBrancher brancher = GitBrancher.getInstance(project);
        String mainBranch = "main";
        String newBranchName = "";

        // 检查是否存在 main 分支
        boolean mainBranchExists = repository.getBranches().getLocalBranches().stream()
                .anyMatch(branch -> branch.getName().equals(mainBranch));

        if (!mainBranchExists) {
            // 如果 main 分支不存在，创建 main 分支
            brancher.checkoutNewBranch(mainBranch, Collections.singletonList(repository));
            newBranchName = "main";
        } else {
            // 如果 main 分支已经存在，创建带日期的分支
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
            newBranchName = "branch_" + now.format(formatter);
        }

        // 创建并切换到带日期的新分支
        brancher.checkoutNewBranch(newBranchName, Collections.singletonList(repository));
        Messages.showMessageDialog(project, "New branch created: " + newBranchName, "Info", Messages.getInformationIcon());

        // 追踪细粒度修改并逐一提交
        trackAndCommitFineGrainedChanges(project, repository, newBranchName);

        // 合并提示
        consolidateAndApplyCommits(project, repository, newBranchName);

    }

    private void trackAndCommitFineGrainedChanges(Project project, GitRepository repository, String branchName) {
        Git git = Git.getInstance();
        VirtualFile root = repository.getRoot();
        try {
            //查看当前状态
            GitLineHandler statusHandler = new GitLineHandler(project, root, GitCommand.STATUS);
            git.runCommand(statusHandler);

            //添加src文件夹下所有文件的更改
            GitLineHandler addHandler = new GitLineHandler(project, root, GitCommand.ADD);
            addHandler.addParameters("./src");
            git.runCommand(addHandler);

            //提交更改
            GitLineHandler commitHandler = new GitLineHandler(project, root, GitCommand.COMMIT);
            commitHandler.addParameters("-m", "Fine-grained commit for all files");
            git.runCommand(commitHandler);

            //打印提交信息
            Messages.showMessageDialog(repository.getProject(), "Committed all fine-grained changes.", "Fine-Grained Commit", Messages.getInformationIcon());
        } catch (Exception ex) {
            // 打印详细的错误信息
            ex.printStackTrace();
            Messages.showMessageDialog(project, "Error during fine-grained commits: " + ex.getMessage(), "Error", Messages.getErrorIcon());
        }
    }

    // 合并并压缩提交到主分支
    private void consolidateAndApplyCommits(Project project, GitRepository repository, String fineGrainedBranchName) {
        Git git = Git.getInstance();
        try {
            String currentBranch = repository.getCurrentBranch() != null ? repository.getCurrentBranch().getName() : "";
            if("main".equals(currentBranch)){
                return;
            }
            // 提示用户是否合并到 main 分支
            int result = Messages.showYesNoDialog(
                    project,
                    "Do you want to merge the fine-grained branch into the main branch?",
                    "Merge to Main",
                    Messages.getQuestionIcon()
            );

            if (result == Messages.YES) {
                GitBrancher brancher = GitBrancher.getInstance(project);

                // 切换回 main 分支，并保证在切换后才进行合并操作
                brancher.checkout("main", false, Collections.singletonList(repository), new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // 使用 --squash 参数压缩提交
                            GitLineHandler mergeHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.MERGE);
                            mergeHandler.addParameters("--squash", fineGrainedBranchName);
                            git.runCommand(mergeHandler);

                            // 提交压缩的更改，只显示一次提交记录
                            GitLineHandler commitHandler = new GitLineHandler(project, repository.getRoot(), GitCommand.COMMIT);
                            commitHandler.addParameters("-m", "Squashed commit from fine-grained branch");
                            git.runCommand(commitHandler);

                            // 刷新文件状态，确保主分支显示更新后的文件
                            VcsDirtyScopeManager.getInstance(project).markEverythingDirty();
                            VirtualFileManager.getInstance().syncRefresh();

                            // 提示合并成功
                            Messages.showMessageDialog(project, "Fine-grained commits successfully squashed and merged into the main branch.", "Success", Messages.getInformationIcon());

                        } catch (Exception ex) {
                            Messages.showMessageDialog(project, "Error during consolidation: " + ex.getMessage(), "Error", Messages.getErrorIcon());
                        }
                    }
                });
            } else {
                // 用户选择不合并到主分支时，保留细粒度分支
                Messages.showMessageDialog(project, "Changes are kept in the fine-grained branch: " + fineGrainedBranchName, "No Merge", Messages.getInformationIcon());
            }

        } catch (Exception ex) {
            Messages.showMessageDialog(project, "Error during consolidation: " + ex.getMessage(), "Error", Messages.getErrorIcon());
        }
    }


}