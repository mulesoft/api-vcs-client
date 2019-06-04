package org.mule.api.vcs.client.service;

public class OrgIdUserInfoProviderDecorator implements UserInfoProvider {

    private UserInfoProvider wrapper;
    private String orgId;

    public OrgIdUserInfoProviderDecorator(UserInfoProvider wrapper, String orgId) {
        this.wrapper = wrapper;
        this.orgId = orgId;
    }

    @Override
    public String getAccessToken() {
        return wrapper.getAccessToken();
    }

    @Override
    public String getOrgId() {
        return orgId;
    }

    @Override
    public String getUserId() {
        return wrapper.getUserId();
    }

    public static UserInfoProvider withOrgId(UserInfoProvider provider, String orgId){
        return new OrgIdUserInfoProviderDecorator(provider, orgId);
    }
}
