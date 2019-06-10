package org.mule.api.vcs.client.diff;

import org.mule.api.vcs.client.service.BranchRepositoryManager;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

public class MergeConflictDiff implements Diff, Conflict {

    private List<String> original;
    private List<String> theirs;
    private List<String> ours;
    private String relativePath;

    public MergeConflictDiff(List<String> original, List<String> theirs, List<String> ours, String relativePath) {
        this.original = original;
        this.theirs = theirs;
        this.ours = ours;
        this.relativePath = relativePath;
    }

    public String getTheirsRelativePath() {
        return relativePath + Diff.THEIRS_FILE_EXTENSION;
    }

    public String getOriginalRelativePath() {
        return relativePath + Diff.ORIGINAL_FILE_EXTENSION;
    }

    @Override
    public ApplyResult resolve(File targetDirectory, MergingStrategy mergingStrategy) {
        final File theirsFile = new File(targetDirectory, relativePath + Diff.THEIRS_FILE_EXTENSION);
        final File oursFile = new File(targetDirectory, relativePath + Diff.ORIGINAL_FILE_EXTENSION);
        switch (mergingStrategy) {
            case KEEP_OURS:
                theirsFile.delete();
                oursFile.delete();
                return ApplyResult.SUCCESSFUL;
            case KEEP_THEIRS:
                FileUtils.writeFile(targetDirectory, relativePath, theirs);
                theirsFile.delete();
                oursFile.delete();
                return ApplyResult.SUCCESSFUL;
            default:
                return ApplyResult.fail("Merge Conflict doesn't support " + mergingStrategy);

        }
    }

    @Override
    public ApplyResult apply(File targetDirectory, MergingStrategy mergingStrategy) {
        return ApplyResult.fail("Merge conflicts can not be applied");
    }

    public List<String> getOriginal() {
        return original;
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
        return MergeOperation.MERGE_CONFLICT;
    }

    @Override
    public ApplyResult unApply(File targetDirectory) {
        return ApplyResult.fail("Merge conflicts can not be un applied");
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MergeConflictDiff that = (MergeConflictDiff) o;
        return Objects.equals(original, that.original) &&
                Objects.equals(theirs, that.theirs) &&
                Objects.equals(ours, that.ours) &&
                Objects.equals(relativePath, that.relativePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(original, theirs, ours, relativePath);
    }
}
