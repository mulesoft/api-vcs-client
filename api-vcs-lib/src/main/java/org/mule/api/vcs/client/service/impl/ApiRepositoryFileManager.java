package org.mule.api.vcs.client.service.impl;

import org.mule.api.vcs.client.BranchInfo;
import org.mule.api.vcs.client.PublishInfo;
import org.mule.api.vcs.client.service.*;
import org.mule.apidesigner.api.ApiDesignerXapiClient;
import org.mule.apidesigner.exceptions.ApiDesignerXapiException;
import org.mule.apidesigner.model.Lock;
import org.mule.apidesigner.model.ProjectCreate;
import org.mule.apidesigner.resource.projects.model.Project;
import org.mule.apidesigner.resource.projects.model.ProjectsGETHeader;
import org.mule.apidesigner.resource.projects.model.ProjectsPOSTHeader;
import org.mule.apidesigner.resource.projects.projectId.branches.branch.acquireLock.model.AcquireLockPOSTHeader;
import org.mule.apidesigner.resource.projects.projectId.branches.branch.publish.exchange.model.ExchangePOSTBody;
import org.mule.apidesigner.resource.projects.projectId.branches.branch.publish.exchange.model.ExchangePOSTHeader;
import org.mule.apidesigner.resource.projects.projectId.branches.branch.publish.exchange.model.Metadata;
import org.mule.apidesigner.resource.projects.projectId.branches.branch.releaseLock.model.ReleaseLockPOSTHeader;
import org.mule.apidesigner.resource.projects.projectId.branches.model.Branch;
import org.mule.apidesigner.resource.projects.projectId.branches.model.BranchesGETHeader;
import org.mule.apidesigner.responses.ApiDesignerXapiResponse;

import java.util.List;
import java.util.stream.Collectors;

public class ApiRepositoryFileManager implements RepositoryFileManager {

    private ApiDesignerXapiClient client;

    public ApiRepositoryFileManager() {
        this.client = ApiDesignerXapiClient.create();
    }

    public ApiRepositoryFileManager(String url) {
        client = ApiDesignerXapiClient.create(url);
    }


    @Override
    public BranchRepositoryLock acquireLock(UserInfoProvider provider, String projectId, String branchName) {
        try {
            final String userId = provider.getUserId();
            final String accessToken = provider.getAccessToken();
            final String orgId = provider.getOrgId();
            final org.mule.apidesigner.resource.projects.projectId.branches.branch.Branch branch = client.projects.projectId(projectId).branches.branch(branchName);
            final ApiDesignerXapiResponse<Lock> post = branch.acquireLock.post(new AcquireLockPOSTHeader(orgId, userId), accessToken);
            final Boolean locked = post.getBody().getLocked();
            final ApiManagerBranchManager branchManager = new ApiManagerBranchManager(provider, branch);
            return new BranchRepositoryLock(locked, post.getBody().getName(), branchManager);
        } catch (ApiDesignerXapiException e) {
            throw new RuntimeException(e.getReason());
        }
    }

    @Override
    public void releaseLock(UserInfoProvider provider, String projectId, String branchName) {
        client.projects.projectId(projectId).branches.branch(branchName).releaseLock.post(new ReleaseLockPOSTHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
    }

    @Override
    public List<ApiBranch> branches(UserInfoProvider provider, String projectId) {
        final ApiDesignerXapiResponse<List<Branch>> response = client.projects.projectId(projectId).branches.get(new BranchesGETHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
        return response.getBody().stream().map((branch) -> new ApiBranch(branch.getName())).collect(Collectors.toList());
    }

    @Override
    public List<ProjectInfo> projects(UserInfoProvider provider) {
        final ApiDesignerXapiResponse<List<Project>> response = client.projects.get(new ProjectsGETHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
        return response.getBody().stream().filter(p -> !p.getType().equalsIgnoreCase("Mule_Application")).map((p) -> new ProjectInfo(p.getId(), p.getName(), p.getDescription())).collect(Collectors.toList());
    }

    @Override
    public BranchInfo create(UserInfoProvider provider, ApiType apiType, String name, String description) {
        final ApiDesignerXapiResponse<org.mule.apidesigner.model.Project> post = client.projects.post(new ProjectCreate(name, description, apiType.getType()), new ProjectsPOSTHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
        return new BranchInfo(post.getBody().getId(), "master", provider.getOrgId());
    }

    @Override
    public void publish(UserInfoProvider provider, PublishInfo publishInfo) {
        final ExchangePOSTBody exchangePOSTBody = new ExchangePOSTBody();
        exchangePOSTBody.setApiVersion(publishInfo.getApiVersion());
        exchangePOSTBody.setAssetId(publishInfo.getAssetId());
        exchangePOSTBody.setGroupId(publishInfo.getGroupId());
        exchangePOSTBody.setClassifier(publishInfo.getClassifier());
        exchangePOSTBody.setName(publishInfo.getName());
        exchangePOSTBody.setMain(publishInfo.getMain());
        exchangePOSTBody.setVersion(publishInfo.getVersion());
        exchangePOSTBody.setMetadata(new Metadata(publishInfo.getBranchInfo().getProjectId(), publishInfo.getBranchInfo().getBranch()));
        client.projects.projectId(publishInfo.getBranchInfo().getProjectId()).branches.branch(publishInfo.getBranchInfo().getBranch()).publish.exchange.post(exchangePOSTBody, new ExchangePOSTHeader(provider.getOrgId(), provider.getUserId()), provider.getAccessToken());
    }

}
