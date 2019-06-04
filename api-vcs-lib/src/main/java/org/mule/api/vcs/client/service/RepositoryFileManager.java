package org.mule.api.vcs.client.service;

import org.mule.api.vcs.client.BranchInfo;

import java.util.List;

public interface RepositoryFileManager {

    BranchRepositoryLock acquireLock(UserInfoProvider provider, String projectId, String branchName);

    void releaseLock(UserInfoProvider provider, String projectId, String branchName);

    List<ApiBranch> branches(UserInfoProvider provider, String projectId);

    List<ProjectInfo> projects(UserInfoProvider provider);

    BranchInfo create(UserInfoProvider provider, ApiType apiType, String name, String description);

}
