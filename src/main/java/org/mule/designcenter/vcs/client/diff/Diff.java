package org.mule.designcenter.vcs.client.diff;

import org.mule.designcenter.vcs.client.service.BranchFileManager;

import java.io.File;
import java.io.PrintWriter;

public interface Diff {

    ApplyResult apply(File targetDirectory);

    void print(PrintWriter printWriter);

    void push(BranchFileManager branch, File targetDirectory);
}
