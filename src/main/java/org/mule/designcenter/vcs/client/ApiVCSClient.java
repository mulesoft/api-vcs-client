package org.mule.designcenter.vcs.client;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.Patch;
import org.mule.designcenter.vcs.client.diff.*;
import org.mule.designcenter.vcs.client.service.*;

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
    private File targetDirectory;
    private ApiFileManager fileManager;


    public ApiVCSClient(File targetDirectory, ApiFileManager fileManager) {
        this.targetDirectory = targetDirectory;
        this.fileManager = fileManager;
    }

    public List<String> branches(String projectId) {
        final List<ApiBranch> theBranch = fileManager.listBranches(projectId);
        return theBranch.stream().map((branch) -> branch.getName()).collect(Collectors.toList());
    }

    public SimpleResult clone(ApiVCSConfig config) {
        final File apiVCSDirectory = getApiVCSDirectory();
        final SimpleResult simpleResult = storeConfig(config.getProjectId(), config.getBranch(), apiVCSDirectory);
        if (simpleResult.isFailure()) {
            return simpleResult;
        } else {
            final File branchDirectory = getBranchDirectory(config.getBranch());
            if (branchDirectory.mkdirs()) {
                return checkoutBranch(config);
            } else {
                return SimpleResult.fail("Unable to initialize apivcs as it was already initialized. Clean .apivcs directory before clone.");
            }
        }
    }

    public SimpleResult push(MergingStrategy mergingStrategy) {
        final ApiVCSConfig apiVCSConfig = loadConfig();
        final String branchName = apiVCSConfig.getBranch();
        try {
            final ApiLock acquireLock = fileManager.acquireLock(apiVCSConfig.getProjectId(), branchName);
            if (acquireLock.isSuccess()) {
                //Calculate patch
                final List<Diff> diffs = diff();
                //pull
                if (!diffs.isEmpty()) {
                    //pull
                    checkoutBranch(apiVCSConfig);
                    //apply patches
                    List<String> messages = new ArrayList<>();
                    boolean success = applyDiffs(diffs, messages, mergingStrategy);
                    if (success) {
                        final List<Diff> newDiffs = diff();
                        for (Diff newDiff : newDiffs) {
                            newDiff.push(acquireLock.getBranchFileManager(), targetDirectory);
                        }
                        return SimpleResult.SUCCESS;
                    } else {
                        return SimpleResult.fail(messages.stream().reduce((l, r) -> l + "\n" + r).orElse(""));
                    }
                } else {
                    return SimpleResult.fail("No changes where detected.");
                }
            } else {
                return SimpleResult.fail("Repository is locked by " + acquireLock.getOwner());
            }
        } finally {
            fileManager.releaseLock(apiVCSConfig.getProjectId(), branchName);
        }
    }

    public SimpleResult pull(MergingStrategy mergingStrategy) {
        final List<Diff> diffs = diff();
        if (diffs.isEmpty()) {
            return checkoutBranch(loadConfig());
        } else {
            checkoutBranch(loadConfig());
            final List<String> messages = new ArrayList<>();
            final boolean success = applyDiffs(diffs, messages, mergingStrategy);
            if (success) {
                return SimpleResult.SUCCESS;
            } else {
                return SimpleResult.fail(messages.stream().reduce((l, r) -> l + "\n" + r).orElse(""));
            }
        }
    }

    public List<Diff> diff() {
        final ApiVCSConfig apiVCSConfig = loadConfig();
        final String branchName = apiVCSConfig.getBranch();
        final File branchDirectory = getBranchDirectory(branchName);
        return calculateDiff(targetDirectory, branchDirectory, "");
    }

    private SimpleResult checkoutBranch(ApiVCSConfig config) {
        final File branchDirectory = getBranchDirectory(config.getBranch());
        //Make sure workspace is clean
        cleanupWorkspace();
        //Clear internal branch directory
        deleteDirectory(branchDirectory, pathname -> true);
        //
        final ApiLock apiLock = fileManager.acquireLock(config.getProjectId(), config.getBranch());
        if (apiLock.isSuccess()) {
            final List<ApiFile> apiFiles = apiLock.getBranchFileManager().listFiles();

            for (ApiFile file : apiFiles) {
                try {
                    //Filter exchange_modules
                    if (!file.getPath().startsWith("exchange_modules/")) {
                        ApiFileContent fileGetResponse = apiLock.getBranchFileManager().fileContent(file.getPath());

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
                    return SimpleResult.fail("Problem while trying to write file " + file.getPath() + ".");
                }
            }
            return SimpleResult.SUCCESS;

        } else {
            return SimpleResult.fail("Repository is locked by " + apiLock.getOwner());
        }
    }

    protected File getBranchDirectory(String branch) {
        final File branches = new File(getApiVCSDirectory(), "branches");
        return new File(branches, branch);
    }

    protected File getApiVCSDirectory() {
        return new File(targetDirectory, ".apivcs");
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
                                diffs.add(new DeleteFileDiff(deletedFilePath, Files.readAllLines(targetChild.toPath(), ApiVCSConfig.DEFAULT_CHARSET)));
                            }
                        }
                    }
                }
            } else {
                final Path sourcePath = source.toPath();
                final Path targetPath = target.toPath();
                if (source.isDirectory()) {
                    if (target.isFile()) {
                        diffs.add(new DeleteFileDiff(relativePath, Files.readAllLines(target.toPath(), ApiVCSConfig.DEFAULT_CHARSET)));
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
                        final List<String> original = Files.readAllLines(target.toPath(), ApiVCSConfig.DEFAULT_CHARSET);
                        final List<String> revised = Files.readAllLines(source.toPath(), ApiVCSConfig.DEFAULT_CHARSET);
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
                        diffs.add(new DeleteFileDiff(relativePath, Files.readAllLines(target.toPath(), ApiVCSConfig.DEFAULT_CHARSET)));
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
                        diffs.add(new DeleteFileDiff(newRelativePath, Files.readAllLines(targetPath, ApiVCSConfig.DEFAULT_CHARSET)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private SimpleResult storeConfig(String projectId, String branch, File apiVCSDirectory) {
        apiVCSDirectory.mkdirs();
        final File config = getConfigFile(apiVCSDirectory);
        final Properties properties = new Properties();
        properties.put(PROJECT_ID_KEY, projectId);
        properties.put(BRANCH_KEY, branch);
        try (FileOutputStream fileOutputStream = new FileOutputStream(config)) {
            properties.store(fileOutputStream, "");
        } catch (IOException e) {
            return SimpleResult.fail("Unable to store settings at : " + config.getAbsolutePath() + ". Verify the user has the right access.");
        }
        return SimpleResult.SUCCESS;
    }

    private File getConfigFile(File apiVCSDirectory) {
        return new File(apiVCSDirectory, "config.properties");
    }

    protected ApiVCSConfig loadConfig() {
        final Properties properties = new Properties();
        try (final FileInputStream inStream = new FileInputStream(getConfigFile(getApiVCSDirectory()))) {
            properties.load(inStream);
        } catch (IOException e) {
            //
        }
        return new ApiVCSConfig(properties.getProperty(PROJECT_ID_KEY), properties.getProperty(BRANCH_KEY));
    }

}
