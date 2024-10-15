package it.pagopa.pn.paper.event.enricher.utils;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

class PdfUtilsTest {


    static Stream<Arguments> pdfProvider() {
        return Stream.of(
                Arguments.of("src/test/resources/attachment_example_1.pdf", 1),
                Arguments.of("src/test/resources/attachment_example_2.pdf", 2),
                Arguments.of("src/test/resources/attachment_example_3.pdf", 2)
        );
    }

    @ParameterizedTest
    @MethodSource("pdfProvider")
    void cutPdf(String filePath, int expectedPages) throws IOException {
        Path path = Paths.get(filePath);
        byte[] fileContent = Files.readAllBytes(path);
        byte[] cuttedPdf = PdfUtils.cutPdf(fileContent, 2);
        try (PDDocument document = Loader.loadPDF(cuttedPdf)) {
            Assertions.assertEquals(expectedPages, document.getNumberOfPages());
        }
    }

    @Test
    void cutPdfError() {
        Assertions.assertThrows(PaperEventEnricherException.class,
                () -> PdfUtils.cutPdf(null, 2));
    }
}