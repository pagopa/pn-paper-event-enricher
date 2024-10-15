package it.pagopa.pn.paper.event.enricher.utils;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class PdfUtilsTest {

    @Test
    void cutPdf1Page() throws IOException {
        Path path = Paths.get("src/test/resources/attachment_example_1.pdf");
        byte[] fileContent = Files.readAllBytes(path);
        byte[] cuttedPdf = PdfUtils.cutPdf(fileContent, 2);
        try(PDDocument document = Loader.loadPDF(cuttedPdf)){
            Assertions.assertEquals(1, document.getNumberOfPages());
        }
    }

    @Test
    void cutPdf2Page() throws IOException {
        Path path = Paths.get("src/test/resources/attachment_example_2.pdf");
        byte[] fileContent = Files.readAllBytes(path);
        byte[] cuttedPdf = PdfUtils.cutPdf(fileContent, 2);
        try(PDDocument document = Loader.loadPDF(cuttedPdf)){
            Assertions.assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    void cutPdf3Page() throws IOException {
        Path path = Paths.get("src/test/resources/attachment_example_3.pdf");
        byte[] fileContent = Files.readAllBytes(path);
        byte[] cuttedPdf = PdfUtils.cutPdf(fileContent, 2);
        try(PDDocument document = Loader.loadPDF(cuttedPdf)){
            Assertions.assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    void cutPdfError() {
        Assertions.assertThrows(PaperEventEnricherException.class,
                () -> PdfUtils.cutPdf(null, 2));
    }
}