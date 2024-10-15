package it.pagopa.pn.paper.event.enricher.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Sha256HandlerTest {

    @Test
    void computeSha256WithValidContent() {
        byte[] content = "test content".getBytes();
        String result = Sha256Handler.computeSha256(content);
        assertNotNull(result);
        assertEquals("auinVVUgn9bEQVfArtgBbnY/9DWhnPGG92hjFAFD/3I=", result);
    }

    @Test
    void computeSha256WithEmptyContent() {
        byte[] content = new byte[0];
        String result = Sha256Handler.computeSha256(content);
        assertNotNull(result);
        assertEquals("47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=", result);
    }
}