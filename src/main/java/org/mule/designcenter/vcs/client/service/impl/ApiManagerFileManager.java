package org.mule.designcenter.vcs.client.service.impl;

import org.mule.designcenter.api.ApiDesignerXapiClient;
import org.mule.designcenter.model.Lock;
import org.mule.designcenter.resource.projects.projectId.branches.branch.acquireLock.model.AcquireLockPOSTHeader;
import org.mule.designcenter.resource.projects.projectId.branches.branch.releaseLock.model.ReleaseLockPOSTHeader;
import org.mule.designcenter.resource.projects.projectId.branches.model.Branch;
import org.mule.designcenter.resource.projects.projectId.branches.model.BranchesGETHeader;
import org.mule.designcenter.responses.ApiDesignerXapiResponse;
import org.mule.designcenter.vcs.client.service.AccessTokenProvider;
import org.mule.designcenter.vcs.client.service.ApiBranch;
import org.mule.designcenter.vcs.client.service.ApiFileManager;
import org.mule.designcenter.vcs.client.service.ApiLock;

import java.util.List;
import java.util.stream.Collectors;

public class ApiManagerFileManager implements ApiFileManager {

    private ApiDesignerXapiClient client;
    private final String orgId;
    private final String userId;
    private final AccessTokenProvider provider;

    public ApiManagerFileManager(String orgId, String userId, AccessTokenProvider provider) {
        this.orgId = orgId;
        this.userId = userId;
        this.provider = provider;
        client = ApiDesignerXapiClient.create();
    }

    public ApiManagerFileManager(String url, String orgId, String userId, AccessTokenProvider provider) {
        client = ApiDesignerXapiClient.create(url);
        this.orgId = orgId;
        this.userId = userId;
        this.provider = provider;
    }


    @Override
    public ApiLock acquireLock(String projectId, String branchName) {
        final org.mule.designcenter.resource.projects.projectId.branches.branch.Branch branch = client.projects.projectId(projectId).branches.branch(branchName);
        final ApiDesignerXapiResponse<Lock> post = branch.acquireLock.post(new AcquireLockPOSTHeader(orgId, userId), provider.getAccessToken());
        final Boolean locked = post.getBody().getLocked();
        final ApiManagerBranchManager branchManager = new ApiManagerBranchManager(orgId, userId, provider, branch);
        return new ApiLock(locked, post.getBody().getName(), branchManager);
    }

    @Override
    public void releaseLock(String projectId, String branchName) {
        final ApiDesignerXapiResponse<String> post = client.projects.projectId(projectId).branches.branch(branchName).releaseLock.post(new ReleaseLockPOSTHeader(orgId, userId), provider.getAccessToken());
    }

    @Override
    public List<ApiBranch> listBranches(String projectId) {
        final ApiDesignerXapiResponse<List<Branch>> response = client.projects.projectId(projectId).branches.get(new BranchesGETHeader(orgId, userId), provider.getAccessToken());
        return response.getBody().stream().map((branch) -> new ApiBranch(branch.getName())).collect(Collectors.toList());
    }
}
