package org.mule.designcenter.vcs.client.service;

public class ApiFile {
    private String path;
    private ApiFileType fileType;

    public ApiFile(String path,ApiFileType fileType) {
        this.path = path;
        this.fileType = fileType;
    }

    public String getPath() {
        return path;
    }

    public ApiFileType getFileType() {
        return fileType;
    }
}
