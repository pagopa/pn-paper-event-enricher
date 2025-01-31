package it.pagopa.pn.paper.event.enricher.middleware.db;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntityMetadata;
import it.pagopa.pn.paper.event.enricher.model.UpdateTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.paper.event.enricher.constant.PaperEventEnricherConstant.SORT_KEY;
import static it.pagopa.pn.paper.event.enricher.model.UpdateTypeEnum.METADATA;
import static it.pagopa.pn.paper.event.enricher.model.UpdateTypeEnum.PDF;
import static it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020BaseEntity.*;
import static it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity.*;
import static it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntityMetadata.*;

@Slf4j
@Repository
public class Con020EnricherDaoImpl extends BaseDao<CON020EnrichedEntity> implements Con020EnricherDao {

    public Con020EnricherDaoImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, DynamoDbAsyncClient dynamoDbAsyncClient, PnPaperEventEnricherConfig pnPaperEventEnricherConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, pnPaperEventEnricherConfig.getDao().getPaperEventEnrichmentTable(), CON020EnrichedEntity.class);
    }

    public static final String QUERY_IF_NOT_EXISTS = " = if_not_exists(";

    @Override
    public Mono<CON020EnrichedEntity> updateMetadata(CON020EnrichedEntity entity) {
        UpdateItemRequest.Builder builder = UpdateItemRequest.builder()
                .key(buildDynamoKey(entity.getHashKey(), SORT_KEY))
                .updateExpression(constructUpdateExpression(UpdateTypeEnum.METADATA))
                .expressionAttributeNames(Map.of("#" + COL_TTL, COL_TTL))
                .expressionAttributeValues(constructexpressionAttributeValuesMap(entity, UpdateTypeEnum.METADATA));

        return updateItem(builder).thenReturn(entity);
    }

    @Override
    public Mono<CON020EnrichedEntity> updatePrintedPdf(CON020EnrichedEntity entity) {
        UpdateItemRequest.Builder builder = UpdateItemRequest.builder()
                .key(buildDynamoKey(entity.getHashKey(), SORT_KEY))
                .updateExpression(constructUpdateExpression(PDF))
                .expressionAttributeNames(Map.of("#" + COL_TTL, COL_TTL))
                .expressionAttributeValues(constructexpressionAttributeValuesMap(entity, PDF));

        return updateItem(builder).thenReturn(entity);
    }

    protected String constructUpdateExpression(UpdateTypeEnum updateType) {
        StringBuilder stringBuilder = new StringBuilder("SET ");
        stringBuilder.append(COL_RECORD_CREATION_TIME).append(QUERY_IF_NOT_EXISTS).append(COL_RECORD_CREATION_TIME).append(",:").append(COL_RECORD_CREATION_TIME).append("), ");
        stringBuilder.append("#" + COL_TTL).append(" = :").append(COL_TTL).append(", ");
        stringBuilder.append(COL_LAST_MODIFICATION_TIME).append(" = :").append(COL_LAST_MODIFICATION_TIME).append(", ");
        stringBuilder.append(COL_ENTITY_NAME).append(" = :").append(COL_ENTITY_NAME).append(", ");
        if (PDF.equals(updateType)) {
            stringBuilder.append(COL_METADATA_PRESENT).append(QUERY_IF_NOT_EXISTS).append(COL_METADATA_PRESENT).append(",:").append(COL_METADATA_PRESENT).append("), ");
            stringBuilder.append(COL_ARCHIVEFILEKEY).append(QUERY_IF_NOT_EXISTS).append(COL_ARCHIVEFILEKEY).append(",:").append(COL_ARCHIVEFILEKEY).append("), ");
            stringBuilder.append(COL_PRINTED_PDF).append(" = :").append(COL_PRINTED_PDF).append(", ");
            stringBuilder.append(COL_PDF_DATE).append(" = :").append(COL_PDF_DATE).append(", ");
            stringBuilder.append(COL_PDF_SHA256).append(" = :").append(COL_PDF_SHA256).append(", ");
            stringBuilder.append(COL_PDF_DOCUMENT_TYPE).append(" = :").append(COL_PDF_DOCUMENT_TYPE);
        } else if (METADATA.equals(updateType)) {
            stringBuilder.append(COL_METADATA_PRESENT).append(" = :").append(COL_METADATA_PRESENT).append(", ");
            stringBuilder.append(COL_ARCHIVEFILEKEY).append(QUERY_IF_NOT_EXISTS).append(COL_ARCHIVEFILEKEY).append(",:").append(COL_ARCHIVEFILEKEY).append("), ");
            stringBuilder.append(COL_METADATA).append(" = :").append(COL_METADATA).append(", ");
            stringBuilder.append(COL_STATUS_DESCRIPTION).append(" = :").append(COL_STATUS_DESCRIPTION).append(", ");
            stringBuilder.append(COL_PRODUCT_TYPE).append(" = :").append(COL_PRODUCT_TYPE);

        }
        return stringBuilder.toString();
    }

    protected Map<String, AttributeValue> buildDynamoKey(String hashKey, String sortKey) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(COL_HASH_KEY, AttributeValue.builder().s(hashKey).build());
        key.put(COL_SORT_KEY, AttributeValue.builder().s(sortKey).build());
        return key;
    }

    public Map<String, AttributeValue> constructexpressionAttributeValuesMap(CON020EnrichedEntity entity, UpdateTypeEnum updateType) {
        Map<String, AttributeValue> attributeValueMap = new HashMap<>();

        attributeValueMap.put(":" + COL_TTL, AttributeValue.builder().n(entity.getTtl().toString()).build());
        attributeValueMap.put(":" + COL_RECORD_CREATION_TIME, AttributeValue.builder().s(entity.getRecordCreationTime().toString()).build());
        attributeValueMap.put(":" + COL_LAST_MODIFICATION_TIME, AttributeValue.builder().s(entity.getLastModificationTime().toString()).build());
        attributeValueMap.put(":" + COL_ENTITY_NAME, AttributeValue.builder().s(entity.getEntityName()).build());

        if (PDF.equals(updateType)) {
            attributeValueMap.put(":" + COL_ARCHIVEFILEKEY, AttributeValue.builder().s(entity.getArchiveFileKey()).build());
            attributeValueMap.put(":" + COL_METADATA_PRESENT, AttributeValue.builder().bool(false).build());
            attributeValueMap.put(":" + COL_PRINTED_PDF, AttributeValue.builder().s(entity.getPrintedPdf()).build());
            attributeValueMap.put(":" + COL_PDF_DATE, AttributeValue.builder().s(entity.getPdfDate().toString()).build());
            attributeValueMap.put(":" + COL_PDF_SHA256, AttributeValue.builder().s(entity.getPdfSha256()).build());
            attributeValueMap.put(":" + COL_PDF_DOCUMENT_TYPE, AttributeValue.builder().s(entity.getPdfDocumentType()).build());
        } else if (METADATA.equals(updateType)) {
            attributeValueMap.put(":" + COL_ARCHIVEFILEKEY, AttributeValue.builder().s(entity.getArchiveFileKey()).build());
            attributeValueMap.put(":" + COL_METADATA_PRESENT, AttributeValue.builder().bool(true).build());
            attributeValueMap.put(":" + COL_METADATA, AttributeValue.builder().m(constructMetadataAttributeValuesMap(entity.getMetadata())).build());
            attributeValueMap.put(":" + COL_STATUS_DESCRIPTION, AttributeValue.builder().s(entity.getStatusDescription()).build());
            attributeValueMap.put(":" + COL_PRODUCT_TYPE, AttributeValue.builder().s(entity.getProductType()).build());
        }

        return attributeValueMap;
    }

    private Map<String, AttributeValue> constructMetadataAttributeValuesMap(CON020EnrichedEntityMetadata metadata) {
        Map<String, AttributeValue> metadataMap = new HashMap<>();
        if(StringUtils.isNotBlank(metadata.getIun())){
            metadataMap.put(COL_IUN, AttributeValue.builder().s(metadata.getIun()).build());
        }
        if(metadata.getRecIndex() != null) {
            metadataMap.put(COL_RECINDEX, AttributeValue.builder().n(metadata.getRecIndex().toString()).build());
        }
        metadataMap.put(COL_GENERATIONTIME, AttributeValue.builder().s(metadata.getGenerationTime().toString()).build());
        metadataMap.put(COL_SENDREQUESTID, AttributeValue.builder().s(metadata.getSendRequestId()).build());
        metadataMap.put(COL_REGISTEREDLETTERCORE, AttributeValue.builder().s(metadata.getRegisteredLetterCode()).build());
        metadataMap.put(COL_EVENTTIME, AttributeValue.builder().s(metadata.getEventTime().toString()).build());
        return metadataMap;

    }
}

