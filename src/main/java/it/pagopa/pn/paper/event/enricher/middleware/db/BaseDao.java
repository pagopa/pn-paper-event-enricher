package it.pagopa.pn.paper.event.enricher.middleware.db;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Slf4j
public abstract class BaseDao<T> {
    private final DynamoDbAsyncTable<T> tableAsync;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final String tableName;

    protected BaseDao(
            DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
            DynamoDbAsyncClient dynamoDbAsyncClient,
            String tableName,
            Class<T> entityClass
    ) {
        this.tableAsync = dynamoDbEnhancedAsyncClient.table(tableName, TableSchema.fromBean(entityClass));
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.tableName = tableName;
    }

    protected Mono<T> updateItem(UpdateItemEnhancedRequest.Builder<T> request, String archiveFileKey) {
        return Mono.fromFuture(this.tableAsync.updateItem(request.build()))
                .onErrorResume(ConditionalCheckFailedException.class, e -> {
                    log.warn("Conditional check failed: {}", e.getMessage());
                    return Mono.error(new PaperEventEnricherException(String.format("Con020ArchiveDao doesn't exist for ArchiveFileKey: [{%s}]", archiveFileKey),
                            400, "ConditionalCheckFailedException"));
                })
                .then(Mono.empty());
    }

    protected Mono<T> updateItem(UpdateItemRequest.Builder updateItemRequestBuilder) {
        UpdateItemRequest updateItemRequest = updateItemRequestBuilder.tableName(tableName).build();
        return Mono.fromFuture(dynamoDbAsyncClient.updateItem(updateItemRequest))
                .then(Mono.empty());
    }

    protected Mono<T> putItem(PutItemEnhancedRequest<T> request, String archiveFileKey) {
        return Mono.fromFuture(tableAsync.putItem(request))
                .thenReturn(request.item())
                .onErrorResume(ConditionalCheckFailedException.class, e -> {
                    log.warn("Conditional check failed: {}", e.getMessage());
                    return Mono.error(new PaperEventEnricherException(String.format("Con020ArchiveDao entity already exists for ArchiveFileKey: [{%s}]", archiveFileKey),
                            400, "ConditionalCheckFailedException"));
                });
    }

    protected String buildExistingConditionExpression(boolean attributeHasToExist, String attribute) {
        if (attributeHasToExist) {
            return "attribute_exists(" + attribute + ")";
        }
        return "attribute_not_exists(" + attribute + ")";
    }



}

