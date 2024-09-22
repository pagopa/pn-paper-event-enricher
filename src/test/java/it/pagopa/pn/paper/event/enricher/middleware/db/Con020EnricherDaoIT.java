package it.pagopa.pn.paper.event.enricher.middleware.db;

import it.pagopa.pn.paper.event.enricher.config.BaseTest;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntityMetadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;


class Con020EnricherDaoIT extends BaseTest.WithLocalStack {

    @Autowired
    private Con020EnricherDao con020EnricherDao;

    private static CON020EnrichedEntity createEnrichedEntityForMetadata(String hashKey, String sortKey) {
        CON020EnrichedEntity con020EnrichedEntity = new CON020EnrichedEntity();
        con020EnrichedEntity.setHashKey(hashKey);
        con020EnrichedEntity.setEntityName("entityName");
        con020EnrichedEntity.setSortKey(sortKey);
        con020EnrichedEntity.setLastModificationTime(Instant.now());
        con020EnrichedEntity.setRecordCreationTime(Instant.now());
        con020EnrichedEntity.setMetadataPresent(true);
        CON020EnrichedEntityMetadata metadata = new CON020EnrichedEntityMetadata();
        metadata.setArchiveFileKey("archiveFileKey");
        metadata.setIun("iun");
        metadata.setEventTime(Instant.now());
        metadata.setGenerationDate(Instant.now());
        metadata.setRecIndex("recIndex");
        metadata.setSendRequestId("sendRequestId");
        con020EnrichedEntity.setTtl(Instant.now().plus(365, ChronoUnit.DAYS).toEpochMilli());
        metadata.setRegisteredLetterCode("registeredLetterCode");
        con020EnrichedEntity.setMetadata(metadata);

        return con020EnrichedEntity;
    }

    private static CON020EnrichedEntity createEnrichedEntityForPrintedPdf(String hashKey, String sortKey) {
        CON020EnrichedEntity con020EnrichedEntity = new CON020EnrichedEntity();
        con020EnrichedEntity.setHashKey(hashKey);
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
        CON020EnrichedEntity con020ArchiveEntity = createEnrichedEntityForMetadata("hashKey1", "sortKey");
        Assertions.assertDoesNotThrow(() -> con020EnricherDao.updateMetadata(con020ArchiveEntity)).block();
    }

    @Test
    void updatePrintedPdf_testOK() {
        CON020EnrichedEntity con020ArchiveEntity = createEnrichedEntityForPrintedPdf("hashKey1", "sortKey");
        Assertions.assertDoesNotThrow(() -> con020EnricherDao.updatePrintedPdf(con020ArchiveEntity)).block();

    }
}
