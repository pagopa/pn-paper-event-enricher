package it.pagopa.pn.paper.event.enricher.utils;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020ArchiveEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperArchiveEvent;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperEventEnricherInputEvent;
import it.pagopa.pn.paper.event.enricher.model.FileCounter;
import it.pagopa.pn.paper.event.enricher.model.IndexData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static it.pagopa.pn.paper.event.enricher.constant.PaperEventEnricherConstant.*;
import static it.pagopa.pn.paper.event.enricher.model.CON020ArchiveStatusEnum.PROCESSING;
import static org.junit.jupiter.api.Assertions.*;

class PaperEventEnricherUtilsTest {

    @Test
    void createEnricherEntityForPrintedPdfWithValidInputs() {
        String fileKey = "fileKey";
        String archiveFileKey = "archiveFileKey";
        String requestId = "requestId";
        String registeredLetterCode = "registeredLetterCode";
        String sha256 = "sha256";

        CON020EnrichedEntity result = PaperEventEnricherUtils.createEnricherEntityForPrintedPdf(fileKey, sha256, archiveFileKey, requestId, registeredLetterCode);

        assertNotNull(result);
        assertEquals(CON020EnrichedEntity.buildHashKeyForCon020EnrichedEntity(archiveFileKey, requestId, registeredLetterCode), result.getHashKey());
        assertEquals(PDF_DOCUMENT_TYPE, result.getPdfDocumentType());
        assertEquals(sha256, result.getPdfSha256());
        assertEquals(SAFE_STORAGE_PREFIX + fileKey, result.getPrintedPdf());
        assertEquals(ENRICHED_ENTITY_NAME, result.getEntityName());
        assertEquals(SORT_KEY, result.getSortKey());
        assertFalse(result.getMetadataPresent());
    }

    @Test
    void createEnricherEntityForMetadataWithValidInputsWithoutIunAndRecIndex() {
        PaperEventEnricherInputEvent.Payload.AnalogMailDetail analogMailDetail = PaperEventEnricherInputEvent.Payload.AnalogMailDetail
                .builder()
                .registeredLetterCode("registeredLetterCode")
                .requestId("requestId")
                .attachments(List.of(PaperEventEnricherInputEvent.Payload.Attachment.builder().uri("uri").build())).build();

        PaperEventEnricherInputEvent.Payload payload = PaperEventEnricherInputEvent.Payload.builder()
                .analogMail(analogMailDetail).build();

        CON020EnrichedEntity result = PaperEventEnricherUtils.createEnricherEntityForMetadata(payload);

        assertNotNull(result);
        assertEquals(CON020EnrichedEntity.buildHashKeyForCon020EnrichedEntity("uri", "requestId", "registeredLetterCode"), result.getHashKey());
        assertEquals(ENRICHED_ENTITY_NAME, result.getEntityName());
        assertEquals(SORT_KEY, result.getSortKey());
        assertTrue(result.getMetadataPresent());
    }

    @Test
    void createEnricherEntityForMetadataWithValidInputs() {
        PaperEventEnricherInputEvent.Payload.AnalogMailDetail analogMailDetail = PaperEventEnricherInputEvent.Payload.AnalogMailDetail
                .builder()
                .registeredLetterCode("registeredLetterCode")
                .requestId("requestId.IUN_01.RECINDEX_01.TEST")
                .attachments(List.of(PaperEventEnricherInputEvent.Payload.Attachment.builder().uri("uri").build())).build();

        PaperEventEnricherInputEvent.Payload payload = PaperEventEnricherInputEvent.Payload.builder()
                .analogMail(analogMailDetail).build();

        CON020EnrichedEntity result = PaperEventEnricherUtils.createEnricherEntityForMetadata(payload);

        assertNotNull(result);
        assertEquals(CON020EnrichedEntity.buildHashKeyForCon020EnrichedEntity("uri", "requestId.IUN_01.RECINDEX_01.TEST", "registeredLetterCode"), result.getHashKey());
        assertEquals(ENRICHED_ENTITY_NAME, result.getEntityName());
        assertEquals(SORT_KEY, result.getSortKey());
        assertTrue(result.getMetadataPresent());
    }

    @Test
    void parseBolWithValidData() {
        byte[] bolBytes = "entry1.pdf|data|data|requestId1|data|data|registeredLetterCode1\nentry2.pdf|data|data|requestId2|data|data|registeredLetterCode2".getBytes();
        Map<String, IndexData> result = PaperEventEnricherUtils.parseBol(bolBytes);

        assertEquals(2, result.size());
        assertTrue(result.containsKey("entry1.pdf"));
        assertTrue(result.containsKey("entry2.pdf"));
        assertEquals("requestId1", result.get("entry1.pdf").getRequestId());
        assertEquals("registeredLetterCode1", result.get("entry1.pdf").getRegisteredLetterCode());
        assertEquals("requestId2", result.get("entry2.pdf").getRequestId());
        assertEquals("registeredLetterCode2", result.get("entry2.pdf").getRegisteredLetterCode());
    }

