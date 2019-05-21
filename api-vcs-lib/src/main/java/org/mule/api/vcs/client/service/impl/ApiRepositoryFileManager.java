package org.mule.api.vcs.client.service.impl;

import org.mule.api.vcs.client.BranchInfo;
import org.mule.api.vcs.client.service.*;
import org.mule.designcenter.api.ApiDesignerXapiClient;
import org.mule.designcenter.exceptions.ApiDesignerXapiException;
import org.mule.designcenter.model.Lock;
import org.mule.designcenter.model.ProjectCreate;
import org.mule.designcenter.resource.projects.model.Project;
import org.mule.designcenter.resource.projects.model.ProjectsGETHeader;
import org.mule.designcenter.resource.projects.model.ProjectsPOSTHeader;
import org.mule.designcenter.resource.projects.projectId.branches.branch.acquireLock.model.AcquireLockPOSTHeader;
import org.mule.designcenter.resource.projects.projectId.branches.branch.releaseLock.model.ReleaseLockPOSTHeader;
import org.mule.designcenter.resource.projects.projectId.branches.model.Branch;
import org.mule.designcenter.resource.projects.projectId.branches.model.BranchesGETHeader;
import org.mule.designcenter.responses.ApiDesignerXapiResponse;

import java.util.List;
import java.util.stream.Collectors;

public class ApiRepositoryFileManager implements RepositoryFileManager {

    private ApiDesignerXapiClient client;

    private final UserInfoProvider provider;

    public ApiRepositoryFileManager(UserInfoProvider provider) {
        this.provider = provider;
        this.client = ApiDesignerXapiClient.create();
    }

    public ApiRepositoryFileManager(String url, UserInfoProvider provider) {
        client = ApiDesignerXapiClient.create(url);
        this.provider = provider;
    }


    @Override
    public BranchRepositoryLock acquireLock(String projectId, String branchName) {
        try {
            final String userId = provider.getUserId();
            final String accessToken = provider.getAccessToken();
            final String orgId = provider.getOrgId();
            final org.mule.designcenter.resource.projects.projectId.branches.branch.Branch branch = client.projects.projectId(projectId).branches.branch(branchName);
            final ApiDesignerXapiResponse<Lock> post = branch.acquireLock.post(new AcquireLockPOSTHeader(orgId, userId), accessToken);
            final Boolean locked = post.getBody().getLocked();
            final ApiManagerBranchManager branchManager = new ApiManagerBranchManager(provider, branch);
            return new BranchRepositoryLock(locked, post.getBody().getName(), branchManager);
        } catch (ApiDesignerXapiException e) {
            throw new RuntimeException(e.getReason());
        }
    }

    @Override
    public void releaseLock(String projectId, String branchName) {
        client.projects.projectId(projectId).branches.branch(branchName).releaseLock.post(new ReleaseLockPOSTHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
    }

    @Override
    public List<ApiBranch> branches(String projectId) {
        final ApiDesignerXapiResponse<List<Branch>> response = client.projects.projectId(projectId).branches.get(new BranchesGETHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
        return response.getBody().stream().map((branch) -> new ApiBranch(branch.getName())).collect(Collectors.toList());
    }

    @Override
    public List<ProjectInfo> projects() {
        final ApiDesignerXapiResponse<List<Project>> response = client.projects.get(new ProjectsGETHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
        return response.getBody().stream().filter(p -> !p.getType().equalsIgnoreCase("Mule_Application")).map((p) -> new ProjectInfo(p.getId(), p.getName(), p.getDescription())).collect(Collectors.toList());
    }

    @Override
    public BranchInfo create(ApiType apiType, String name, String description) {
        final ApiDesignerXapiResponse<org.mule.designcenter.model.Project> post = client.projects.post(new ProjectCreate(name, description, apiType.getType()), new ProjectsPOSTHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
        return new BranchInfo(post.getBody().getId(), "master");
    }
}
