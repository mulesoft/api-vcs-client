package org.mule.api.vcs.client;

import java.util.List;

public class PublishInfo {

    private final String name;
    private final String apiVersion;
    private final String version;
    private final List<Object> tags;
    private final String main;
    private final String assetId;
    private final String groupId;
    private final String classifier;
    private final BranchInfo branchInfo;

    public PublishInfo(String name, String apiVersion, String version, List<Object> tags, String main, String assetId, String groupId, String classifier, BranchInfo branchInfo) {
        this.name = name;
        this.apiVersion = apiVersion;
        this.version = version;
        this.tags = tags;
        this.main = main;
        this.assetId = assetId;
        this.groupId = groupId;
        this.classifier = classifier;
        this.branchInfo = branchInfo;

    }

    public String getName() {
        return name;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getVersion() {
        return version;
    }

    public List<Object> getTags() {
        return tags;
    }

    public String getMain() {
        return main;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getClassifier() {
        return classifier;
    }

    public BranchInfo getBranchInfo() {
        return branchInfo;
    }


}
