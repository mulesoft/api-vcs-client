package org.mule.api.vcs.client.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import org.mule.api.vcs.client.BranchInfo;
import org.mule.api.vcs.client.service.BranchRepositoryManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class ModifiedFileDiff implements Diff {

    private Patch<String> diff;
    private String relativePath;
    private List<String> originalLines;

    public ModifiedFileDiff(Patch<String> diff, String relativePath, List<String> originalLines) {
        this.diff = diff;
        this.relativePath = relativePath;
        this.originalLines = originalLines;
    }

    @Override
    public ApplyResult apply(File targetDirectory, MergingStrategy mergingStrategy) {
        try {
            final Path theFilePath = new File(targetDirectory, relativePath).toPath();
            final List<String> source = Files.readAllLines(theFilePath, BranchInfo.DEFAULT_CHARSET);
            final List<String> patch;
            try {
                patch = DiffUtils.patch(source, diff);
                Files.write(theFilePath, patch, BranchInfo.DEFAULT_CHARSET, StandardOpenOption.WRITE);
            } catch (PatchFailedException e) {
                switch (mergingStrategy) {
                    case KEEP_OURS:
                        try {
                            final List<String> ours = DiffUtils.patch(originalLines, diff);
                            Files.write(theFilePath, ours, BranchInfo.DEFAULT_CHARSET, StandardOpenOption.WRITE);
                        } catch (PatchFailedException ex) {
                            //This should not happen
                            return ApplyResult.fail("FATAL ERROR while trying to apply patch." + ex.getMessage());
                        }
                        break;
                    case KEEP_BOTH:
                        try {
                            final List<String> ours = DiffUtils.patch(originalLines, diff);
                            Files.write(new File(targetDirectory, relativePath + Diff.OURS_FILE_EXTENSION).toPath(), ours, BranchInfo.DEFAULT_CHARSET, StandardOpenOption.WRITE);
                        } catch (PatchFailedException ex) {
                            //This should not happen
                            return ApplyResult.fail("FATAL ERROR while trying to apply patch." + ex.getMessage());
                        }
                        break;
                }
                //We should some how patch it and
                return ApplyResult.fail("Error while trying to apply patch on `" + relativePath + "`. Reason: " + e.getMessage());
            }

            return ApplyResult.SUCCESSFUL;
        } catch (IOException e) {
            return ApplyResult.fail("Error while trying to write `" + relativePath + "`. Reason :" + e.getMessage());
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
        final Path file = new File(targetDirectory, relativePath).toPath();
        try {
            branch.updateFile(relativePath, Files.readAllBytes(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
