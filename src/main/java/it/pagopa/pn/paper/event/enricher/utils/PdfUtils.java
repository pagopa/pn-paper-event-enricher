package it.pagopa.pn.paper.event.enricher.utils;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.util.List;

import static it.pagopa.pn.paper.event.enricher.exception.PnPaperEventEnricherExceptionConstant.FAILED_TO_CUT_PDF_FILE;


@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PdfUtils {



    public static byte[] cutPdf(byte[] pdf, int pageNumbers) {
        try (PDDocument document = Loader.loadPDF(pdf); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if (document.getNumberOfPages() <= pageNumbers) {
                return pdf;
            }
            Splitter splitter = new Splitter();
            splitter.setEndPage(pageNumbers);
            splitter.setSplitAtPage(pageNumbers);
            List<PDDocument> documents = splitter.split(document);
            PDDocument firstDoc = documents.stream().findFirst().orElseThrow(() ->
                    new PaperEventEnricherException("Error saving cutted pdf.", 500, FAILED_TO_CUT_PDF_FILE));
            firstDoc.save(baos);
            for (PDDocument doc : documents) {
                doc.close();
            }
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error in cutting pdfs.", e);
            throw new PaperEventEnricherException("Error in cutting pdfs.", 500, FAILED_TO_CUT_PDF_FILE);
        }
    }
}
