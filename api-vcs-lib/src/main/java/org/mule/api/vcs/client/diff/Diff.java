package org.mule.api.vcs.client.diff;

import org.mule.api.vcs.client.MergeOperation;
import org.mule.api.vcs.client.service.BranchRepositoryManager;

import java.io.File;
import java.io.PrintWriter;

public interface Diff {

    String OURS_FILE_EXTENSION = ".ours";

    String THEIRS_FILE_EXTENSION = ".theirs";

    ApplyResult apply(File targetDirectory, MergingStrategy mergingStrategy);

    void print(PrintWriter printWriter);

    void push(BranchRepositoryManager branch, File targetDirectory);

    String getRelativePath();

    MergeOperation getOperationType();

    ApplyResult unApply(File targetDirectory);
}
