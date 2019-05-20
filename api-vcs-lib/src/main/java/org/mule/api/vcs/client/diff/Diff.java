package org.mule.api.vcs.client.diff;

import org.mule.api.vcs.client.service.BranchRepositoryManager;

import java.io.File;
import java.io.PrintWriter;

public interface Diff {

    String OURS_FILE_EXTENSION = ".ours";

    ApplyResult apply(File targetDirectory, MergingStrategy mergingStrategy);

    void print(PrintWriter printWriter);

    void push(BranchRepositoryManager branch, File targetDirectory);
}
