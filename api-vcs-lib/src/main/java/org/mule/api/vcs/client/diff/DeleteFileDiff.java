package org.mule.api.vcs.client.diff;

import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.DeleteDelta;
import com.github.difflib.patch.Patch;
import org.mule.api.vcs.client.BranchInfo;
import org.mule.api.vcs.client.MergeOperation;
import org.mule.api.vcs.client.service.BranchRepositoryManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DeleteFileDiff implements Diff {

    private String relativePath;
    private List<String> originalLines;

    public DeleteFileDiff(String relativePath, List<String> originalLines) {
        this.relativePath = relativePath;
        this.originalLines = originalLines;
    }

    public String getRelativePath() {
        return relativePath;
    }

    @Override
    public MergeOperation getOperationType() {
        return MergeOperation.DELETE;
    }

    @Override
    public ApplyResult unApply(File targetDirectory) {
        final File file = new File(targetDirectory, relativePath);
        try (final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file), BranchInfo.DEFAULT_CHARSET)) {
            outputStreamWriter.write(originalLines.stream().reduce((l, r) -> l + "\n" + r).orElse(""));
        } catch (IOException e) {
            return ApplyResult.fail(e.getMessage());
        }
        return ApplyResult.SUCCESSFUL;
    }

    @Override
    public ApplyResult apply(File targetDirectory, MergingStrategy mergingStrategy) {
        final File file = new File(targetDirectory, relativePath);
        if (file.exists()) {
            final boolean delete = file.delete();
            if (delete) {
                return ApplyResult.SUCCESSFUL;
            } else {
                final String message = "Unable to delete" + file.getAbsolutePath();
                return ApplyResult.fail(message);
            }
        } else {
            //ApiFile was already deleted
            return ApplyResult.success("File `" + relativePath + "` was deleted on the server and locally. Ignoring this change.");
        }
    }

    public void print(PrintWriter printWriter) {
        printWriter.println("Index: " + relativePath);
        printWriter.println("===================================================================");

        final Patch<String> patch = new Patch<>();
        patch.addDelta(new DeleteDelta<>(new Chunk<>(0, originalLines), new Chunk<>(0, new String[0])));

        final List<String> stringList = UnifiedDiffUtils.generateUnifiedDiff(relativePath, relativePath, new ArrayList<>(), patch, 2);
        for (String line : stringList) {
            printWriter.println(line);
        }
    }

    @Override
    public void push(BranchRepositoryManager branch, File targetDirectory) {
        branch.delete(relativePath);
    }


}
