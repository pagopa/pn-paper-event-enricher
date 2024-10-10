package it.pagopa.pn.paper.event.enricher.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.Instant;

import static it.pagopa.pn.paper.event.enricher.constant.PaperEventEnricherConstant.ENRICHED_HASH_KEY_PREFIX;
import static it.pagopa.pn.paper.event.enricher.constant.PaperEventEnricherConstant.SAFE_STORAGE_PREFIX;

@DynamoDbBean
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class CON020EnrichedEntity extends CON020BaseEntity{
    public static final String COL_METADATA = "metadata";
    public static final String COL_METADATA_PRESENT = "metadataPresent";
    public static final String COL_PRINTED_PDF = "printedPdf";
    public static final String COL_PRODUCT_TYPE = "productType";
    public static final String COL_STATUS_DESCRIPTION = "statusDescription";
    public static final String COL_PDF_DOCUMENT_TYPE = "pdfDocumentType";
    public static final String COL_PDF_SHA256 = "pdfSha256";
    public static final String COL_PDF_DATE = "pdfDate";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_METADATA)}))
    private CON020EnrichedEntityMetadata metadata;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_METADATA_PRESENT)}))
    private Boolean metadataPresent;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PRINTED_PDF)}))
    private String printedPdf;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PRODUCT_TYPE)}))
    private String productType;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_DESCRIPTION)}))
    private String statusDescription;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PDF_DOCUMENT_TYPE)}))
    private String pdfDocumentType;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PDF_SHA256)}))
    private String pdfSha256;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PDF_DATE)}))
    private Instant pdfDate;

    public static String buildHashKeyForCon020EnrichedEntity(String archiveFileKey, String sendRequestId, String registeredLetterCode) {
        return ENRICHED_HASH_KEY_PREFIX + removePrefixFromArchiveFileKey(archiveFileKey) + "_" + sendRequestId + "_" + registeredLetterCode;
    }

    private static String removePrefixFromArchiveFileKey(String archiveFileKey) {
        return archiveFileKey.replace(SAFE_STORAGE_PREFIX, "");
    }
}
