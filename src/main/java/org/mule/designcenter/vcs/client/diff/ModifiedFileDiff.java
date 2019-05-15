package org.mule.designcenter.vcs.client.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import org.mule.designcenter.vcs.client.ApiVCSConfig;
import org.mule.designcenter.vcs.client.service.BranchFileManager;

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
    public ApplyResult apply(File targetDirectory) {
        try {
            final Path file = new File(targetDirectory, relativePath).toPath();
            final List<String> source = Files.readAllLines(file, ApiVCSConfig.DEFAULT_CHARSET);
            final List<String> patch;
            try {
                patch = DiffUtils.patch(source, diff);
            } catch (PatchFailedException e) {
                //We should some how patch it and
                return ApplyResult.fail(e.getMessage());
            }
            Files.write(file, patch, ApiVCSConfig.DEFAULT_CHARSET, StandardOpenOption.WRITE);
            return ApplyResult.SUCCESSFUL;
        } catch (IOException e) {
            return ApplyResult.fail(e.getMessage());
        }
    }

    @Override
    public void print(PrintWriter printWriter) {
        final List<String> stringList = UnifiedDiffUtils.generateUnifiedDiff(relativePath, relativePath, originalLines, diff, 2);
        for (String line : stringList) {
            printWriter.println(line);
        }
    }

    @Override
    public void push(BranchFileManager branch, File targetDirectory) {
        final Path file = new File(targetDirectory, relativePath).toPath();
        try {
            branch.updateFile(relativePath, Files.readAllBytes(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
