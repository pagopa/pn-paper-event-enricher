package it.pagopa.pn.paper.event.enricher.middleware.db.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;

@Setter
@ToString
@Data
@EqualsAndHashCode
public class CON020BaseEntity {
    public static final String COL_HASH_KEY = "hashKey";
    public static final String COL_SORT_KEY = "sortKey";
    public static final String COL_TTL = "ttl";
    public static final String COL_RECORD_CREATION_TIME = "recordCreationTime";
    public static final String COL_LAST_MODIFICATION_TIME = "lastModificationTime";
    public static final String COL_ENTITY_NAME = "entityName";

    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_HASH_KEY)}))
    private String hashKey;
    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SORT_KEY)}))
    private String sortKey;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TTL)}))
    private Long ttl;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_RECORD_CREATION_TIME)}))
    private Instant recordCreationTime;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_LAST_MODIFICATION_TIME)}))
    private Instant lastModificationTime;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ENTITY_NAME)}))
    private String entityName;
}
