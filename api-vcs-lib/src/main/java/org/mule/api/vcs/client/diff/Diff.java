package org.mule.api.vcs.client.diff;

import org.mule.api.vcs.client.service.BranchRepositoryManager;

import java.io.File;
import java.io.PrintWriter;

public interface Diff {

    String ORIGINAL_FILE_EXTENSION = ".original";

    String THEIRS_FILE_EXTENSION = ".theirs";

    ApplyResult apply(File targetDirectory, MergingStrategy mergingStrategy);

    void print(PrintWriter printWriter);

    void push(BranchRepositoryManager branch, File targetDirectory);

    String getRelativePath();

    MergeOperation getOperationType();

    ApplyResult unApply(File targetDirectory);
}
