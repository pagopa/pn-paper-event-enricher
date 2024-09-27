package it.pagopa.pn.paper.event.enricher.model;

import lombok.Data;

import java.io.InputStream;

@Data
public class FileDetail {
    private String requestId;
    private String registeredLetterCode;
    private String filename;
    private InputStream content;
    private String fileKey;

    public FileDetail(String filename, String fileKey, InputStream content) {
        this.filename = filename;
        this.fileKey = fileKey;
        this.content = content;
    }
}
