package it.pagopa.pn.paper.event.enricher.model;

import lombok.Data;

@Data
public class IndexData {
    private String filename;
    private String requestId;
    private String registeredLetterCode;

    public IndexData( String requestId, String registeredLetterCode, String filename) {
        this.requestId = requestId;
        this.registeredLetterCode = registeredLetterCode;
        this.filename = filename;
    }
}
