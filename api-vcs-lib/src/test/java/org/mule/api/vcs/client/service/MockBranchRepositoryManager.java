package org.mule.api.vcs.client.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class MockBranchRepositoryManager implements BranchRepositoryManager {

    private File branchDirectory;

    public MockBranchRepositoryManager(File branchDirectory) {
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
        final ArrayList<ApiFile> result = new ArrayList<>();
        final Path root = branchDirectory.toPath();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    result.add(new ApiFile(root.relativize(file).toString(), ApiFileType.FILE));
                    return FileVisitResult.CONTINUE;
                }


            });
        } catch (Exception e) {

        }

        return result;
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
