package org.mule.api.vcs.client;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.Patch;
import org.mule.api.vcs.client.diff.*;
import org.mule.api.vcs.client.service.*;
import org.mule.maven.exchange.model.ExchangeModel;
import org.mule.maven.exchange.model.ExchangeModelSerializer;

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

import static org.mule.api.vcs.client.service.OrgIdUserInfoProviderDecorator.withOrgId;

public class ApiVCSClient {

    public static final String PROJECT_ID_KEY = "projectId";
    public static final String BRANCH_KEY = "branch";
    public static final String APIVCS_FOLDER_NAME = ".apivcs";
    public static final String ORG_ID_KEY = "orgId";
    private File targetDirectory;
    private RepositoryFileManager fileManager;


    public ApiVCSClient(File targetDirectory, RepositoryFileManager fileManager) {
        this.targetDirectory = targetDirectory;
        this.fileManager = fileManager;
    }

    public List<String> branches(UserInfoProvider provider, String projectId) {
        final List<ApiBranch> theBranch = fileManager.branches(provider, projectId);
        return theBranch.stream().map((branch) -> branch.getName()).collect(Collectors.toList());
    }


    public ValueResult<Void> clone(UserInfoProvider provider, BranchInfo config) {
        final ValueResult<Void> valueResult = storeConfig(config.getProjectId(), config.getBranch(), config.getOrgId());
        if (valueResult.isFailure()) {
            return valueResult.asFailure();
        } else {
            final File branchDirectory = getBranchDirectory(config.getBranch());
            if (branchDirectory.mkdirs()) {
                final BranchRepositoryLock apiLock = fileManager.acquireLock(withOrgId(provider, config.getOrgId()), config.getProjectId(), config.getBranch());
                if (apiLock.isSuccess()) {
                    return cloneBranchContentTo(apiLock, this.targetDirectory, branchDirectory);
                } else {
                    return repositoryAlreadyLocked(apiLock);
                }
            } else {
                return ValueResult.fail("Unable to initialize apivcs as it was already initialized. Clean .apivcs directory before clone.");
            }
        }
    }

    public ValueResult<Void> create(UserInfoProvider provider, MergeListener listener, ApiType apiType, String name, String description) {
        try {
            final File file = targetDirectory;
            if (file.exists()) {
                return ValueResult.fail("Folder " + name + " already exists");
            } else {
                file.mkdirs();
                final BranchInfo branchInfo = fileManager.create(provider, apiType, name, description);
                storeConfig(branchInfo.getProjectId(), branchInfo.getBranch(), provider.getOrgId());
                return pull(provider, MergingStrategy.KEEP_BOTH, listener);
            }
        } catch (Exception e) {
            return ValueResult.fail(e.getMessage());
        }
    }

    public ValueResult<Void> publish(UserInfoProvider provider, MergingStrategy mergingStrategy, MergeListener listener) {
        final File exchangeJsonFile = new File(targetDirectory, "exchange.json");
        if (!exchangeJsonFile.exists()) {
            return ValueResult.fail("exchange.json file is not present");
        }
        final ValueResult<BranchInfo> mayBeBranchInfo = loadConfig();
        if (mayBeBranchInfo.isFailure()) {
            return mayBeBranchInfo.asFailure();
        } else {
            BranchInfo branchInfo = mayBeBranchInfo.getValue().get();
            final String branchName = branchInfo.getBranch();
            try {
                final BranchRepositoryLock acquireLock = fileManager.acquireLock(withOrgId(provider, branchInfo.getOrgId()), branchInfo.getProjectId(), branchName);
                if (acquireLock.isSuccess()) {
                    final ValueResult<Void> voidValueResult = pull(acquireLock, branchInfo, mergingStrategy, listener);
                    if (voidValueResult.isSuccess()) {
                        //apply patches
                        try {
                            final ExchangeModel exchangeModel = new ExchangeModelSerializer().read(exchangeJsonFile);
                            final PublishInfo publishInfo = new PublishInfo(
                                    exchangeModel.getName(),
                                    exchangeModel.getApiVersion(),
                                    exchangeModel.getVersion(),
                                    exchangeModel.getTags(),
                                    exchangeModel.getMain(),
                                    exchangeModel.getAssetId(),
                                    exchangeModel.getGroupId(),
                                    exchangeModel.getClassifier(),
                                    branchInfo);
                            fileManager.publish(withOrgId(provider, branchInfo.getOrgId()), publishInfo);
                            return ValueResult.SUCCESS;
                        } catch (IOException e) {
                            return ValueResult.fail(" Unable to parse `exchange.json` : " + e.getMessage());
                        }


                    } else {
                        return voidValueResult;
                    }

                } else {
                    return repositoryAlreadyLocked(acquireLock);
                }
            } finally {
                fileManager.releaseLock(withOrgId(provider, branchInfo.getOrgId()), branchInfo.getProjectId(), branchName);
            }
        }
    }

