package org.mule.api.vcs.client.service.impl;

import org.mule.api.vcs.client.BranchInfo;
import org.mule.api.vcs.client.service.*;
import org.mule.designcenter.model.File;
import org.mule.designcenter.model.FileContent;
import org.mule.designcenter.resource.projects.projectId.branches.branch.Branch;
import org.mule.designcenter.resource.projects.projectId.branches.branch.files.filePath.model.FilePathDELETEHeader;
import org.mule.designcenter.resource.projects.projectId.branches.branch.files.filePath.model.FilePathGETHeader;
import org.mule.designcenter.resource.projects.projectId.branches.branch.files.model.FilesGETHeader;
import org.mule.designcenter.resource.projects.projectId.branches.branch.save.model.SavePOSTHeader;
import org.mule.designcenter.responses.ApiDesignerXapiResponse;


import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ApiManagerBranchManager implements BranchFileManager {


    private final UserInfoProvider provider;
    private final org.mule.designcenter.resource.projects.projectId.branches.branch.Branch branch;

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
        final List<FileContent> fileContents = Collections.singletonList(new FileContent(path, new String(content, BranchInfo.DEFAULT_CHARSET)));
        final ApiDesignerXapiResponse<List<File>> post = branch.save.post(fileContents, new SavePOSTHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
        return true;
    }

    @Override
    public boolean updateFile(String path, byte[] content) {
        final List<FileContent> fileContents = Collections.singletonList(new FileContent(path, new String(content, BranchInfo.DEFAULT_CHARSET)));
        final ApiDesignerXapiResponse<List<File>> post = branch.save.post(fileContents, new SavePOSTHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
        return true;
    }

    @Override
    public List<ApiFile> listFiles() {
        final List<File> fileList = branch.files.get(new FilesGETHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken()).getBody();
        return fileList.stream().map((file) -> new ApiFile(file.getPath(), ApiFileType.valueOf(file.getType()))).collect(Collectors.toList());
    }

    @Override
    public ApiFileContent fileContent(String path) {
        final ApiDesignerXapiResponse<String> stringApiDesignerXapiResponse = branch.files.filePath(path).get(new FilePathGETHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
        final String body = stringApiDesignerXapiResponse.getBody();
        return new ApiFileContent(body.getBytes(BranchInfo.DEFAULT_CHARSET), stringApiDesignerXapiResponse.getResponse().getMediaType().toString());
    }
}
