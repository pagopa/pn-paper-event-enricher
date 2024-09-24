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

        return updateItem(builder);
    }

    @Override
    public Mono<CON020EnrichedEntity> updatePrintedPdf(CON020EnrichedEntity entity) {
        UpdateItemRequest.Builder builder = UpdateItemRequest.builder()
                .key(buildDynamoKey(entity.getHashKey(), SORT_KEY))
                .updateExpression(constructUpdateExpression(PDF))
                .expressionAttributeNames(Map.of("#" + COL_TTL, COL_TTL))
                .expressionAttributeValues(constructexpressionAttributeValuesMap(entity, PDF));

        return updateItem(builder);
    }

    protected String constructUpdateExpression(UpdateTypeEnum updateType) {
        StringBuilder stringBuilder = new StringBuilder("SET ");
        stringBuilder.append(COL_RECORD_CREATION_TIME).append(QUERY_IF_NOT_EXISTS).append(COL_RECORD_CREATION_TIME).append(",:").append(COL_RECORD_CREATION_TIME).append("), ");
        stringBuilder.append("#" + COL_TTL).append(" = :").append(COL_TTL).append(", ");
        stringBuilder.append(COL_LAST_MODIFICATION_TIME).append(" = :").append(COL_LAST_MODIFICATION_TIME).append(", ");
        stringBuilder.append(COL_ENTITY_NAME).append(" = :").append(COL_ENTITY_NAME).append(", ");
        if (PDF.equals(updateType)) {
            stringBuilder.append(COL_METADATA_PRESENT).append(QUERY_IF_NOT_EXISTS).append(COL_METADATA_PRESENT).append(",:").append(COL_METADATA_PRESENT).append("), ");
            stringBuilder.append(COL_PRINTED_PDF).append(" = :").append(COL_PRINTED_PDF);
        } else if (METADATA.equals(updateType)) {
            stringBuilder.append(COL_METADATA_PRESENT).append(" = :").append(COL_METADATA_PRESENT).append(", ");
            stringBuilder.append(COL_METADATA).append(" = :").append(COL_METADATA);
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
            attributeValueMap.put(":" + COL_METADATA_PRESENT, AttributeValue.builder().bool(false).build());
            attributeValueMap.put(":" + COL_PRINTED_PDF, AttributeValue.builder().s(entity.getPrintedPdf()).build());
        } else if (METADATA.equals(updateType)) {
            attributeValueMap.put(":" + COL_METADATA_PRESENT, AttributeValue.builder().bool(true).build());
            attributeValueMap.put(":" + COL_METADATA, AttributeValue.builder().m(constructMetadataAttributeValuesMap(entity.getMetadata())).build());
        }

        return attributeValueMap;
    }

    private Map<String, AttributeValue> constructMetadataAttributeValuesMap(CON020EnrichedEntityMetadata metadata) {
        Map<String, AttributeValue> metadataMap = new HashMap<>();
        metadataMap.put(COL_IUN, AttributeValue.builder().s(metadata.getIun()).build());
        metadataMap.put(COL_GENERATIONDATE, AttributeValue.builder().s(metadata.getGenerationDate().toString()).build());
        metadataMap.put(COL_RECINDEX, AttributeValue.builder().s(metadata.getRecIndex()).build());
        metadataMap.put(COL_SENDREQUESTID, AttributeValue.builder().s(metadata.getSendRequestId()).build());
        metadataMap.put(COL_REGISTEREDLETTERCORE, AttributeValue.builder().s(metadata.getRegisteredLetterCode()).build());
        metadataMap.put(COL_EVENTTIME, AttributeValue.builder().s(metadata.getEventTime().toString()).build());
        metadataMap.put(COL_ARCHIVEFILEKEY, AttributeValue.builder().s(metadata.getArchiveFileKey()).build());
        return metadataMap;

    }
}

