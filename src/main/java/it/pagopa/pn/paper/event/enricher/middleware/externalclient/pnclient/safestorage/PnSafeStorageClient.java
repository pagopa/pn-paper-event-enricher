package it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.constant.PaperEventEnricherConstant;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.exception.PnSafeStorageException;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.api.FileDownloadApi;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.api.FileUploadApi;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileCreationRequest;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.common.common.BaseClient;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static it.pagopa.pn.paper.event.enricher.constant.PaperEventEnricherConstant.SAFE_STORAGE_PREFIX;


@CustomLog
@Component
@RequiredArgsConstructor
public class PnSafeStorageClient extends BaseClient {

    private final FileUploadApi fileUploadApi;
    private final FileDownloadApi fileDownloadApi;
    private final PnPaperEventEnricherConfig pnPaperEventEnricherConfig;


    public Mono<FileCreationResponse> createFile(FileCreationRequest fileCreationRequestDto, String checksum) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, "createFile");

        log.debug(String.format("Req params: %s", fileCreationRequestDto.getContentType()));
        log.debug(String.format("storage id %s ", this.pnPaperEventEnricherConfig.getCxId()));
        return this.fileUploadApi.createFile(this.pnPaperEventEnricherConfig.getCxId(), PaperEventEnricherConstant.X_CHECKSUM, checksum, fileCreationRequestDto)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(25))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                ).onErrorResume(WebClientResponseException.class, ex -> {
                    log.error(ex.getResponseBodyAsString());
                    return Mono.error(new PaperEventEnricherException("DOCUMENT_UPLOAD_ERROR", 500, "DOCUMENT_UPLOAD_ERROR"));
                });
    }

    public Mono<FileDownloadResponse> getFile(String fileKey) {
        if (fileKey.startsWith(SAFE_STORAGE_PREFIX)) {
            fileKey = fileKey.replace(SAFE_STORAGE_PREFIX, "");
        }
        log.debug("Req params : {}", fileKey);
        return fileDownloadApi.getFile(fileKey, this.pnPaperEventEnricherConfig.getCxId(), false)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                )
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error(ex.getResponseBodyAsString());
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.error(new PaperEventEnricherException("DOCUMENT_UNAVAILABLE", 404, "DOCUMENT_UNAVAILABLE"));
                    }
                    return Mono.error(new PnSafeStorageException(ex));
                });
    }

}