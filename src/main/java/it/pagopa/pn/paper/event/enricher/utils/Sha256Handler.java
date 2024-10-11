package it.pagopa.pn.paper.event.enricher.utils;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.exception.PnPaperEventEnricherExceptionCode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Sha256Handler {
    private Sha256Handler() { }
    private static final String ALGORITHM = "SHA-256";

    public static String computeSha256(byte[] content) {
        try {
            byte[] hash = MessageDigest.getInstance(ALGORITHM)
                    .digest(content);
            return bytesToBase64(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new PaperEventEnricherException("Error while computing SHA-256 hash", 500, PnPaperEventEnricherExceptionCode.ERROR_MESSAGE_PAPER_EVENT_ENRICHER_ERRORCOMPUTECHECKSUM);
        }
    }

    private static String bytesToBase64(byte[] hash) { return Base64.getEncoder().encodeToString(hash); }

}