    public ValueResult<Void> push(UserInfoProvider provider, MergingStrategy mergingStrategy, MergeListener listener) {
        final ValueResult<BranchInfo> mayBeBranchInfo = loadConfig();
        if (mayBeBranchInfo.isFailure()) {
            return mayBeBranchInfo.asFailure();
        } else {
            BranchInfo branchInfo = mayBeBranchInfo.getValue().get();
            final String branchName = branchInfo.getBranch();
            try {
                final BranchRepositoryLock acquireLock = fileManager.acquireLock(withOrgId(provider, branchInfo.getOrgId()), branchInfo.getProjectId(), branchName);
                if (acquireLock.isSuccess()) {
                    //Calculate patch
                    final List<Diff> diffs = calculateDiff(branchInfo);
                    //pull
                    if (!diffs.isEmpty()) {
                        //pull
                        if (containsConflict(diffs)) {
                            return ValueResult.fail("Resolve conflicts before pushing.");
                        } else {
                            final ValueResult<Void> voidValueResult = pull(acquireLock, branchInfo, mergingStrategy, listener);
                            if (voidValueResult.isSuccess()) {
                                //apply patches
                                final List<Diff> newDiffs = calculateDiff(branchInfo);
                                listener.startPushing(newDiffs);
                                for (Diff newDiff : newDiffs) {
                                    newDiff.push(acquireLock.getBranchRepositoryManager(), targetDirectory);
                                    listener.pushing(newDiff);
                                }
                                applyDiffsOn(diffs, mergingStrategy, new DefaultMergeListener(), getBranchDirectory(branchName));
                                return ValueResult.SUCCESS;
                            } else {
                                return voidValueResult;
                            }
                        }
                    } else {
                        //Nothing to push
                        return ValueResult.SUCCESS;
                    }
                } else {
                    return repositoryAlreadyLocked(acquireLock);
                }
            } finally {
                fileManager.releaseLock(withOrgId(provider, branchInfo.getOrgId()), branchInfo.getProjectId(), branchName);
            }
        }
    }

    private boolean containsConflict(List<Diff> diffs) {
        return diffs.stream() //
                .anyMatch((diff) -> diff instanceof NewFileConflictDiff || diff instanceof MergeConflictDiff);
    }


    private ValueResult<Void> repositoryAlreadyLocked(BranchRepositoryLock acquireLock) {
        return ValueResult.fail("Repository is locked by " + acquireLock.getOwner());
    }

    public synchronized ValueResult<Void> pull(UserInfoProvider provider, MergingStrategy mergingStrategy, MergeListener listener) {
        final ValueResult<BranchInfo> mayBeBranchInfo = loadConfig();
        if (mayBeBranchInfo.isFailure()) {
            return mayBeBranchInfo.asFailure();
        } else {
            final BranchInfo config = mayBeBranchInfo.getValue().get();
            final BranchRepositoryLock apiLock = fileManager.acquireLock(withOrgId(provider, config.getOrgId()), config.getProjectId(), config.getBranch());
            if (apiLock.isSuccess()) {
                try {
                    return pull(apiLock, config, mergingStrategy, listener);
                } finally {
                    fileManager.releaseLock(withOrgId(provider, config.getOrgId()), config.getProjectId(), config.getBranch());
                }
            } else {
                return repositoryAlreadyLocked(apiLock);
            }
        }
    }


    private ValueResult<Void> pull(BranchRepositoryLock apiLock, BranchInfo config, MergingStrategy mergingStrategy, MergeListener listener) {
        if (containsConflict(diff().getValue().orElse(new ArrayList<>()))) {
            return ValueResult.fail("Resolve conflicts before trying to pull.");
        }

        final File staging = getStagingDirectory();
        try {
            final ValueResult<Void> voidValueResult = cloneBranchContentTo(apiLock, staging);
            return voidValueResult.flatMap((success) -> {
                final File branchDirectory = getBranchDirectory(config.getBranch());
                final List<Diff> diffs = calculateDiff(staging, branchDirectory);
                final List<ApplyResult> applyResults = applyDiffsOn(diffs, mergingStrategy, listener, targetDirectory);
                applyDiffsOn(diffs, mergingStrategy, new DefaultMergeListener(), branchDirectory);
                final boolean failure = applyResults.stream().anyMatch((a) -> !a.isSuccess());
                if (failure) {
                    final String errorMessage = applyResults.stream().filter(a -> !a.isSuccess()).map(a -> a.getMessage().get()).reduce((l, r) -> l + "\n" + r).orElse("");
                    return ValueResult.fail(errorMessage);
                } else {
                    return ValueResult.SUCCESS;
                }
            });
        } finally {
            deleteDirectory(staging);
        }
    }

