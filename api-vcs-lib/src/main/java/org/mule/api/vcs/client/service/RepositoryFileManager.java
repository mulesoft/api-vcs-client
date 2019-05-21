package org.mule.api.vcs.client.service;

import org.mule.api.vcs.client.BranchInfo;

import java.util.List;

public interface RepositoryFileManager {

    BranchRepositoryLock acquireLock(String projectId, String branchName);

    void releaseLock(String projectId, String branchName);

    List<ApiBranch> branches(String projectId);

    List<ProjectInfo> projects();

    BranchInfo create(ApiType apiType, String name, String description);

}
