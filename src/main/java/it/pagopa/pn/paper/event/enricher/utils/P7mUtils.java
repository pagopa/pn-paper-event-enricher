package it.pagopa.pn.paper.event.enricher.utils;

import org.bouncycastle.asn1.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class P7mUtils {

    public static Mono<InputStream> findSignedData(InputStream inStrm) {
        ASN1StreamParser ap = new ASN1StreamParser(inStrm);
        try {
            return Mono.just(Objects.requireNonNull(recursiveParse(ap.readObject())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static InputStream recursiveParse(Object obj) {
        if (obj instanceof ASN1SequenceParser seqParser) {
            return parseSequence(seqParser);
        } else if (obj instanceof ASN1TaggedObjectParser objParser) {
            try {
                Object child = objParser.parseExplicitBaseObject();
                return recursiveParse(child);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (obj instanceof ASN1OctetStringParser octetStringParser) {
            return octetStringParser.getOctetStream();
        }
        return null;
    }

    private static InputStream parseSequence(ASN1SequenceParser seqParser) {
        try {
            InputStream result = null;
            ASN1Encodable child;
            while ((child = seqParser.readObject()) != null && !(child instanceof DERNull)) {
                result = recursiveParse(child);
                if (result != null) {
                    break;
                }
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

