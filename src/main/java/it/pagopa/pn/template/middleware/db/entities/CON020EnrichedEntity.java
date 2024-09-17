package it.pagopa.pn.template.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.Map;

@DynamoDbBean
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class CON020EnrichedEntity extends CON020BaseEntity{
    public static final String COL_METADATA = "metadata";
    public static final String COL_METADATA_PRESENT = "metadataPresent";
    public static final String COL_PRINTED_PDF = "printedPdf";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_METADATA)}))
    private Map<String,String> metadata;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_METADATA_PRESENT)}))
    private boolean metadataPresent;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PRINTED_PDF)}))
    private String printedPdf;
}
