package it.pagopa.pn.template.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class CON020ArchiveEntity extends CON020BaseEntity{
    public static final String COL_ARCHIVE_FILE_KEY = "archiveFileKey";
    public static final String COL_ARCHIVE_STATUS = "archiveStatus";
    public static final String COL_PROCESSING_TASK = "processingTask";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ARCHIVE_FILE_KEY)}))
    private String archiveFileKey;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ARCHIVE_STATUS)}))
    private String archiveStatus;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PROCESSING_TASK)}))
    private String processingTask;
}
