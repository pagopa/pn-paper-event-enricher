package it.pagopa.pn.paper.event.enricher.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;

@Builder
@Getter
@Setter
public class FileDetail {
    private String requestId;
    private String registeredLetterCode;
    private String filename;
    private InputStream content;
    private String fileKey;
    private String sha256;
}
