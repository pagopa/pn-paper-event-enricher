package it.pagopa.pn.paper.event.enricher.middleware.db;

import it.pagopa.pn.paper.event.enricher.config.BaseTest;
import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020ArchiveEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntityMetadata;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Con020EnricherDaoTest {

    private Con020EnricherDaoImpl con020EnricherDao;
    @MockBean
    private DynamoDbAsyncClient dynamoDbAsyncClient;

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
        CON020EnrichedEntityMetadata metadata = new CON020EnrichedEntityMetadata();
        metadata.setArchiveFileKey("archiveFileKey");
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
        CompletableFuture<UpdateItemResponse> completedFuture = CompletableFuture.completedFuture(UpdateItemResponse.builder().build());
        when(dynamoDbAsyncClient.updateItem((UpdateItemRequest) any())).thenReturn(completedFuture);
        StepVerifier.create(con020EnricherDao.updateMetadata(con020ArchiveEntity))
                .expectNext(con020ArchiveEntity)
                .verifyComplete();
    }

    @Test
    void updatePrintedPdf_testOK() {
        CON020EnrichedEntity con020ArchiveEntity = createEnrichedEntityForPrintedPdf(uuid, "sortKey");
        CompletableFuture<UpdateItemResponse> completedFuture = CompletableFuture.completedFuture(UpdateItemResponse.builder().build());
        when(dynamoDbAsyncClient.updateItem((UpdateItemRequest) any())).thenReturn(completedFuture);
        StepVerifier.create(con020EnricherDao.updatePrintedPdf(con020ArchiveEntity))
                .expectNext(con020ArchiveEntity)
                .verifyComplete();

    }
}
