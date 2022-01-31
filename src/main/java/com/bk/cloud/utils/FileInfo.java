package com.bk.cloud.utils;

public class FileInfo {
    private String fileName;
    private String fileType;

    public FileInfo(String fileName, String fileType) {
        this.fileName = fileName;
        this.fileType = fileType;
    }

    public FileInfo(String fileName) {
        this.fileName = fileName;
    }

    public FileInfo() {
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String toString() {
        String fInfo = null;
        if (this.fileType.equals("")) {
            fInfo = this.fileName;
        } else {
            fInfo = this.fileName + "-&!&-" + this.fileType;
        }
        return fInfo;
    }

    public void fileInfoFromString(String file) {
        if (file.contains("-&!&-")) {
            String[] parts = file.split("-&!&-");
            this.fileName = parts[0];
            this.fileType = parts[1];
        } else {
            this.fileName = file;
        }

    }
}
