package org.mule.api.vcs.client.diff;

import org.mule.api.vcs.client.service.BranchRepositoryManager;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

public class NewFileConflictDiff implements Diff {


    private List<String> theirs;
    private List<String> ours;
    private String relativePath;

    public NewFileConflictDiff(List<String> theirs, List<String> ours, String relativePath) {

        this.theirs = theirs;
        this.ours = ours;
        this.relativePath = relativePath;
    }

    @Override
    public ApplyResult apply(File targetDirectory, MergingStrategy mergingStrategy) {
        final File theirsFile = new File(targetDirectory, relativePath + Diff.THEIRS_FILE_EXTENSION);
        switch (mergingStrategy) {
            case KEEP_OURS:
                theirsFile.delete();
                break;
            case KEEP_THEIRS:
                FileUtils.writeFile(targetDirectory, relativePath, theirs);
                theirsFile.delete();
                break;
        }
        return ApplyResult.fail("Merge conflicts can not be applied");
    }

    public List<String> getTheirs() {
        return theirs;
    }

    public List<String> getOurs() {
        return ours;
    }

    @Override
    public void print(PrintWriter printWriter) {

    }

    @Override
    public void push(BranchRepositoryManager branch, File targetDirectory) {

    }

    @Override
    public String getRelativePath() {
        return relativePath;
    }

    @Override
    public MergeOperation getOperationType() {
        return MergeOperation.NEW_FILE_CONFLICT;
    }

    @Override
    public ApplyResult unApply(File targetDirectory) {
        return ApplyResult.fail("Merge conflicts can not be un applied");
    }
}
