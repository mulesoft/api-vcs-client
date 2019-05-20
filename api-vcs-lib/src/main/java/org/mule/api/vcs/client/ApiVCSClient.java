package org.mule.api.vcs.client;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.Patch;
import org.mule.api.vcs.client.diff.*;
import org.mule.api.vcs.client.service.*;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ApiVCSClient {

    public static final String PROJECT_ID_KEY = "projectId";
    public static final String BRANCH_KEY = "branch";
    public static final String APIVCS_FOLDER_NAME = ".apivcs";
    private File targetDirectory;
    private RepositoryFileManager fileManager;


    public ApiVCSClient(File targetDirectory, RepositoryFileManager fileManager) {
        this.targetDirectory = targetDirectory;
        this.fileManager = fileManager;
    }

    public List<String> branches(String projectId) {
        final List<ApiBranch> theBranch = fileManager.branches(projectId);
        return theBranch.stream().map((branch) -> branch.getName()).collect(Collectors.toList());
    }

    public ValueResult<Void> clone(BranchInfo config) {
        final ValueResult<Void> valueResult = storeConfig(config.getProjectId(), config.getBranch());
        if (valueResult.isFailure()) {
            return valueResult.asFailure();
        } else {
            final File branchDirectory = getBranchDirectory(config.getBranch());
            if (branchDirectory.mkdirs()) {
                return checkoutBranch(config);
            } else {
                return ValueResult.fail("Unable to initialize apivcs as it was already initialized. Clean .apivcs directory before clone.");
            }
        }
    }

    public ValueResult<Void> init(MergingStrategy mergingStrategy, ApiType apiType, String name, String description) {
        try {
            final BranchInfo branchInfo = fileManager.init(apiType, name, description);
            storeConfig(branchInfo.getProjectId(), branchInfo.getBranch());
            return pull(mergingStrategy);
        } catch (Exception e) {
            return ValueResult.fail(e.getMessage());
        }
    }

    public ValueResult<Void> push(MergingStrategy mergingStrategy) {
        final ValueResult<BranchInfo> mayBeBranchInfo = loadConfig();
        if (mayBeBranchInfo.isFailure()) {
            return mayBeBranchInfo.asFailure();
        } else {
            BranchInfo branchInfo = mayBeBranchInfo.getValue().get();
            final String branchName = branchInfo.getBranch();
            try {
                final ApiLock acquireLock = fileManager.acquireLock(branchInfo.getProjectId(), branchName);
                if (acquireLock.isSuccess()) {
                    //Calculate patch
                    final List<Diff> diffs = calculateDiff(branchInfo);
                    //pull
                    if (!diffs.isEmpty()) {
                        //pull
                        checkoutBranch(branchInfo);
                        //apply patches
                        List<String> messages = new ArrayList<>();
                        boolean success = applyDiffs(diffs, messages, mergingStrategy);
                        if (success) {
                            final List<Diff> newDiffs = calculateDiff(branchInfo);
                            for (Diff newDiff : newDiffs) {
                                newDiff.push(acquireLock.getBranchRepositoryManager(), targetDirectory);
                            }
                            return ValueResult.SUCCESS;
                        } else {
                            return ValueResult.fail(messages.stream().reduce((l, r) -> l + "\n" + r).orElse(""));
                        }
                    } else {
                        return ValueResult.fail("No changes where detected.");
                    }
                } else {
                    return ValueResult.fail("Repository is locked by " + acquireLock.getOwner());
                }
            } finally {
                fileManager.releaseLock(branchInfo.getProjectId(), branchName);
            }
        }
    }

    public ValueResult<Void> pull(MergingStrategy mergingStrategy) {

        final ValueResult<BranchInfo> mayBeBranchInfo = loadConfig();
        if (mayBeBranchInfo.isFailure()) {
            return mayBeBranchInfo.asFailure();
        } else {
            final BranchInfo branchInfo = mayBeBranchInfo.getValue().get();
            final List<Diff> diffs = calculateDiff(branchInfo);
            if (diffs.isEmpty()) {
                return checkoutBranch(branchInfo);
            } else {
                checkoutBranch(branchInfo);
                final List<String> messages = new ArrayList<>();
                final boolean success = applyDiffs(diffs, messages, mergingStrategy);
                if (success) {
                    return ValueResult.SUCCESS;
                } else {
                    return ValueResult.fail(messages.stream().reduce((l, r) -> l + "\n" + r).orElse(""));
                }
            }
        }
    }

    public ValueResult<List<Diff>> diff() {
        final ValueResult<BranchInfo> mayBeBranchInfo = loadConfig();
        if (mayBeBranchInfo.isFailure()) {
            return mayBeBranchInfo.asFailure();
        } else {
            BranchInfo branchInfo = mayBeBranchInfo.getValue().get();
            final List<Diff> value = calculateDiff(branchInfo);
            return ValueResult.success(value);
        }
    }

    private List<Diff> calculateDiff(BranchInfo branchInfo) {
        final String branchName = branchInfo.getBranch();
        final File branchDirectory = getBranchDirectory(branchName);
        return calculateDiff(targetDirectory, branchDirectory, "");
    }

    public List<ProjectInfo> list() {
        return fileManager.projects();
    }

    private ValueResult<Void> checkoutBranch(BranchInfo config) {
        final File branchDirectory = getBranchDirectory(config.getBranch());
        //Make sure workspace is clean
        cleanupWorkspace();
        //Clear internal branch directory
        deleteDirectory(branchDirectory, pathname -> true);
        //
        final ApiLock apiLock = fileManager.acquireLock(config.getProjectId(), config.getBranch());
        if (apiLock.isSuccess()) {
            final List<ApiFile> apiFiles = apiLock.getBranchRepositoryManager().listFiles();

            for (ApiFile file : apiFiles) {
                try {
                    //Filter exchange_modules
                    if (!file.getPath().startsWith("exchange_modules/")) {
                        ApiFileContent fileGetResponse = apiLock.getBranchRepositoryManager().fileContent(file.getPath());

                        File targetFile = new File(targetDirectory, file.getPath());
                        //Make sure container folder exists
                        if (!targetFile.getParentFile().exists())
                            targetFile.getParentFile().mkdirs();
                        try (FileOutputStream writer = new FileOutputStream(targetFile)) {
                            writer.write(fileGetResponse.getContent());
                        }
                        File branchTargetDir = new File(branchDirectory, file.getPath());
                        //Make sure container folder exists
                        if (!branchTargetDir.getParentFile().exists())
                            branchTargetDir.getParentFile().mkdirs();
                        try (FileOutputStream writer = new FileOutputStream(branchTargetDir)) {
                            writer.write(fileGetResponse.getContent());
                        }
                    }
                } catch (IOException e) {
                    return ValueResult.fail("Problem while trying to write file " + file.getPath() + ".");
                }
            }
            return ValueResult.SUCCESS;

        } else {
            return ValueResult.fail("Repository is locked by " + apiLock.getOwner());
        }
    }

    protected File getBranchDirectory(String branch) {
        final File branches = new File(getApiVCSDirectory(), "branches");
        return new File(branches, branch);
    }

    protected File getApiVCSDirectory() {
        return new File(targetDirectory, APIVCS_FOLDER_NAME);
    }

    private boolean applyDiffs(List<Diff> diffs, List<String> messages, MergingStrategy mergingStrategy) {
        boolean success = true;
        for (Diff diff : diffs) {
            final ApplyResult apply = diff.apply(targetDirectory, mergingStrategy);
            success = apply.isSuccess() && success;
            if (apply.getMessage().isPresent()) {
                messages.add(apply.getMessage().get());
            }
        }
        return success;
    }


    private void cleanupWorkspace() {
        deleteDirectory(targetDirectory, (file) -> {
            if (file.isDirectory() && file.equals(getApiVCSDirectory())) {
                return false;
            } else {
                return true;
            }
        });
    }


    private void deleteDirectory(File branchDirectory, FileFilter filter) {
        try {
            Files.walkFileTree(branchDirectory.toPath(), new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (filter.accept(dir.toFile())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    } else {
                        return FileVisitResult.CONTINUE;
                    }
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (filter.accept(file.toFile())) {
                        Files.delete(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException e) {
            //
        }
    }


    private List<Diff> calculateDiff(File source, File target, String relativePath) {
        final ArrayList<Diff> diffs = new ArrayList<>();
        if (isIgnore(source)) {
            return diffs;
        }
        try {
            if (source.isDirectory() && target.isDirectory()) {
                final File[] sourceChildren = source.listFiles();
                if (sourceChildren != null) {
                    for (File sourceChild : sourceChildren) {
                        diffs.addAll(calculateDiff(sourceChild, new File(target, sourceChild.getName()), relativePath + File.separator + sourceChild.getName()));
                    }
                }
                final File[] targetChildren = target.listFiles();
                if (targetChildren != null) {
                    for (File targetChild : targetChildren) {
                        final File sourceChild = new File(source, targetChild.getName());
                        if (!sourceChild.exists()) {
                            if (targetChild.isDirectory()) {
                                addDeletedDiffs(targetChild.toPath(), relativePath, diffs);
                            } else {
                                final String deletedFilePath = relativePath + File.separator + targetChild.getName();
                                diffs.add(new DeleteFileDiff(deletedFilePath, Files.readAllLines(targetChild.toPath(), BranchInfo.DEFAULT_CHARSET)));
                            }
                        }
                    }
                }
            } else {
                final Path sourcePath = source.toPath();
                final Path targetPath = target.toPath();
                if (source.isDirectory()) {
                    if (target.isFile()) {
                        diffs.add(new DeleteFileDiff(relativePath, Files.readAllLines(target.toPath(), BranchInfo.DEFAULT_CHARSET)));
                    }

                    Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (attrs.isRegularFile()) {
                                final String newRelativePath = relativePath + File.separator + sourcePath.relativize(file).toString();
                                final byte[] content = Files.readAllBytes(file);
                                diffs.add(new NewFileDiff(content, newRelativePath));
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });

                } else if (target.isDirectory()) {
                    if (source.isFile()) {
                        final byte[] content = Files.readAllBytes(sourcePath);
                        diffs.add(new NewFileDiff(content, relativePath));
                    }
                    addDeletedDiffs(targetPath, relativePath, diffs);
                } else if (source.isFile()) {
                    if (target.isFile()) {
                        final List<String> original = Files.readAllLines(target.toPath(), BranchInfo.DEFAULT_CHARSET);
                        final List<String> revised = Files.readAllLines(source.toPath(), BranchInfo.DEFAULT_CHARSET);
                        try {
                            final Patch<String> diff = DiffUtils.diff(original, revised);
                            if (!diff.getDeltas().isEmpty()) {
                                diffs.add(new ModifiedFileDiff(diff, relativePath, original));
                            }
                        } catch (DiffException e) {
                            throw new RuntimeException(e.getMessage());
                        }
                    } else if (!target.exists()) {
                        diffs.add(new NewFileDiff(Files.readAllBytes(source.toPath()), relativePath));
                    }
                } else {
                    if (target.isFile() && !source.exists()) {
                        diffs.add(new DeleteFileDiff(relativePath, Files.readAllLines(target.toPath(), BranchInfo.DEFAULT_CHARSET)));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return diffs;
    }

    private boolean isIgnore(File source) {
        return source.isHidden();
    }

    private void addDeletedDiffs(Path targetPath, String relativePath, ArrayList<Diff> diffs) throws IOException {
        Files.walkFileTree(targetPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    final String newRelativePath = relativePath + File.separator + targetPath.relativize(file).toString();
                    try {
                        diffs.add(new DeleteFileDiff(newRelativePath, Files.readAllLines(targetPath, BranchInfo.DEFAULT_CHARSET)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private ValueResult<Void> storeConfig(String projectId, String branch) {
        final File apiVCSDirectory = getApiVCSDirectory();
        apiVCSDirectory.mkdirs();
        final File config = getConfigFile(apiVCSDirectory);
        final Properties properties = new Properties();
        properties.put(PROJECT_ID_KEY, projectId);
        properties.put(BRANCH_KEY, branch);
        try (FileOutputStream fileOutputStream = new FileOutputStream(config)) {
            properties.store(fileOutputStream, "");
        } catch (IOException e) {
            return ValueResult.fail("Unable to store settings at : " + config.getAbsolutePath() + ". Verify the user has the right access.");
        }
        return ValueResult.SUCCESS;
    }

    private File getConfigFile(File apiVCSDirectory) {
        return new File(apiVCSDirectory, "config.properties");
    }

    protected ValueResult<BranchInfo> loadConfig() {
        final Properties properties = new Properties();
        final File apiVCSDirectory = getApiVCSDirectory();
        if (!apiVCSDirectory.exists()) {
            return ValueResult.fail("Not an apivcs directory.");
        } else {
            try (final FileInputStream inStream = new FileInputStream(getConfigFile(apiVCSDirectory))) {
                properties.load(inStream);
            } catch (IOException e) {
                //
                return ValueResult.fail(e.getMessage());
            }
            return ValueResult.success(new BranchInfo(properties.getProperty(PROJECT_ID_KEY), properties.getProperty(BRANCH_KEY)));
        }
    }

}