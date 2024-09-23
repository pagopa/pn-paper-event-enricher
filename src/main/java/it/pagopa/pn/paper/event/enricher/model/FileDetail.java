package it.pagopa.pn.paper.event.enricher.model;

import lombok.Data;

@Data
public class FileDetail {
    private String requestId;
    private String registeredLetterCode;
    private String filename;
    private byte[] content;
    private String fileKey;

    public FileDetail(String filename, byte[] content, String fileKey) {
        this.filename = filename;
        this.content = content;
        this.fileKey = fileKey;
    }
}
