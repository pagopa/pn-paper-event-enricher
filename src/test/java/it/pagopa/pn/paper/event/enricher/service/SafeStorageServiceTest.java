package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileCreationRequest;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileDownloadInfo;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage.UploadDownloadClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class SafeStorageServiceTest {

    @Mock
    private PnSafeStorageClient pnSafeStorageClient;

    @Mock
    private UploadDownloadClient uploadDownloadClient;

    @InjectMocks
    private SafeStorageService safeStorageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void callSafeStorageCreateFileAndUpload_success() {
        byte[] content = "file content".getBytes();
        String sha256 = "dummySha256";
        FileCreationRequest fileCreationRequest = new FileCreationRequest();
        fileCreationRequest.setContentType("application/pdf");
        fileCreationRequest.setStatus("ATTACHED");
        fileCreationRequest.setDocumentType("DOCUMENT_TYPE");

        FileCreationResponse fileCreationResponse = new FileCreationResponse();
        fileCreationResponse.setKey("fileKey");

        when(pnSafeStorageClient.createFile(any(FileCreationRequest.class), eq(sha256)))
                .thenReturn(Mono.just(fileCreationResponse));
        when(uploadDownloadClient.uploadContent(eq(content), any(FileCreationResponse.class), eq(sha256)))
                .thenReturn(Mono.just("fileKey"));

        StepVerifier.create(safeStorageService.callSafeStorageCreateFileAndUpload(content, sha256))
                .expectNext("fileKey")
                .verifyComplete();
    }

    @Test
    void callSafeStorageCreateFileAndUpload_failure() {
        byte[] content = "file content".getBytes();
        String sha256 = "dummySha256";

        when(pnSafeStorageClient.createFile(any(FileCreationRequest.class), eq(sha256)))
                .thenReturn(Mono.error(new RuntimeException("Creation failed")));

        StepVerifier.create(safeStorageService.callSafeStorageCreateFileAndUpload(content, sha256))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void callSafeStorageGetFile_success() {
        String fileKey = "SAFE_STORAGE_PREFIXfileKey";
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        FileDownloadInfo fileDownloadInfo = new FileDownloadInfo();
        fileDownloadInfo.setUrl("http://download.url");
        fileDownloadResponse.setDownload(fileDownloadInfo);

        when(pnSafeStorageClient.getFile(fileKey))
                .thenReturn(Mono.just(fileDownloadResponse));

        StepVerifier.create(safeStorageService.callSafeStorageGetFile(fileKey))
                .expectNext("http://download.url")
                .verifyComplete();
    }

    @Test
    void callSafeStorageGetFile_downloadUrlIsNull() {
        String fileKey = "SAFE_STORAGE_PREFIXfileKey";
        FileDownloadResponse fileCreationResponseDto = new FileDownloadResponse();
        fileCreationResponseDto.setKey(fileKey);

        when(pnSafeStorageClient.getFile(fileKey))
                .thenReturn(Mono.just(fileCreationResponseDto));

        StepVerifier.create(safeStorageService.callSafeStorageGetFile(fileKey))
                .expectError(PaperEventEnricherException.class)
                .verify();
    }

    @Test
    void callSafeStorageGetFile_failure() {
        String fileKey = "SAFE_STORAGE_PREFIXfileKey";

        when(pnSafeStorageClient.getFile(fileKey))
                .thenReturn(Mono.error(new RuntimeException("Download failed")));

        StepVerifier.create(safeStorageService.callSafeStorageGetFile(fileKey))
                .expectError(RuntimeException.class)
                .verify();
    }
}