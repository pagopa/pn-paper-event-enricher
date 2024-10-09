package it.pagopa.pn.paper.event.enricher.middleware.db;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020ArchiveEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Con020ArchiveDaoTest {

    private Con020ArchiveDaoImpl con020ArchiveDao;
    @MockBean
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    DynamoDbAsyncTable<CON020ArchiveEntity> tableAsync;


    @BeforeAll
    void setUp() {
        tableAsync = mock(DynamoDbAsyncTable.class);
        PnPaperEventEnricherConfig pnPaperEventEnricherConfig = new PnPaperEventEnricherConfig();
        PnPaperEventEnricherConfig.Dao dao = new PnPaperEventEnricherConfig.Dao();
        dao.setPaperEventEnrichmentTable("tableName");
        pnPaperEventEnricherConfig.setDao(dao);
        when(dynamoDbEnhancedAsyncClient.table(anyString(), (TableSchema<CON020ArchiveEntity>) any())).thenReturn(tableAsync);
        con020ArchiveDao = new Con020ArchiveDaoImpl(dynamoDbEnhancedAsyncClient, pnPaperEventEnricherConfig);
    }

    private static CON020ArchiveEntity createArchiveEntity(String hashKey) {
        CON020ArchiveEntity con020ArchiveEntity = new CON020ArchiveEntity();
        con020ArchiveEntity.setHashKey(hashKey);
        con020ArchiveEntity.setArchiveStatus("NEW");
        con020ArchiveEntity.setArchiveFileKey("archiveFileKey");
        con020ArchiveEntity.setEntityName("entityName");
        con020ArchiveEntity.setSortKey("sortKey");
        con020ArchiveEntity.setProcessingTask("processingTask");
        con020ArchiveEntity.setLastModificationTime(Instant.now());
        con020ArchiveEntity.setRecordCreationTime(Instant.now());
        con020ArchiveEntity.setTtl(123L);

        return con020ArchiveEntity;
    }

    @Test
    void putIfAbsentAndUpdateIfExist_testOK() {
        String hashKey = UUID.randomUUID().toString();

        CON020ArchiveEntity con020ArchiveEntity = createArchiveEntity(hashKey);
        CompletableFuture<Void> completedFuture = CompletableFuture.completedFuture(null);
        CompletableFuture<CON020ArchiveEntity> completedFutureEntity = CompletableFuture.completedFuture(con020ArchiveEntity);
        when(tableAsync.putItem((PutItemEnhancedRequest<CON020ArchiveEntity>) any())).thenReturn(completedFuture);
        when(tableAsync.updateItem((UpdateItemEnhancedRequest<CON020ArchiveEntity>) any())).thenReturn(completedFutureEntity);
        con020ArchiveDao.putIfAbsent(con020ArchiveEntity).block();

        con020ArchiveEntity.setArchiveStatus("PROCESSING");

        Assertions.assertDoesNotThrow(() -> con020ArchiveDao.updateIfExists(con020ArchiveEntity).block());
    }


    @Test
    void putIfAbsentAndUpdateIfExist_updateKO_itemDoesNotExists() {
        String hashKey = UUID.randomUUID().toString();

        CON020ArchiveEntity con020ArchiveEntity = createArchiveEntity(hashKey);

        con020ArchiveEntity.setArchiveStatus("PROCESSING");
        con020ArchiveEntity.setHashKey(UUID.randomUUID().toString());
        con020ArchiveEntity.setArchiveFileKey("test_ko");

        CompletableFuture<CON020ArchiveEntity> completedFuture = CompletableFuture.failedFuture(ConditionalCheckFailedException.builder().build());

        when(tableAsync.updateItem((UpdateItemEnhancedRequest<CON020ArchiveEntity>) any()))
                .thenReturn(completedFuture);

        StepVerifier.create(con020ArchiveDao.updateIfExists(con020ArchiveEntity))
                .expectErrorMatches(throwable -> throwable instanceof PaperEventEnricherException
                        && Objects.equals(throwable.getMessage(), "Con020ArchiveDao doesn't exist for ArchiveFileKey: [{test_ko}]"))
                .verify();
    }


    @Test
    void putIfAbsentAndUpdateIfExist_putKO_itemAlreadyExists() {
        String hashKey = UUID.randomUUID().toString();
        CON020ArchiveEntity con020ArchiveEntity = createArchiveEntity(hashKey);

        CompletableFuture<Void> completedFuture = CompletableFuture.failedFuture(ConditionalCheckFailedException.builder().build());

        when(tableAsync.putItem((PutItemEnhancedRequest<CON020ArchiveEntity>) any()))
                .thenReturn(completedFuture);

        StepVerifier.create(con020ArchiveDao.putIfAbsent(con020ArchiveEntity))
                .expectErrorMatches(throwable -> throwable instanceof PaperEventEnricherException
                        && Objects.equals(throwable.getMessage(), "Con020ArchiveDao already exist for ArchiveFileKey: [" + con020ArchiveEntity.getArchiveFileKey() + "]"));
    }
}
