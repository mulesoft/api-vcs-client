package org.mule.designcenter.vcs.client.diff;

import org.mule.designcenter.vcs.client.service.BranchFileManager;

import java.io.File;
import java.io.PrintWriter;

public class DeleteFileDiff implements Diff {

    private String relativePath;

    public DeleteFileDiff(String relativePath) {
        this.relativePath = relativePath;
    }

    @Override
    public ApplyResult apply(File targetDirectory) {
        final File file = new File(targetDirectory, relativePath);
        if (file.exists()) {
            final boolean delete = file.delete();
            if (delete) {
                return ApplyResult.SUCCESSFUL;
            } else {
                return ApplyResult.fail("Unable to delete" + file.getAbsolutePath());
            }
        } else {
            //ApiFile was already deleted
            return ApplyResult.SUCCESSFUL;
        }
    }

    public void print(PrintWriter printWriter) {
        printWriter.println("[New File] " + relativePath);
    }

    @Override
    public void push(BranchFileManager branch, File targetDirectory) {
        branch.delete(relativePath);
    }


}
