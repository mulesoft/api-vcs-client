package org.mule.designcenter.vcs.client.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MockBranchFileManager implements BranchFileManager {

    private File branchDirectory;

    public MockBranchFileManager(File branchDirectory) {
        this.branchDirectory = branchDirectory;
    }

    @Override
    public boolean delete(String path) {
        return false;
    }

    @Override
    public boolean newFile(String path, byte[] content, String mimeType) {
        return false;
    }

    @Override
    public boolean updateFile(String path, byte[] content) {
        return false;
    }

    @Override
    public List<ApiFile> listFiles() {
        return Arrays.stream(branchDirectory.list()).map((name) -> new ApiFile(name, ApiFileType.FILE)).collect(Collectors.toList());
    }

    @Override
    public ApiFileContent fileContent(String path) {
        try {
            final File file = new File(branchDirectory, path);
            final byte[] bytes = Files.readAllBytes(file.toPath());
            return new ApiFileContent(bytes, Files.probeContentType(file.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
