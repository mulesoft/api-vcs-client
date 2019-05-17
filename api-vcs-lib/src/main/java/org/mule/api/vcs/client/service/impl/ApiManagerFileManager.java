package org.mule.api.vcs.client.service.impl;

import org.mule.api.vcs.client.service.*;
import org.mule.designcenter.api.ApiDesignerXapiClient;
import org.mule.designcenter.exceptions.ApiDesignerXapiException;
import org.mule.designcenter.model.Lock;
import org.mule.designcenter.resource.projects.model.Project;
import org.mule.designcenter.resource.projects.model.ProjectsGETHeader;
import org.mule.designcenter.resource.projects.projectId.branches.branch.acquireLock.model.AcquireLockPOSTHeader;
import org.mule.designcenter.resource.projects.projectId.branches.branch.releaseLock.model.ReleaseLockPOSTHeader;
import org.mule.designcenter.resource.projects.projectId.branches.model.Branch;
import org.mule.designcenter.resource.projects.projectId.branches.model.BranchesGETHeader;
import org.mule.designcenter.responses.ApiDesignerXapiResponse;

import java.util.List;
import java.util.stream.Collectors;

public class ApiManagerFileManager implements ApiFileManager {

    private ApiDesignerXapiClient client;

    private final UserInfoProvider provider;

    public ApiManagerFileManager(UserInfoProvider provider) {
        this.provider = provider;
        this.client = ApiDesignerXapiClient.create();
    }

    public ApiManagerFileManager(String url, UserInfoProvider provider) {
        client = ApiDesignerXapiClient.create(url);
        this.provider = provider;
    }


    @Override
    public ApiLock acquireLock(String projectId, String branchName) {
        try {
            final String userId = provider.getUserId();
            final String accessToken = provider.getAccessToken();
            final String orgId = provider.getOrgId();
            final org.mule.designcenter.resource.projects.projectId.branches.branch.Branch branch = client.projects.projectId(projectId).branches.branch(branchName);
            final ApiDesignerXapiResponse<Lock> post = branch.acquireLock.post(new AcquireLockPOSTHeader(orgId, userId), accessToken);
            final Boolean locked = post.getBody().getLocked();
            final ApiManagerBranchManager branchManager = new ApiManagerBranchManager(provider, branch);
            return new ApiLock(locked, post.getBody().getName(), branchManager);
        } catch (ApiDesignerXapiException e) {
            throw new RuntimeException(e.getReason());
        }
    }

    @Override
    public void releaseLock(String projectId, String branchName) {
        final ApiDesignerXapiResponse<String> post = client.projects.projectId(projectId).branches.branch(branchName).releaseLock.post(new ReleaseLockPOSTHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
    }

    @Override
    public List<ApiBranch> branches(String projectId) {
        final ApiDesignerXapiResponse<List<Branch>> response = client.projects.projectId(projectId).branches.get(new BranchesGETHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
        return response.getBody().stream().map((branch) -> new ApiBranch(branch.getName())).collect(Collectors.toList());
    }

    @Override
    public List<ProjectInfo> projects() {
        final ApiDesignerXapiResponse<List<Project>> response = client.projects.get(new ProjectsGETHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
        return response.getBody().stream().map((p) -> new ProjectInfo(p.getId(), p.getName(), p.getDescription())).collect(Collectors.toList());
    }
}
