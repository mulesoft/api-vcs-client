package org.mule.api.vcs.client.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import org.mule.api.vcs.client.BranchInfo;
import org.mule.api.vcs.client.service.BranchRepositoryManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ModifiedFileDiff implements Diff {

    private Patch<String> diff;
    private String relativePath;
    private List<String> originalLines;
    private File original;

    public ModifiedFileDiff(Patch<String> diff, String relativePath, List<String> originalLines, File original) {
        this.diff = diff;
        this.relativePath = relativePath;
        this.originalLines = originalLines;
        this.original = original;
    }

    public File getOriginal() {
        return original;
    }

    public List<String> getOriginalLines() {
        return originalLines;
    }

    @Override
    public ApplyResult apply(File targetDirectory, MergingStrategy mergingStrategy) {
        try {
            final Path theFilePath = new File(targetDirectory, relativePath).toPath();
            final List<String> source = Files.readAllLines(theFilePath, BranchInfo.DEFAULT_CHARSET);
            final List<String> patch;
            try {
                patch = DiffUtils.patch(source, diff);
                Files.write(theFilePath, patch, BranchInfo.DEFAULT_CHARSET);
            } catch (PatchFailedException e) {
                switch (mergingStrategy) {
                    case KEEP_THEIRS:
                        try {
                            final List<String> ours = DiffUtils.patch(originalLines, diff);
                            Files.write(theFilePath, ours, BranchInfo.DEFAULT_CHARSET);
                        } catch (PatchFailedException ex) {
                            //This should not happen
                            return ApplyResult.fail("FATAL ERROR while trying to apply patch." + ex.getMessage());
                        }
                        break;
                    case KEEP_BOTH:
                        try {
                            //Keep the three files so that we can do a three-way-diff
                            final List<String> theirsContent = DiffUtils.patch(originalLines, diff);
                            final Path theirsPath = new File(targetDirectory, relativePath + Diff.THEIRS_FILE_EXTENSION).toPath();
                            final Path oursPath = new File(targetDirectory, relativePath + Diff.OURS_FILE_EXTENSION).toPath();
                            Files.write(theirsPath, theirsContent, BranchInfo.DEFAULT_CHARSET);
                            Files.copy(theFilePath, oursPath);
                            Files.write(theFilePath, originalLines, BranchInfo.DEFAULT_CHARSET);
                        } catch (PatchFailedException ex) {
                            //This should not happen
                            return ApplyResult.fail("FATAL ERROR while trying to apply patch." + ex.getMessage());
                        }
                        break;
                }
                //We should some how patch it and
                return ApplyResult.fail(e.getMessage() + " resolution strategy `" + mergingStrategy + "`");
            }
            return ApplyResult.SUCCESSFUL;
        } catch (IOException e) {
            return failError(e);
        }
    }

    @Override
    public void print(PrintWriter printWriter) {
        printWriter.println("Index: " + relativePath);
        printWriter.println("===================================================================");
        final List<String> stringList = UnifiedDiffUtils.generateUnifiedDiff(relativePath, relativePath, originalLines, diff, 2);
        for (String line : stringList) {
            printWriter.println(line);
        }
    }

    @Override
    public void push(BranchRepositoryManager branch, File targetDirectory) {
        try {
            final Path fileToPush = new File(targetDirectory, relativePath).getCanonicalFile().toPath();
            branch.updateFile(relativePath, Files.readAllBytes(fileToPush));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getRelativePath() {
        return relativePath;
    }

    @Override
    public MergeOperation getOperationType() {
        return MergeOperation.MODIFIED;
    }

    @Override
    public ApplyResult unApply(File targetDirectory) {
        final Path theFilePath = new File(targetDirectory, relativePath).toPath();
        try {
            Files.write(theFilePath, originalLines, BranchInfo.DEFAULT_CHARSET);
        } catch (IOException e) {
            return failError(e);
        }
        return ApplyResult.SUCCESSFUL;
    }

    private ApplyResult failError(IOException e) {
        return ApplyResult.fail("[FATAL] Error while trying to write `" + relativePath + "`. Reason :" + e.getMessage());
    }
}
