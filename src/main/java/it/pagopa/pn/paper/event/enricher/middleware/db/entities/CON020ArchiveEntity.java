package it.pagopa.pn.paper.event.enricher.middleware.db.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import static it.pagopa.pn.paper.event.enricher.constant.PaperEventEnricherConstant.ARCHIVE_HASH_KEY_PREFIX;
import static it.pagopa.pn.paper.event.enricher.constant.PaperEventEnricherConstant.SAFE_STORAGE_PREFIX;

@DynamoDbBean
@Setter
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class CON020ArchiveEntity extends CON020BaseEntity {
    public static final String COL_ARCHIVE_FILE_KEY = "archiveFileKey";
    public static final String COL_ARCHIVE_STATUS = "archiveStatus";
    public static final String COL_PROCESSING_TASK = "processingTask";
    public static final String COL_FILE_NUMBER = "fileNumber";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ARCHIVE_FILE_KEY)}))
    private String archiveFileKey;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ARCHIVE_STATUS)}))
    private String archiveStatus;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PROCESSING_TASK)}))
    private String processingTask;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FILE_NUMBER)}))
    private Integer fileNumber;

    public static String buildHashKeyForCon020ArchiveEntity(String archiveFileKey) {
        return ARCHIVE_HASH_KEY_PREFIX + removePrefixFromArchiveFileKey(archiveFileKey);
    }

    private static String removePrefixFromArchiveFileKey(String archiveFileKey) {
        return archiveFileKey.replace(SAFE_STORAGE_PREFIX, "");
    }
}
