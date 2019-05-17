package org.mule.api.vcs.client.service;

public interface UserInfoProvider {

    String getAccessToken();

    String getOrgId();

    String getUserId();
}
