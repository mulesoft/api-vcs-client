package org.mule.api.vcs.client.service;

import java.util.List;

public interface BranchFileManager {
    boolean delete(String path);

    boolean newFile(String path, byte[] content, String mimeType);

    boolean updateFile(String path, byte[] content);

    List<ApiFile> listFiles();

    ApiFileContent fileContent(String path);
}
