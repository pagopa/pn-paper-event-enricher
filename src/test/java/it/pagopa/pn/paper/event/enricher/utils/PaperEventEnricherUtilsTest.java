package it.pagopa.pn.paper.event.enricher.utils;

import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.model.IndexData;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static it.pagopa.pn.paper.event.enricher.constant.PaperEventEnricherConstant.*;
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
        assertEquals(DOCUMENT_TYPE, result.getPdfDocumentType());
        assertEquals(sha256, result.getPdfSha256());
        assertEquals(fileKey, result.getPrintedPdf());
        assertEquals(ENRICHED_ENTITY_NAME, result.getEntityName());
        assertEquals(SORT_KEY, result.getSortKey());
        assertFalse(result.getMetadataPresent());
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
}
