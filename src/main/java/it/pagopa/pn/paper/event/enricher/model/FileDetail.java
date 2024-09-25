package it.pagopa.pn.paper.event.enricher.model;

import lombok.Data;

import java.io.InputStream;

@Data
public class FileDetail {
    private String requestId;
    private String registeredLetterCode;
    private String filename;
    private InputStream content;
    private byte[] contentBytes;
    private String fileKey;

    public FileDetail(String filename, InputStream content, String fileKey, byte[] contentBytes) {
        this.filename = filename;
        this.content = content;
        this.fileKey = fileKey;
        this.contentBytes = contentBytes;
    }
}
