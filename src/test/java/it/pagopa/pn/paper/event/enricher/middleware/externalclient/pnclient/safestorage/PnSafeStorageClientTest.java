package it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.constant.PaperEventEnricherConstant;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.exception.PnSafeStorageException;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.api.FileDownloadApi;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.api.FileUploadApi;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileCreationRequest;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileDownloadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Objects;

import static org.mockito.Mockito.when;

class PnSafeStorageClientTest {

    private PnSafeStorageClient pnSafeStorageClient;
    private FileUploadApi fileUploadApi;
    private FileDownloadApi fileDownloadApi;
    private PnPaperEventEnricherConfig pnPaperEventEnricherConfig;

    @BeforeEach
    void setUp() {
        fileUploadApi = Mockito.mock(FileUploadApi.class);
        fileDownloadApi = Mockito.mock(FileDownloadApi.class);
        pnPaperEventEnricherConfig = Mockito.mock(PnPaperEventEnricherConfig.class);
        pnSafeStorageClient = new PnSafeStorageClient(fileUploadApi, fileDownloadApi, pnPaperEventEnricherConfig);
    }

    @Test
    void createFile_success() {
        FileCreationRequest fileCreationRequest = new FileCreationRequest();
        FileCreationResponse fileCreationResponse = new FileCreationResponse();
        when(pnPaperEventEnricherConfig.getCxId()).thenReturn("cxId");
        when(fileUploadApi.createFile("cxId", PaperEventEnricherConstant.X_CHECKSUM, "checksum", fileCreationRequest))
                .thenReturn(Mono.just(fileCreationResponse));

        Mono<FileCreationResponse> result = pnSafeStorageClient.createFile(fileCreationRequest, "checksum");

        StepVerifier.create(result)
                .expectNext(fileCreationResponse)
                .verifyComplete();
    }

    @Test
    void createFile_error() {
        FileCreationRequest fileCreationRequest = new FileCreationRequest();
        when(pnPaperEventEnricherConfig.getCxId()).thenReturn("cxId");
        when(fileUploadApi.createFile("cxId", PaperEventEnricherConstant.X_CHECKSUM, "checksum", fileCreationRequest))
                .thenReturn(Mono.error(new WebClientResponseException(500, "Internal Server Error", null, null, null)));

        Mono<FileCreationResponse> result = pnSafeStorageClient.createFile(fileCreationRequest, "checksum");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof PaperEventEnricherException &&
                        Objects.equals(throwable.getMessage(), "DOCUMENT_UPLOAD_ERROR"))
                .verify();
    }

    @Test
    void getFile_success() {
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        when(pnPaperEventEnricherConfig.getCxId()).thenReturn("cxId");
        when(fileDownloadApi.getFile("fileKey", "cxId", false))
                .thenReturn(Mono.just(fileDownloadResponse));

        Mono<FileDownloadResponse> result = pnSafeStorageClient.getFile("safestorage://fileKey");

        StepVerifier.create(result)
                .expectNext(fileDownloadResponse)
                .verifyComplete();
    }

    @Test
    void getFile_notFound() {
        when(pnPaperEventEnricherConfig.getCxId()).thenReturn("cxId");
        when(fileDownloadApi.getFile("fileKey", "cxId", false))
                .thenReturn(Mono.error(new WebClientResponseException(404, "Not Found", null, null, null)));

        Mono<FileDownloadResponse> result = pnSafeStorageClient.getFile("fileKey");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof PaperEventEnricherException &&
                        Objects.equals(throwable.getMessage(), "DOCUMENT_UNAVAILABLE"))
                .verify();
    }

    @Test
    void getFile_otherError() {
        when(pnPaperEventEnricherConfig.getCxId()).thenReturn("cxId");
        when(fileDownloadApi.getFile("fileKey", "cxId", false))
                .thenReturn(Mono.error(new WebClientResponseException(500, "Internal Server Error", null, null, null)));

        Mono<FileDownloadResponse> result = pnSafeStorageClient.getFile("fileKey");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof PnSafeStorageException)
                .verify();
    }
}