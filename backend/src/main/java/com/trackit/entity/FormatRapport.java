package com.trackit.entity;

public enum FormatRapport {
    PDF("application/pdf", "pdf"),
    EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

    private final String contentType;
    private final String extension;

    FormatRapport(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtension() {
        return extension;
    }
}
