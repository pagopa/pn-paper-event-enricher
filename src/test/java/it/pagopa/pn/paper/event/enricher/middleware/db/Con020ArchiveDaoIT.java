package it.pagopa.pn.paper.event.enricher.middleware.db;

import it.pagopa.pn.paper.event.enricher.config.BaseTest;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020ArchiveEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application-test.properties")
class Con020ArchiveDaoIT extends BaseTest.WithLocalStack {

    @Autowired
    private Con020ArchiveDaoImpl con020ArchiveDao;


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

        con020ArchiveDao.putIfAbsent(con020ArchiveEntity).block();

        con020ArchiveEntity.setArchiveStatus("PROCESSING");

        Assertions.assertDoesNotThrow(() -> con020ArchiveDao.updateIfExists(con020ArchiveEntity).block());
    }


    @Test
    void putIfAbsentAndUpdateIfExist_updateKO_itemDoesNotExists() {
        String hashKey = UUID.randomUUID().toString();

        CON020ArchiveEntity con020ArchiveEntity = createArchiveEntity(hashKey);

        con020ArchiveDao.putIfAbsent(con020ArchiveEntity).block();

        con020ArchiveEntity.setArchiveStatus("PROCESSING");
        con020ArchiveEntity.setHashKey(UUID.randomUUID().toString());
        con020ArchiveEntity.setArchiveFileKey("test_ko");

        Executable executable = () -> con020ArchiveDao.updateIfExists(con020ArchiveEntity).block();

        Assertions.assertThrows(PaperEventEnricherException.class,
                executable,
                "Con020ArchiveDao doesn't exist for ArchiveFileKey: [" + con020ArchiveEntity.getArchiveFileKey() + "]");
    }


    @Test
    void putIfAbsentAndUpdateIfExist_putKO_itemAlreadyExists() {
        String hashKey = UUID.randomUUID().toString();
        CON020ArchiveEntity con020ArchiveEntity = createArchiveEntity(hashKey);

        con020ArchiveDao.putIfAbsent(con020ArchiveEntity).block();

        Executable executable = () -> con020ArchiveDao.putIfAbsent(con020ArchiveEntity).block();

        Assertions.assertThrows(PaperEventEnricherException.class,
                executable,
                "Con020ArchiveDao already exist for ArchiveFileKey: [" + con020ArchiveEntity.getArchiveFileKey() + "]");
    }
}
