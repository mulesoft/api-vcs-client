package org.mule.designcenter.vcs.client.diff;

import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.InsertDelta;
import com.github.difflib.patch.Patch;
import org.mule.designcenter.vcs.client.ApiVCSConfig;
import org.mule.designcenter.vcs.client.service.BranchFileManager;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class NewFileDiff implements Diff {
    private byte[] content;
    private String relativePath;

    public NewFileDiff(byte[] content, String relativePath) {
        this.content = content;
        this.relativePath = relativePath;
    }

    @Override
    public ApplyResult apply(File targetDirectory, MergingStrategy mergingStrategy) {
        final File file = new File(targetDirectory, relativePath);
        if (file.exists()) {
            switch (mergingStrategy) {
                case KEEP_BOTH:
                    createFile(new File(targetDirectory, relativePath + Diff.OURS_FILE_EXTENSION));
                    break;
                case KEEP_OURS:
                    createFile(file);
                    break;
            }
            return ApplyResult.fail("File already exists " + file.getAbsolutePath() + ". Resolution strategy applied was : " + mergingStrategy);
        } else {
            return createFile(file);
        }
    }

    private ApplyResult createFile(File file) {
        if (!file.getParentFile().exists()) {
            //Make sure container is present
            file.getParentFile().mkdirs();
        }
        try (final FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(content);
        } catch (IOException e) {
            return ApplyResult.fail("Unable to create file " + e.getMessage());
        }
        return ApplyResult.SUCCESSFUL;
    }

    @Override
    public void print(PrintWriter printWriter) {
        printWriter.println("Index: " + relativePath);
        printWriter.println("===================================================================");
        final Patch<String> patch = new Patch<>();
        patch.addDelta(new InsertDelta<>(new Chunk<>(0, new String[0]), new Chunk<>(0, getLines())));
        final List<String> stringList = UnifiedDiffUtils.generateUnifiedDiff(relativePath, relativePath, new ArrayList<>(), patch, 2);
        for (String line : stringList) {
            printWriter.println(line);
        }
    }

    private String[] getLines() {
        final String content = new String(this.content, ApiVCSConfig.DEFAULT_CHARSET);
        return content.split("\n");
    }

    @Override
    public void push(BranchFileManager branch, File targetDirectory) {
        final File file = new File(targetDirectory, relativePath);
        try {
            final String type = Files.probeContentType(file.toPath());
            branch.newFile(relativePath, Files.readAllBytes(file.toPath()), type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
