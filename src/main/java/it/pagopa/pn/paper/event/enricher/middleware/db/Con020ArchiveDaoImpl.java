package it.pagopa.pn.paper.event.enricher.middleware.db;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020ArchiveEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import static it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020BaseEntity.COL_HASH_KEY;

@Slf4j
@Repository
public class Con020ArchiveDaoImpl extends BaseDao<CON020ArchiveEntity> implements Con020ArchiveDao {

    protected Con020ArchiveDaoImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, PnPaperEventEnricherConfig config) {
        super(dynamoDbEnhancedAsyncClient, null, config.getDao().getPaperEventEnrichmentTable(), CON020ArchiveEntity.class);
    }

    @Override
    public Mono<CON020ArchiveEntity> putIfAbsent(CON020ArchiveEntity entity) {
        PutItemEnhancedRequest<CON020ArchiveEntity> putItemEnhancedRequest = PutItemEnhancedRequest
                .builder(CON020ArchiveEntity.class)
                .item(entity)
                .conditionExpression(Expression.builder().expression(buildExistingConditionExpression(false, COL_HASH_KEY)).build())
                .build();

        return putItem(putItemEnhancedRequest, entity.getArchiveFileKey());
    }

    @Override
    public Mono<CON020ArchiveEntity> updateIfExists(CON020ArchiveEntity entity) {
        UpdateItemEnhancedRequest.Builder<CON020ArchiveEntity> updateItemEnhancedRequest = UpdateItemEnhancedRequest
                .builder(CON020ArchiveEntity.class)
                .item(entity)
            //    .conditionExpression(Expression.builder().expression(buildExistingConditionExpression(true, COL_HASH_KEY)).build())
                .ignoreNulls(true);

        return updateItem(updateItemEnhancedRequest, entity.getArchiveFileKey());
    }
}