    @Test
    void parseBolWithEmptyData() {
        byte[] bolBytes = "".getBytes();
        Map<String, IndexData> result = PaperEventEnricherUtils.parseBol(bolBytes);

        assertTrue(result.isEmpty());
    }

    @Test
    void parseBolWithNonPdfEntries() {
        byte[] bolBytes = "entry1.txt|data|data|requestId1|data|data|registeredLetterCode1\nentry2.doc|data|data|requestId2|data|data|registeredLetterCode2".getBytes();
        Map<String, IndexData> result = PaperEventEnricherUtils.parseBol(bolBytes);

        assertTrue(result.isEmpty());
    }

    @Test
    void parseBolWithMixedEntries() {
        byte[] bolBytes = "entry1.pdf|data|data|requestId1|data|data|registeredLetterCode1\nentry2.txt|data|data|requestId2|data|data|registeredLetterCode2".getBytes();
        Map<String, IndexData> result = PaperEventEnricherUtils.parseBol(bolBytes);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("entry1.pdf"));
        assertEquals("requestId1", result.get("entry1.pdf").getRequestId());
        assertEquals("registeredLetterCode1", result.get("entry1.pdf").getRegisteredLetterCode());
    }

    @Test
    void parseBolWithMixedEntriesError() {
        byte[] bolBytes = "entry1.pdf|data|data|".getBytes();
        Map<String, IndexData> result = PaperEventEnricherUtils.parseBol(bolBytes);

        assertEquals(0, result.size());
    }

    @Test
    void createArchiveEntityForStatusUpdate() {
        PaperArchiveEvent.Payload payload = PaperArchiveEvent.Payload.builder().archiveFileKey("test").build();
        FileCounter fileCounter = new FileCounter(new AtomicInteger(0), new AtomicInteger(0), 1);

        CON020ArchiveEntity result = PaperEventEnricherUtils.createArchiveEntityForStatusUpdate(payload, PROCESSING.name(), fileCounter);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("CON020AR~test", result.getHashKey());
    }

    @Test
    void createArchiveEntityWithValidInputs() {
        PaperEventEnricherInputEvent.Payload.AnalogMailDetail analogMailDetail = PaperEventEnricherInputEvent.Payload.AnalogMailDetail
                .builder().attachments(List.of(PaperEventEnricherInputEvent.Payload.Attachment.builder().uri("uri").build())).build();

        CON020ArchiveEntity result = PaperEventEnricherUtils.createArchiveEntity(analogMailDetail);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("uri", result.getArchiveFileKey());
    }

    @Test
    void createEnricherEntityForMetadataWithValidInputsWithNullAnalogMailDetail() {
        Assertions.assertThrows(PaperEventEnricherException.class, () -> PaperEventEnricherUtils.createArchiveEntity(null));
    }

    @Test
    void createEnricherEntityForMetadataWithValidInputsWithNullUri() {
        PaperEventEnricherInputEvent.Payload.AnalogMailDetail analogMailDetail = PaperEventEnricherInputEvent.Payload.AnalogMailDetail
                .builder()
                .registeredLetterCode("registeredLetterCode")
                .requestId("requestId")
                .attachments(List.of(PaperEventEnricherInputEvent.Payload.Attachment.builder().build())).build();

        Assertions.assertThrows(PaperEventEnricherException.class, () -> PaperEventEnricherUtils.createArchiveEntity(analogMailDetail));

    }

    @Test
    void createArchiveEntityWithoutAttachment() {
        PaperEventEnricherInputEvent.Payload.AnalogMailDetail analogMailDetail = PaperEventEnricherInputEvent.Payload.AnalogMailDetail
                .builder().attachments(Collections.emptyList()).build();

        Assertions.assertThrows(PaperEventEnricherException.class,
                () -> PaperEventEnricherUtils.createArchiveEntity(analogMailDetail),
                "Archive attachment uri not found.");
    }

    @Test
    void getContentWithValidInputStream() {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());

        byte[] result = PaperEventEnricherUtils.getContent(inputStream, "fileName");

        assertArrayEquals("test content".getBytes(), result);
    }

    @Test
    void getContentWithValidInputStreamError() {

        Assertions.assertThrows(PaperEventEnricherException.class, () -> PaperEventEnricherUtils.getContent(null, "fileName"));
    }
}
