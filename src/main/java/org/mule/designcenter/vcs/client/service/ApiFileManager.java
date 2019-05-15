package org.mule.designcenter.vcs.client.service;

import java.util.List;

public interface ApiFileManager {



    ApiLock acquireLock(String projectId, String branchName);

    void releaseLock(String projectId,String branchName);

    List<ApiBranch> listBranches(String projectId);


}
