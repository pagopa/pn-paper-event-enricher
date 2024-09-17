package it.pagopa.pn.template.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
@Setter
@ToString
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
    private Number ttl;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_RECORD_CREATION_TIME)}))
    private String recordCreationTime;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_LAST_MODIFICATION_TIME)}))
    private String lastModificationTime;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ENTITY_NAME)}))
    private String entityName;
}