    private File getStagingDirectory() {
        final File stagingDirectory = new File(getApiVCSDirectory(), "tmp" + File.separator + "staging");
        if (stagingDirectory.exists()) {
            //Make sure is clean
            deleteDirectory(stagingDirectory);
        }
        stagingDirectory.mkdirs();
        return stagingDirectory;
    }

    private List<ApplyResult> applyDiffsOn(List<Diff> diffs, MergingStrategy mergingStrategy, MergeListener listener, File targetDirectory) {
        listener.startApplying(diffs);
        //Apply changes to both the branch and the working directory
        final List<ApplyResult> result = diffs.stream().map((diff) -> {
            final ApplyResult apply = diff.apply(targetDirectory, mergingStrategy);
            listener.applied(diff, apply);
            return apply;
        }).collect(Collectors.toList());
        listener.endApplying(diffs, result);
        return result;
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
        return calculateDiff(targetDirectory, branchDirectory);
    }

    public List<ProjectInfo> list(UserInfoProvider provider) {
        return fileManager.projects(provider);
    }

    private void deleteDirectory(File branchDirectory) {
        deleteDirectory(branchDirectory, pathname -> true);
    }

    private ValueResult<Void> cloneBranchContentTo(BranchRepositoryLock apiLock, File... targetDirectory) {
        final List<ApiFile> apiFiles = apiLock.getBranchRepositoryManager().listFiles();

        for (ApiFile file : apiFiles) {
            try {
                //Filter exchange_modules
                if (!file.getPath().startsWith("exchange_modules/")) {
                    ApiFileContent fileGetResponse = apiLock.getBranchRepositoryManager().fileContent(file.getPath());
                    for (File directory : targetDirectory) {
                        File targetFile = new File(directory, file.getPath());
                        //Make sure container folder exists
                        if (!targetFile.getParentFile().exists())
                            targetFile.getParentFile().mkdirs();
                        try (FileOutputStream writer = new FileOutputStream(targetFile)) {
                            writer.write(fileGetResponse.getContent());
                        }
                    }

                }
            } catch (IOException e) {
                return ValueResult.fail("Problem while trying to write file " + file.getPath() + ".");
            }
        }
        return ValueResult.SUCCESS;
    }

    protected File getBranchDirectory(String branch) {
        final File branches = new File(getApiVCSDirectory(), "branches");
        return new File(branches, branch);
    }

