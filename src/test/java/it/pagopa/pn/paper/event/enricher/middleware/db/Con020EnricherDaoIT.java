package it.pagopa.pn.paper.event.enricher.middleware.db;

import it.pagopa.pn.paper.event.enricher.config.BaseTest;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntityMetadata;
import it.pagopa.pn.paper.event.enricher.model.UpdateTypeEnum;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

class Con020EnricherDaoIT extends BaseTest.WithLocalStack {

    @Autowired
    private Con020EnricherDao con020EnricherDao;

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
        con020EnrichedEntity.setArchiveFileKey("archiveFileKey");
        con020EnrichedEntity.setTtl(Instant.now().plus(365, ChronoUnit.DAYS).toEpochMilli());
        con020EnrichedEntity.setPrintedPdf("printedPdf");
        return con020EnrichedEntity;
    }

    @Test
    void updateMetadata_testOK() {
        CON020EnrichedEntity con020EnrichedEntity = createEnrichedEntityForMetadata(uuid, "sortKey");
        CON020EnrichedEntity updatedEntity = con020EnricherDao.update(con020EnrichedEntity, UpdateTypeEnum.METADATA).block();
        Assertions.assertNotNull(updatedEntity);
        Assertions.assertTrue(updatedEntity.getMetadataPresent());
    }

    @Test
    void updatePrintedPdf_testOK() {
        CON020EnrichedEntity con020EnrichedEntity = createEnrichedEntityForPrintedPdf(uuid, "sortKey");
        CON020EnrichedEntity updatedEntity = con020EnricherDao.update(con020EnrichedEntity, UpdateTypeEnum.PDF).block();
        Assertions.assertNotNull(updatedEntity);
    }

    @Test
    void updateSafeStorageEvent_testOK() {
        CON020EnrichedEntity con020EnrichedEntity = createEnrichedEntityForPrintedPdf(uuid, "sortKey");
        CON020EnrichedEntity updatedEntity = con020EnricherDao.update(con020EnrichedEntity, UpdateTypeEnum.SAFE_STORAGE).block();
        Assertions.assertNotNull(updatedEntity);
        Assertions.assertTrue(updatedEntity.getReceivedSafeStorageEvent());
    }

    @Test
    void retrieveEntitiesByArchiveFileKeyAndPrintedPdfReturnsEntityWhenFound() {
        String archiveFileKey = "validArchiveFileKey";
        String fileKey = "validFileKey";
        CON020EnrichedEntity entity = getEntity(archiveFileKey);
        entity.setHashKey("hashkey1");
        entity.setPrintedPdf("validFileKey");
        con020EnricherDao.update(entity, UpdateTypeEnum.PDF).block();

        entity.setHashKey("hashkey2");
        entity.setPrintedPdf("validFileKey2");
        con020EnricherDao.update(entity, UpdateTypeEnum.PDF).block();

        CON020EnrichedEntity result = con020EnricherDao.retrieveEntitiesByArchiveFileKeyAndPrintedPdf(archiveFileKey, fileKey).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("validFileKey", result.getPrintedPdf());
        Assertions.assertEquals("hashkey1", result.getHashKey());

    }

    @Test
    void retrieveEntitiesByArchiveFileKeyAndPrintedPdfThrowsErrorWhenNotFound() {
        String archiveFileKey = "validArchiveFileKey";
        String fileKey = "nonExistentFileKey";
        CON020EnrichedEntity entity = getEntity(archiveFileKey);
        entity.setHashKey("hashkey3");
        entity.setPrintedPdf("validFileKey");
        con020EnricherDao.update(entity, UpdateTypeEnum.PDF).block();

        Assertions.assertThrows(PaperEventEnricherException.class,
                () -> con020EnricherDao.retrieveEntitiesByArchiveFileKeyAndPrintedPdf(archiveFileKey, fileKey).block(),
                "No CON020EnrichedEntity found for ArchiveFileKey: [ validArchiveFileKey ] and FileKey: [ nonExistentFileKey ]");


    }

    @Test
    void retrieveEntitiesByArchiveFileKeyAndPrintedPdfHandlesPagination() {
        String archiveFileKey = "validArchiveFileKey";
        String fileKey = "validFileKey";
        CON020EnrichedEntity entity = getEntity(archiveFileKey);
        entity.setHashKey("hashkey4");
        entity.setPrintedPdf("invalidFileKey");
        con020EnricherDao.update(entity, UpdateTypeEnum.PDF).block();
        entity.setPrintedPdf("validFileKey");
        entity.setHashKey("hashkey5");
        con020EnricherDao.update(entity, UpdateTypeEnum.PDF).block();

        CON020EnrichedEntity result = con020EnricherDao.retrieveEntitiesByArchiveFileKeyAndPrintedPdf(archiveFileKey, fileKey).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("validFileKey", result.getPrintedPdf());
        Assertions.assertEquals("hashkey5", result.getHashKey());


    }

    private static CON020EnrichedEntity getEntity(String archiveFileKey) {
        CON020EnrichedEntity entity = new CON020EnrichedEntity();
        entity.setArchiveFileKey(archiveFileKey);
        entity.setSortKey("-");
        entity.setRecordCreationTime(Instant.now());
        entity.setTtl(10L);
        entity.setLastModificationTime(Instant.now());
        entity.setEntityName("entityName");
        entity.setPdfDocumentType("docType");
        entity.setPdfSha256("sha");
        entity.setPdfDate(Instant.now());
        return entity;
    }

}
