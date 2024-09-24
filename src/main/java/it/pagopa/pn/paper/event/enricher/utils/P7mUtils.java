package it.pagopa.pn.paper.event.enricher.utils;

import org.bouncycastle.asn1.*;

import java.io.IOException;
import java.io.InputStream;

public class P7mUtils {

    public static InputStream findSignedData(InputStream inStrm ) {
        ASN1StreamParser ap = new ASN1StreamParser( inStrm );

        Object content = null;
        try {
            content = ap.readObject();
            return recursiveParse("",  content );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static InputStream recursiveParse( String prefix, Object obj) throws IOException {
        InputStream result = null;

        System.out.println(  prefix + ") " + ( obj == null ? "<null>" : obj.getClass()) + " [" + obj + "]" );

        if( obj instanceof ASN1SequenceParser) {
            ASN1SequenceParser seqParser = (ASN1SequenceParser) obj;

            int i = 0;
            Object child = new Object();
            while( ! ( child instanceof DERNull) && child != null && result == null) {
                child = seqParser.readObject();
                i += 1;
                String p = prefix + "." + i;


                result = recursiveParse( p, child);
            }
        }
        else if ( obj instanceof ASN1TaggedObjectParser) {
            ASN1TaggedObjectParser op = (ASN1TaggedObjectParser) obj;

            System.out.println(  prefix + ".0) Tag: " + op.getTagNo() );
            Object child = op.getObjectParser( op.getTagNo(), true);

            result = recursiveParse( prefix + ".1", child);
        }
        else if ( obj instanceof ASN1OctetStringParser) {
            ASN1OctetStringParser osp = (ASN1OctetStringParser) obj;
            InputStream inStrm = osp.getOctetStream();
            result = inStrm;
        }

        return result;
    }
}
