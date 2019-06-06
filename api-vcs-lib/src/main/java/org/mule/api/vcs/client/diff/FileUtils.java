package org.mule.api.vcs.client.diff;

import org.mule.api.vcs.client.BranchInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class FileUtils {

    public static ApplyResult writeFile(File targetDirectory, String relativePath, List<String> originalLines) {
        final File file = new File(targetDirectory, relativePath);
        try (final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file), BranchInfo.DEFAULT_CHARSET)) {
            outputStreamWriter.write(originalLines.stream().reduce((l, r) -> l + "\n" + r).orElse(""));
        } catch (IOException e) {
            return ApplyResult.fail(e.getMessage());
        }
        return ApplyResult.SUCCESSFUL;
    }

}
