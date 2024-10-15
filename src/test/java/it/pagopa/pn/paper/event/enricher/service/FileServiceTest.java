package it.pagopa.pn.paper.event.enricher.service;


import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage.UploadDownloadClient;
import it.pagopa.pn.paper.event.enricher.model.FileCounter;
import it.pagopa.pn.paper.event.enricher.model.FileDetail;
import it.pagopa.pn.paper.event.enricher.model.IndexData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileServiceTest {

    FileService fileService;

    SafeStorageService safeStorageService;
    UploadDownloadClient uploadDownloadClient;
    PnPaperEventEnricherConfig config;

    @BeforeAll
    void setUp() {
        safeStorageService = mock(SafeStorageService.class);
        uploadDownloadClient = mock(UploadDownloadClient.class);
        config = new PnPaperEventEnricherConfig();
        config.setSafeStorageUploadMaxConcurrentRequest(10);
        config.setPdfPagesNumber(2);
        fileService = new FileService(safeStorageService, uploadDownloadClient, config);
    }


    @Test
    void extractFileFromBinWithZipFile(){
        Path path = Paths.get("src/test/resources/attachment_example_completed.zip");

        Mono<Path> result = fileService.extractFileFromBin(path).doOnNext(newFile -> {
            try {
                Files.deleteIfExists(newFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void extractFileFromBinWithSevenZipFile() {
        Path path = Paths.get("src/test/resources/attachment_example_completed2.7z");

        Mono<Path> result = fileService.extractFileFromBin(path).doOnNext(newFile -> {
            try {
                Files.deleteIfExists(newFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
    }


    @Test
    void extractFileFromBinWithUnsupportedFile() {
        Path path = Paths.get("src/test/resources/test.txt");

        Mono<Path> result = fileService.extractFileFromBin(path);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof PaperEventEnricherException &&
                        Objects.equals(throwable.getMessage(), "Unsupported file type"));
    }

    @Test
    void extractFileFromArchiveWithZipFile() {
        Map<String, IndexData> indexDataMap = new HashMap<>();
        FileCounter counter = new FileCounter(new AtomicInteger(0), new AtomicInteger(0), 0);
        Path path = Paths.get("src/test/resources/archive.zip");
        when(safeStorageService.callSafeStorageCreateFileAndUpload(any(), any())).thenReturn(Mono.just("key"));

        Flux<FileDetail> result = fileService.extractFileFromArchive(path, indexDataMap, counter);

        StepVerifier.create(result).expectNextCount(1).verifyComplete();
    }

    @Test
    void extractFileFromArchiveWithZipFileWithoutCutFile() {
        config.setPdfPagesNumber(0);
        Map<String, IndexData> indexDataMap = new HashMap<>();
        FileCounter counter = new FileCounter(new AtomicInteger(0), new AtomicInteger(0), 0);
        Path path = Paths.get("src/test/resources/archive.zip");
        when(safeStorageService.callSafeStorageCreateFileAndUpload(any(), any())).thenReturn(Mono.just("key"));

        Flux<FileDetail> result = fileService.extractFileFromArchive(path, indexDataMap, counter);

        StepVerifier.create(result).expectNextCount(1).verifyComplete();
    }

    @Test
    void extractFileFromArchiveWithSevenZipFile() {
        Map<String, IndexData> indexDataMap = new HashMap<>();
        FileCounter counter = new FileCounter(new AtomicInteger(0), new AtomicInteger(0), 0);
        Path path = Paths.get("src/test/resources/archive.7z");
        when(safeStorageService.callSafeStorageCreateFileAndUpload(any(), any())).thenReturn(Mono.just("key"));

        Flux<FileDetail> result = fileService.extractFileFromArchive(path, indexDataMap, counter);

        StepVerifier.create(result).expectNextCount(1).verifyComplete();
    }

    @Test
    void extractFileFromArchiveWithUnsupportedFile() {
        Map<String, IndexData> indexDataMap = new HashMap<>();
        FileCounter counter = new FileCounter(new AtomicInteger(0), new AtomicInteger(0), 0);
        Path path = Paths.get("src/test/resources/test.txt");
        when(safeStorageService.callSafeStorageCreateFileAndUpload(any(), any())).thenReturn(Mono.just("key"));

        Flux<FileDetail> result = fileService.extractFileFromArchive(path, indexDataMap, counter);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof PaperEventEnricherException &&
                        Objects.equals(throwable.getMessage(), "Unsupported file type"));
    }

    @Test
    void downloadFile() {
        String archiveFileKey = "validArchiveFileKey";
        Path file = mock(Path.class);

        when(safeStorageService.callSafeStorageGetFile(archiveFileKey)).thenReturn(Mono.just("http://example.com/file"));
        when(uploadDownloadClient.downloadContent("http://example.com/file", file)).thenReturn(Mono.empty());
        Flux<Void> result = fileService.downloadFile(archiveFileKey, file);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void deleteFileTmp() throws IOException {
        Path file = Files.createTempFile("test", ".tmp");
        Assertions.assertDoesNotThrow(() -> fileService.deleteFileTmp(file));
    }
}