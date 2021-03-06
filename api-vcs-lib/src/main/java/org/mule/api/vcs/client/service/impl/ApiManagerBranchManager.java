package org.mule.api.vcs.client.service.impl;

import org.mule.api.vcs.client.BranchInfo;
import org.mule.api.vcs.client.service.*;
import org.mule.apidesigner.model.File;
import org.mule.apidesigner.model.FileContent;
import org.mule.apidesigner.resource.projects.projectId.branches.branch.Branch;
import org.mule.apidesigner.resource.projects.projectId.branches.branch.files.filePath.model.FilePathDELETEHeader;
import org.mule.apidesigner.resource.projects.projectId.branches.branch.files.filePath.model.FilePathGETHeader;
import org.mule.apidesigner.resource.projects.projectId.branches.branch.files.model.FilesGETHeader;
import org.mule.apidesigner.resource.projects.projectId.branches.branch.save.model.SavePOSTHeader;
import org.mule.apidesigner.responses.ApiDesignerXapiResponse;


import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ApiManagerBranchManager implements BranchRepositoryManager {


    private final UserInfoProvider provider;
    private final org.mule.apidesigner.resource.projects.projectId.branches.branch.Branch branch;

    public ApiManagerBranchManager(UserInfoProvider provider, Branch branch) {
        this.provider = provider;
        this.branch = branch;
    }

    @Override
    public boolean delete(String path) {
        final ApiDesignerXapiResponse<Void> delete = branch.files.filePath(path).delete(new FilePathDELETEHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
        return true;
    }

    @Override
    public boolean newFile(String path, byte[] content, String mimeType) {
        return doSave(path, content);
    }

    private boolean doSave(String path, byte[] content) {
        final String fileContent = new String(content, BranchInfo.DEFAULT_CHARSET);
        final List<FileContent> fileContents = Collections.singletonList(new FileContent(path, fileContent));
        final ApiDesignerXapiResponse<List<File>> post = branch.save.post(fileContents, new SavePOSTHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
        return true;
    }

    @Override
    public boolean updateFile(String path, byte[] content) {
        return doSave(path, content);
    }

    @Override
    public List<ApiFile> listFiles() {
        final List<File> fileList = branch.files.get(new FilesGETHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken()).getBody();
        return fileList.stream()
                .filter((file) -> file.getType().equalsIgnoreCase("FILE"))
                .map((file) -> new ApiFile(file.getPath(), ApiFileType.valueOf(file.getType())))
                .collect(Collectors.toList());
    }

    @Override
    public ApiFileContent fileContent(String path) {
        final ApiDesignerXapiResponse<String> stringApiDesignerXapiResponse = branch.files.filePath(path).get(new FilePathGETHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
        final String body = stringApiDesignerXapiResponse.getBody();
        return new ApiFileContent(body.getBytes(BranchInfo.DEFAULT_CHARSET), stringApiDesignerXapiResponse.getResponse().getMediaType().toString());
    }
}
