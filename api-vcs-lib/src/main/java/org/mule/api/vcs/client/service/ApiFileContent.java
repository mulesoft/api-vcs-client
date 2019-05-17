package org.mule.api.vcs.client.service;

public class ApiFileContent {
    private byte[] content;
    private String mimeType;

    public ApiFileContent(byte[] content, String mimeType) {
        this.content = content;
        this.mimeType = mimeType;
    }

    public byte[] getContent() {
        return content;
    }

    public String getMimeType() {
        return mimeType;
    }

}
