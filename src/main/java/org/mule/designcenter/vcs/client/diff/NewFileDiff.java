package org.mule.designcenter.vcs.client.diff;

import org.mule.designcenter.vcs.client.service.BranchFileManager;

import java.io.*;
import java.nio.file.Files;

public class NewFileDiff implements Diff {
    private byte[] content;
    private String conentType;
    private String relativePath;

    public NewFileDiff(byte[] content, String conentType, String relativePath) {
        this.content = content;
        this.conentType = conentType;
        this.relativePath = relativePath;
    }

    @Override
    public ApplyResult apply(File targetDirectory) {
        final File file = new File(targetDirectory, relativePath);
        if (file.exists()) {
            return ApplyResult.fail("File already exists " + file.getAbsolutePath());
        } else {
            try (final FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                fileOutputStream.write(content);
            } catch (IOException e) {
                return ApplyResult.fail(e.getMessage());
            }
            return ApplyResult.SUCCESSFUL;
        }
    }

    @Override
    public void print(PrintWriter printWriter) {
        printWriter.println("[New File] " + relativePath);
    }

    @Override
    public void push(BranchFileManager branch, File targetDirectory) {
        final File file = new File(targetDirectory, relativePath);
        try {
            final String type = Files.probeContentType(file.toPath());
            branch.newFile(relativePath, Files.readAllBytes(file.toPath()), type);
        } catch (IOException e) {
        }
    }


}
