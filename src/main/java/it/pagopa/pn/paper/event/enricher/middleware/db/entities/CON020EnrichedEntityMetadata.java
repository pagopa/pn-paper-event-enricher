package it.pagopa.pn.paper.event.enricher.middleware.db.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@ToString
public class CON020EnrichedEntityMetadata {
    private String iun;
    private Instant generationDate;
    private String recIndex;
    private String sendRequestId;
    private String registeredLetterCode;
    private Instant eventTime;
    private String archiveFileKey;
}
