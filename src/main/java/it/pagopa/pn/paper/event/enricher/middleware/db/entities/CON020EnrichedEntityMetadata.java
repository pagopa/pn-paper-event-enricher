package it.pagopa.pn.paper.event.enricher.middleware.db.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.Instant;

@Getter
@Setter
@ToString
@DynamoDbBean
public class CON020EnrichedEntityMetadata {

    public static final String COL_IUN = "iun";
    public static final String COL_GENERATIONTIME = "generationTime";
    public static final String COL_RECINDEX = "recIndex";
    public static final String COL_SENDREQUESTID = "sendRequestId";
    public static final String COL_REGISTEREDLETTERCORE = "registeredLetterCode";
    public static final String COL_EVENTTIME = "eventTime";
    public static final String COL_ARCHIVEFILEKEY = "archiveFileKey";

    private String iun;
    private Instant generationTime;
    private String recIndex;
    private String sendRequestId;
    private String registeredLetterCode;
    private Instant eventTime;
    private String archiveFileKey;
}
