package it.pagopa.pn.paper.event.enricher.utils;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.model.IndexData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileUtilsTest {


    @Test
    void getContentWithValidInputStream() {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());

        byte[] result = FileUtils.getContent(inputStream, "fileName");

        assertArrayEquals("test content".getBytes(), result);
    }

    @Test
    void getContentWithValidInputStreamError() {

        Assertions.assertThrows(PaperEventEnricherException.class, () -> FileUtils.getContent(null, "fileName"));
    }

    @Test
    void parseBolWithValidData() {
        byte[] bolBytes = "entry1.pdf|data|data|requestId1|data|data|registeredLetterCode1\nentry2.pdf|data|data|requestId2|data|data|registeredLetterCode2".getBytes();
        Map<String, IndexData> result = FileUtils.parseBol(bolBytes);

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
        Map<String, IndexData> result = FileUtils.parseBol(bolBytes);

        assertTrue(result.isEmpty());
    }

    @Test
    void parseBolWithNonPdfEntries() {
        byte[] bolBytes = "entry1.txt|data|data|requestId1|data|data|registeredLetterCode1\nentry2.doc|data|data|requestId2|data|data|registeredLetterCode2".getBytes();
        Map<String, IndexData> result = FileUtils.parseBol(bolBytes);

        assertTrue(result.isEmpty());
    }

    @Test
    void parseBolWithMixedEntries() {
        byte[] bolBytes = "entry1.pdf|data|data|requestId1|data|data|registeredLetterCode1\nentry2.txt|data|data|requestId2|data|data|registeredLetterCode2".getBytes();
        Map<String, IndexData> result = FileUtils.parseBol(bolBytes);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("entry1.pdf"));
        assertEquals("requestId1", result.get("entry1.pdf").getRequestId());
        assertEquals("registeredLetterCode1", result.get("entry1.pdf").getRegisteredLetterCode());
    }

    @Test
    void parseBolWithMixedEntriesError() {
        byte[] bolBytes = "entry1.pdf|data|data|".getBytes();
        Map<String, IndexData> result = FileUtils.parseBol(bolBytes);

        assertEquals(0, result.size());
    }
}
