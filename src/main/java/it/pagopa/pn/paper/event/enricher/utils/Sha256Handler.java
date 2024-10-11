package it.pagopa.pn.paper.event.enricher.utils;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static it.pagopa.pn.paper.event.enricher.exception.PnPaperEventEnricherExceptionConstant.ERROR_WHILE_COMPUTING_SHA_256_HASH;

public class Sha256Handler {


    private Sha256Handler() { }
    private static final String ALGORITHM = "SHA-256";

    public static String computeSha256(byte[] content) {
        try {
            byte[] hash = MessageDigest.getInstance(ALGORITHM)
                    .digest(content);
            return bytesToBase64(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new PaperEventEnricherException(ERROR_WHILE_COMPUTING_SHA_256_HASH, 500, ERROR_WHILE_COMPUTING_SHA_256_HASH);
        }
    }

    private static String bytesToBase64(byte[] hash) { return Base64.getEncoder().encodeToString(hash); }

}
