package org.mule.api.vcs.client.diff;

import org.mule.api.vcs.client.service.BranchRepositoryManager;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

public class NewFileConflictDiff implements Diff, Conflict {


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
        return ApplyResult.fail("NewFile conflicts can not be un applied");
    }

    @Override
    public ApplyResult resolve(File targetDirectory, MergingStrategy mergingStrategy) {
        final File theirsFile = new File(targetDirectory, relativePath + Diff.THEIRS_FILE_EXTENSION);
        switch (mergingStrategy) {
            case KEEP_OURS:
                theirsFile.delete();
                return ApplyResult.SUCCESSFUL;
            case KEEP_THEIRS:
                FileUtils.writeFile(targetDirectory, relativePath, theirs);
                theirsFile.delete();
                return ApplyResult.SUCCESSFUL;
            default:
                return ApplyResult.fail("NewFile Conflict doesn't support " + mergingStrategy.name());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewFileConflictDiff that = (NewFileConflictDiff) o;
        return Objects.equals(theirs, that.theirs) &&
                Objects.equals(ours, that.ours) &&
                Objects.equals(relativePath, that.relativePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(theirs, ours, relativePath);
    }
}
