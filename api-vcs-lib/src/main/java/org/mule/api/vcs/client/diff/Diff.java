package org.mule.api.vcs.client.diff;

import org.mule.api.vcs.client.service.BranchFileManager;

import java.io.File;
import java.io.PrintWriter;

public interface Diff {

    String OURS_FILE_EXTENSION = ".ours";

    ApplyResult apply(File targetDirectory, MergingStrategy mergingStrategy);

    void print(PrintWriter printWriter);

    void push(BranchFileManager branch, File targetDirectory);
}
