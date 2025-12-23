package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileCreationRequest;
import it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage.UploadDownloadClient;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static it.pagopa.pn.paper.event.enricher.constant.PaperEventEnricherConstant.*;
import static it.pagopa.pn.paper.event.enricher.exception.PnPaperEventEnricherExceptionConstant.DOWNLOAD_URL_IS_NULL;
import static it.pagopa.pn.paper.event.enricher.exception.PnPaperEventEnricherExceptionConstant.ERROR_GET_FILE;

@Service
@CustomLog
@RequiredArgsConstructor
public class SafeStorageService {
    private final PnSafeStorageClient pnSafeStorageClient;
    private final UploadDownloadClient uploadDownloadClient;

    public Mono<String> callSafeStorageCreateFileAndUpload(byte[] content, String sha256, String archiveFileKey) {
        FileCreationRequest fileCreationRequestDto = buildFileCreationRequest(archiveFileKey);
        return pnSafeStorageClient.createFile(fileCreationRequestDto, sha256)
                .flatMap(fileCreationResponseDto -> uploadDownloadClient.uploadContent(content, fileCreationResponseDto, sha256)
                        .doOnNext(response -> log.info("file [{}] uploaded", fileCreationResponseDto.getKey()))
                        .thenReturn(fileCreationResponseDto.getKey()))
                .onErrorResume(e -> {
                    log.error("failed to create file", e);
                    return Mono.error(e);
                });
    }

    private FileCreationRequest buildFileCreationRequest(String archiveFileKey) {
        FileCreationRequest fileCreationRequest = new FileCreationRequest();
        fileCreationRequest.setContentType("application/pdf");
        fileCreationRequest.setStatus(ATTACHED);
        fileCreationRequest.setDocumentType(DOCUMENT_TYPE);
        fileCreationRequest.setTags(Map.of("archiveFileKey", List.of(archiveFileKey)));
        return fileCreationRequest;
    }

    public Mono<String> callSafeStorageGetFile(String fileKey) {
        String finalFileKey = fileKey.replace(SAFE_STORAGE_PREFIX, "");
        return pnSafeStorageClient.getFile(finalFileKey)
                .flatMap(fileCreationResponseDto -> {
                    if(Objects.nonNull(fileCreationResponseDto.getDownload()) && Objects.nonNull(fileCreationResponseDto.getDownload().getUrl())){
                        return Mono.just(fileCreationResponseDto.getDownload().getUrl());
                    }
                    return Mono.error(new PaperEventEnricherException(DOWNLOAD_URL_IS_NULL, 400, ERROR_GET_FILE));
                });
    }
}
