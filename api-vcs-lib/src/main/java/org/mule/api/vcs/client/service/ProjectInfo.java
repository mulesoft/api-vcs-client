package org.mule.api.vcs.client.service;

public class ProjectInfo {
    private String projectId;
    private String projectName;
    private String projectDescription;

    public ProjectInfo(String projectId, String projectName, String projectDescription) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.projectDescription = projectDescription;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public String getProjectName() {
        return projectName;
    }
}
