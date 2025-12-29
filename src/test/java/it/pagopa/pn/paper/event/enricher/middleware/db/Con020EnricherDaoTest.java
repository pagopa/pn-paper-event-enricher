package it.pagopa.pn.paper.event.enricher.middleware.db;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020ArchiveEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntityMetadata;
import it.pagopa.pn.paper.event.enricher.model.UpdateTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity.ARCHIVEFILEKEY_INDEX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Con020EnricherDaoTest {

    private Con020EnricherDaoImpl con020EnricherDao;
    @MockitoBean
    private DynamoDbAsyncClient dynamoDbAsyncClient;

    @MockitoBean
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    DynamoDbAsyncTable<CON020EnrichedEntity> tableAsync;


    @BeforeAll
    void setUp() {
        tableAsync = mock(DynamoDbAsyncTable.class);
        PnPaperEventEnricherConfig pnPaperEventEnricherConfig = new PnPaperEventEnricherConfig();
        PnPaperEventEnricherConfig.Dao dao = new PnPaperEventEnricherConfig.Dao();
        dao.setPaperEventEnrichmentTable("tableName");
        pnPaperEventEnricherConfig.setDao(dao);
        when(dynamoDbEnhancedAsyncClient.table(anyString(), (TableSchema<CON020EnrichedEntity>) any())).thenReturn(tableAsync);
        con020EnricherDao = new Con020EnricherDaoImpl(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, pnPaperEventEnricherConfig);
    }

    private final String uuid = UUID.randomUUID().toString();

    private static CON020EnrichedEntity createEnrichedEntityForMetadata(String hashKey, String sortKey) {
        CON020EnrichedEntity con020EnrichedEntity = new CON020EnrichedEntity();
        con020EnrichedEntity.setHashKey(hashKey);
        con020EnrichedEntity.setEntityName("entityName");
        con020EnrichedEntity.setSortKey(sortKey);
        con020EnrichedEntity.setLastModificationTime(Instant.now());
        con020EnrichedEntity.setRecordCreationTime(Instant.now());
        con020EnrichedEntity.setMetadataPresent(true);
        con020EnrichedEntity.setProductType("AR");
        con020EnrichedEntity.setStatusDescription("Affido conservato");
        con020EnrichedEntity.setArchiveFileKey("archiveFileKey");
        CON020EnrichedEntityMetadata metadata = new CON020EnrichedEntityMetadata();
        metadata.setIun("iun");
        metadata.setEventTime(Instant.now());
        metadata.setGenerationTime(Instant.now());
        metadata.setRecIndex(0);
        metadata.setSendRequestId("sendRequestId");
        con020EnrichedEntity.setTtl(Instant.now().plus(365, ChronoUnit.DAYS).toEpochMilli());
        metadata.setRegisteredLetterCode("registeredLetterCode");
        con020EnrichedEntity.setMetadata(metadata);

        return con020EnrichedEntity;
    }

    private static CON020EnrichedEntity createEnrichedEntityForPrintedPdf(String hashKey, String sortKey) {
        CON020EnrichedEntity con020EnrichedEntity = new CON020EnrichedEntity();
        con020EnrichedEntity.setHashKey(hashKey);
        con020EnrichedEntity.setPdfSha256("5HL0UugZeqdulYq9ld4Aj88mkfcteGKS8p/1RwDT7ek=");
        con020EnrichedEntity.setPdfDocumentType("PN_PRINTED");
        con020EnrichedEntity.setPdfDate(Instant.now());
        con020EnrichedEntity.setEntityName("entityName");
        con020EnrichedEntity.setSortKey(sortKey);
        con020EnrichedEntity.setLastModificationTime(Instant.now());
        con020EnrichedEntity.setRecordCreationTime(Instant.now());
        con020EnrichedEntity.setTtl(Instant.now().plus(365, ChronoUnit.DAYS).toEpochMilli());
        con020EnrichedEntity.setPrintedPdf("printedPdf");
        return con020EnrichedEntity;
    }

    @Test
    void updateMetadata_testOK() {
        CON020EnrichedEntity con020ArchiveEntity = createEnrichedEntityForMetadata(uuid, "sortKey");
        UpdateItemResponse updateItemResponse = mock(UpdateItemResponse.class);
        when(updateItemResponse.attributes()).thenReturn(CON020EnrichedEntity.paperTrackingsToAttributeValueMap(con020ArchiveEntity));
        CompletableFuture<UpdateItemResponse> completedFuture = CompletableFuture.completedFuture(updateItemResponse);
        when(dynamoDbAsyncClient.updateItem((UpdateItemRequest) any())).thenReturn(completedFuture);
        StepVerifier.create(con020EnricherDao.update(con020ArchiveEntity, UpdateTypeEnum.METADATA))
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();
    }

    @Test
    void updatePrintedPdf_testOK() {
        CON020EnrichedEntity con020ArchiveEntity = createEnrichedEntityForPrintedPdf(uuid, "sortKey");
        UpdateItemResponse updateItemResponse = mock(UpdateItemResponse.class);
        when(updateItemResponse.attributes()).thenReturn(CON020EnrichedEntity.paperTrackingsToAttributeValueMap(con020ArchiveEntity));
        CompletableFuture<UpdateItemResponse> completedFuture = CompletableFuture.completedFuture(updateItemResponse);
        when(dynamoDbAsyncClient.updateItem((UpdateItemRequest) any())).thenReturn(completedFuture);
        StepVerifier.create(con020EnricherDao.update(con020ArchiveEntity, UpdateTypeEnum.PDF))
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();

    }

    @Test
    void retrieveEntitiesByArchiveFileKeyAndPrintedPdfReturnsEntityWhenFound() {
        String archiveFileKey = "validArchiveFileKey";
        String fileKey = "validFileKey";
        CON020EnrichedEntity entity = new CON020EnrichedEntity();
        entity.setPrintedPdf("validFileKey");
        CON020EnrichedEntity entity2 = new CON020EnrichedEntity();
        entity2.setPrintedPdf("validFileKey2");

        SdkPublisher<Page<CON020EnrichedEntity>> publisher =
                subscriber -> Flux.just(
                        Page.create(
                                List.of(entity, entity2),
                                Map.of("LastEvaluatedKey", AttributeValue.builder().s("lastKey").build())
                        )
                ).subscribe(subscriber);

        DynamoDbAsyncIndex<CON020EnrichedEntity> indexMock = mock(DynamoDbAsyncIndex.class);

        Mockito.when(tableAsync.index(anyString())).thenReturn(indexMock);
        Mockito.when(indexMock.query(any(QueryEnhancedRequest.class)))
                .thenReturn(publisher);

        Mono<CON020EnrichedEntity> result = con020EnricherDao.retrieveEntitiesByArchiveFileKeyAndPrintedPdf(archiveFileKey, fileKey);

        StepVerifier.create(result)
                .expectNext(entity)
                .verifyComplete();
    }

    @Test
    void retrieveEntitiesByArchiveFileKeyAndPrintedPdfThrowsErrorWhenNotFound() {
        String archiveFileKey = "validArchiveFileKey";
        String fileKey = "nonExistentFileKey";
        CON020EnrichedEntity entity = new CON020EnrichedEntity();
        entity.setPrintedPdf("validFileKey");

        SdkPublisher<Page<CON020EnrichedEntity>> publisher =
                subscriber -> Flux.just(
                        Page.create(
                                List.of(entity),
                                null
                        )
                ).subscribe(subscriber);

        DynamoDbAsyncIndex<CON020EnrichedEntity> indexMock = mock(DynamoDbAsyncIndex.class);

        Mockito.when(tableAsync.index(anyString())).thenReturn(indexMock);
        Mockito.when(indexMock.query(any(QueryEnhancedRequest.class)))
                .thenReturn(publisher);

        Mono<CON020EnrichedEntity> result = con020EnricherDao.retrieveEntitiesByArchiveFileKeyAndPrintedPdf(archiveFileKey, fileKey);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof PaperEventEnricherException &&
                        throwable.getMessage().equals("No CON020EnrichedEntity found for ArchiveFileKey: [validArchiveFileKey] and FileKey: [nonExistentFileKey]"))
                .verify();
    }

    @Test
    void retrieveEntitiesByArchiveFileKeyAndPrintedPdfHandlesPagination() {
        String archiveFileKey = "validArchiveFileKey";
        String fileKey = "validFileKey";
        CON020EnrichedEntity entity = new CON020EnrichedEntity();
        entity.setPrintedPdf("invalidFileKey");
        CON020EnrichedEntity entity2 = new CON020EnrichedEntity();
        entity2.setPrintedPdf("validFileKey");

        SdkPublisher<Page<CON020EnrichedEntity>> publisher =
                subscriber -> Flux.just(
                        Page.create(
                                List.of(entity),
                                null
                        )
                ).subscribe(subscriber);

        DynamoDbAsyncIndex<CON020EnrichedEntity> indexMock = mock(DynamoDbAsyncIndex.class);

        Mockito.when(tableAsync.index(anyString())).thenReturn(indexMock);
        Mockito.when(indexMock.query(any(QueryEnhancedRequest.class)))
                .thenReturn(publisher);

        SdkPublisher<Page<CON020EnrichedEntity>> publisher2 =
                subscriber -> Flux.just(
                        Page.create(
                                List.of(entity2),
                                Map.of("LastEvaluatedKey", AttributeValue.builder().s("lastKey").build())
                        )
                ).subscribe(subscriber);

        Mockito.when(tableAsync.index(anyString())).thenReturn(indexMock);
        Mockito.when(indexMock.query(any(QueryEnhancedRequest.class)))
                .thenReturn(publisher2);

        Mono<CON020EnrichedEntity> result = con020EnricherDao.retrieveEntitiesByArchiveFileKeyAndPrintedPdf(archiveFileKey, fileKey);

        StepVerifier.create(result)
                .expectNext(entity2)
                .verifyComplete();
    }

}