    protected File getApiVCSDirectory() {
        return new File(targetDirectory, APIVCS_FOLDER_NAME);
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
                    if (!filter.accept(dir.toFile())) {
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


    private List<Diff> calculateDiff(File modified, File original) {
        return calculateDiff(modified, original, ".");
    }

    private List<Diff> calculateDiff(File revised, File original, String relativePath) {
        final ArrayList<Diff> diffs = new ArrayList<>();
        if (isIgnore(revised)) {
            return diffs;
        }
        try {
            if (revised.isDirectory() && original.isDirectory()) {
                final File[] sourceChildren = revised.listFiles();
                if (sourceChildren != null) {
                    for (File sourceChild : sourceChildren) {
                        diffs.addAll(calculateDiff(sourceChild, new File(original, sourceChild.getName()), relativePath + File.separator + sourceChild.getName()));
                    }
                }
                final File[] targetChildren = original.listFiles();
                if (targetChildren != null) {
                    for (File targetChild : targetChildren) {
                        final File sourceChild = new File(revised, targetChild.getName());
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
                final Path sourcePath = revised.toPath();
                final Path targetPath = original.toPath();
                if (revised.isDirectory()) {
                    if (original.isFile()) {
                        diffs.add(new DeleteFileDiff(relativePath, Files.readAllLines(original.toPath(), BranchInfo.DEFAULT_CHARSET)));
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

                } else if (original.isDirectory()) {
                    if (revised.isFile()) {
                        final byte[] content = Files.readAllBytes(sourcePath);
                        diffs.add(new NewFileDiff(content, relativePath));
                    }
                    addDeletedDiffs(targetPath, relativePath, diffs);
                } else if (revised.isFile()) {

                    final File theirsFile = new File(revised.getPath() + Diff.THEIRS_FILE_EXTENSION);
                    if (theirsFile.exists()) {
                        final List<String> theirsLines = Files.readAllLines(theirsFile.toPath(), BranchInfo.DEFAULT_CHARSET);
                        final List<String> originalLines = Files.readAllLines(revised.toPath(), BranchInfo.DEFAULT_CHARSET);
                        final File oursFile = new File(revised.getPath() + Diff.ORIGINAL_FILE_EXTENSION);
                        if (oursFile.exists()) {
                            final List<String> oursLines = Files.readAllLines(oursFile.toPath(), BranchInfo.DEFAULT_CHARSET);
                            diffs.add(new MergeConflictDiff(originalLines, theirsLines, oursLines, relativePath));
                        } else {
                            diffs.add(new NewFileConflictDiff(theirsLines, originalLines, relativePath));
                        }
                    } else {
                        if (original.isFile()) {
                            final List<String> originalLines = Files.readAllLines(original.toPath(), BranchInfo.DEFAULT_CHARSET);
                            final List<String> revisedLines = Files.readAllLines(revised.toPath(), BranchInfo.DEFAULT_CHARSET);
                            try {
                                final Patch<String> diff = DiffUtils.diff(originalLines, revisedLines);
                                if (!diff.getDeltas().isEmpty()) {
                                    diffs.add(new ModifiedFileDiff(diff, relativePath, originalLines, original));
                                }
                            } catch (DiffException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                        } else if (!original.exists()) {
                            diffs.add(new NewFileDiff(Files.readAllBytes(revised.toPath()), relativePath));
                        }
                    }
                } else {
                    if (original.isFile() && !revised.exists()) {
                        diffs.add(new DeleteFileDiff(relativePath, Files.readAllLines(original.toPath(), BranchInfo.DEFAULT_CHARSET)));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return diffs;
    }

    private boolean isIgnore(File source) {
        return source.isHidden() || source.getName().endsWith(Diff.THEIRS_FILE_EXTENSION) || source.getName().endsWith(Diff.ORIGINAL_FILE_EXTENSION);
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

    private ValueResult<Void> storeConfig(String projectId, String branch, String groupId) {
        final File apiVCSDirectory = getApiVCSDirectory();
        apiVCSDirectory.mkdirs();
        final File config = getConfigFile(apiVCSDirectory);
        final Properties properties = new Properties();
        properties.put(PROJECT_ID_KEY, projectId);
        properties.put(BRANCH_KEY, branch);
        properties.put(ORG_ID_KEY, groupId);
        try (FileOutputStream fileOutputStream = new FileOutputStream(config)) {
            properties.store(fileOutputStream, "");
        } catch (IOException e) {
            return ValueResult.fail("Unable to store settings at : " + config.getAbsolutePath() + ". Verify the user has the right access.");
        }
        return ValueResult.SUCCESS;
    }

    public static File getConfigFile(File apiVCSDirectory) {
        return new File(apiVCSDirectory, "config.properties");
    }

    protected ValueResult<BranchInfo> loadConfig() {
        final File apiVCSDirectory = getApiVCSDirectory();
        final Properties properties = new Properties();
        if (!apiVCSDirectory.exists()) {
            return ValueResult.fail("Not an apivcs directory.");
        } else {
            try (final FileInputStream inStream = new FileInputStream(getConfigFile(apiVCSDirectory))) {
                properties.load(inStream);
            } catch (IOException e) {
                //
                return ValueResult.fail(e.getMessage());
            }
            return ValueResult.success(new BranchInfo(properties.getProperty(PROJECT_ID_KEY), properties.getProperty(BRANCH_KEY), properties.getProperty(ORG_ID_KEY)));
        }
    }

    public ValueResult<String> currentBranch() {
        return loadConfig().map(b -> b.getBranch());
    }

    public ValueResult<Void> markResolved(String relativePath) {
        try {
            final File file = new File(targetDirectory, relativePath).getCanonicalFile();
            final File oursFile = new File(file.getAbsolutePath() + Diff.ORIGINAL_FILE_EXTENSION);
            if (oursFile.exists()) {
                oursFile.delete();
            }
            final File theirsFile = new File(file.getAbsolutePath() + Diff.THEIRS_FILE_EXTENSION);
            if (theirsFile.exists()) {
                theirsFile.delete();
            }
            return ValueResult.SUCCESS;
        } catch (IOException io) {
            return ValueResult.fail(io.getMessage());
        }
    }

    public ValueResult<Void> revert(String relativePath) {
        try {
            final File file = new File(targetDirectory, relativePath).getCanonicalFile();
            final ValueResult<List<Diff>> mayBe = diff();
            return mayBe.map((diff) -> {
                for (Diff diff1 : diff) {
                    final File aFileWithDiff;
                    try {
                        aFileWithDiff = new File(targetDirectory, diff1.getRelativePath()).getCanonicalFile();
                        if (file.equals(aFileWithDiff)) {
                            diff1.unApply(targetDirectory);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            });
        } catch (IOException io) {
            return ValueResult.fail(io.getMessage());
        }
    }

    public ValueResult<Void> revertAll() {
        final ValueResult<List<Diff>> mayBe = diff();
        return mayBe.map((diff) -> {
            for (Diff diff1 : diff) {
                diff1.unApply(targetDirectory);
            }
            return null;
        });
    }
}
