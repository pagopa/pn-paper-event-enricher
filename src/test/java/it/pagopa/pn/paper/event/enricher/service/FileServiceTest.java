package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.model.FileDetail;
import it.pagopa.pn.paper.event.enricher.model.IndexData;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.function.IOIterator;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileServiceTest {

    FileService fileService;

    SafeStorageService safeStorageService;

    @BeforeAll
    void setUp() {
        safeStorageService = mock(SafeStorageService.class);
        fileService = new FileService(safeStorageService);
    }

    @Test
    void extractFilesFromArchiveWithValidZipEntries() throws IOException {
        ZipArchiveInputStream zipInputStream = mock(ZipArchiveInputStream.class);
        ZipArchiveEntry zipEntry = new ZipArchiveEntry("test.pdf");
        when(zipInputStream.readAllBytes()).thenReturn("testContent".getBytes());
        when(zipInputStream.getNextEntry()).thenReturn(zipEntry, (ZipArchiveEntry) null);
        when(zipInputStream.iterator()).thenReturn(IOIterator.adapt(Collections.singletonList(zipEntry).iterator()));

        Map<String, IndexData> indexDataMap = new HashMap<>();
        Flux<FileDetail> result = fileService.extractFilesFromArchive(zipInputStream, indexDataMap);

        StepVerifier.create(result)
                .expectNextMatches(fileDetail -> "test.pdf".equals(fileDetail.getFilename()))
                .verifyComplete();
    }

    @Test
    void extractFilesFromArchiveWithEmptyZip() {
        ZipArchiveInputStream zipInputStream = mock(ZipArchiveInputStream.class);
        when(zipInputStream.iterator()).thenReturn(IOIterator.adapt(Collections.emptyIterator()));

        Map<String, IndexData> indexDataMap = new HashMap<>();
        Flux<FileDetail> result = fileService.extractFilesFromArchive(zipInputStream, indexDataMap);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void extractFilesFromArchiveP7mWithEmptyZip() {
        ZipArchiveInputStream zipInputStream = mock(ZipArchiveInputStream.class);
        when(zipInputStream.iterator()).thenReturn(IOIterator.adapt(Collections.emptyIterator()));

        Map<String, IndexData> indexDataMap = new HashMap<>();
        List<FileDetail> result = fileService.extractFilesFromArchiveP7m(zipInputStream, indexDataMap);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void retrieveDownloadUrlReturnsValidUrl() {
        String archiveFileKey = "testKey";
        when(safeStorageService.callSafeStorageGetFileAndDownload(archiveFileKey)).thenReturn(Mono.just("testUrl"));

        Mono<String> result = fileService.retrieveDownloadUrl(archiveFileKey);

        StepVerifier.create(result)
                .expectNext("testUrl")
                .verifyComplete();
    }

    @Test
    void downloadFileReturnsFileContent() {
        String url = "testUrl";
        byte[] content = "testContent".getBytes();
        when(safeStorageService.downloadContent(url)).thenReturn(Flux.just(content));

        Flux<byte[]> result = fileService.downloadFile(url);

        StepVerifier.create(result)
                .expectNext(content)
                .verifyComplete();
    }

    @Test
    void uploadPdfReturnsFileKey() {
        byte[] pdfBytes = "testPdf".getBytes();
        String sha256 = "testSha256";
        when(safeStorageService.callSafeStorageCreateFileAndUpload(pdfBytes, sha256)).thenReturn(Mono.just("testFileKey"));

        Mono<String> result = fileService.uploadPdf(pdfBytes, sha256);

        StepVerifier.create(result)
                .expectNext("testFileKey")
                .verifyComplete();
    }

    @Test
    void retrieveDownloadUrlWithInvalidKey() {
        String archiveFileKey = "invalidKey";
        when(safeStorageService.callSafeStorageGetFileAndDownload(archiveFileKey)).thenReturn(Mono.error(new RuntimeException("File not found")));

        Mono<String> result = fileService.retrieveDownloadUrl(archiveFileKey);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("File not found"))
                .verify();
    }


    @Test
    void downloadFileWithInvalidUrl() {
        String url = "http://example.com/invalid";
        when(safeStorageService.downloadContent(url)).thenReturn(Flux.error(new RuntimeException("File not found")));

        Flux<byte[]> result = fileService.downloadFile(url);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("File not found"))
                .verify();
    }


    @Test
    void uploadPdfWithInvalidData() {
        byte[] pdfBytes = "invalid content".getBytes();
        String sha256 = "invalidSha256";
        when(safeStorageService.callSafeStorageCreateFileAndUpload(pdfBytes, sha256)).thenReturn(Mono.error(new RuntimeException("Upload failed")));

        Mono<String> result = fileService.uploadPdf(pdfBytes, sha256);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("Upload failed"))
                .verify();
    }
}
