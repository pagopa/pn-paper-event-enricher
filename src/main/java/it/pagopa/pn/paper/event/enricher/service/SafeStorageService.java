package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileCreationRequest;
import it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage.UploadDownloadClient;
import it.pagopa.pn.paper.event.enricher.model.ByteObject;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.Objects;

import static it.pagopa.pn.paper.event.enricher.constant.PaperEventEnricherConstant.*;

@Service
@CustomLog
@RequiredArgsConstructor
public class SafeStorageService {
    private final PnSafeStorageClient pnSafeStorageClient;
    private final UploadDownloadClient uploadDownloadClient;

    private final PnPaperEventEnricherConfig pnPaperEventEnricherConfig;

    public Mono<String> callSafeStorageCreateFileAndUpload(byte[] content, String sha256) {
        FileCreationRequest fileCreationRequestDto = buildFileCreationRequest();
        return pnSafeStorageClient.createFile(fileCreationRequestDto, sha256)
                .flatMap(fileCreationResponseDto -> uploadDownloadClient.uploadContent(content, fileCreationResponseDto, sha256)
                        .doOnNext(response -> log.info("file [{}] uploaded", fileCreationResponseDto.getKey()))
                        .thenReturn(fileCreationResponseDto.getKey()))
                .onErrorResume(e -> {
                    log.error("failed to create file", e);
                    return Mono.error(e);
                });
    }

    private FileCreationRequest buildFileCreationRequest() {
        FileCreationRequest fileCreationRequest = new FileCreationRequest();
        fileCreationRequest.setContentType("application/pdf");
        fileCreationRequest.setStatus(ATTACHED);
        fileCreationRequest.setDocumentType(DOCUMENT_TYPE);
        return fileCreationRequest;
    }

    public Mono<String> callSafeStorageGetFileAndDownload(String fileKey) {
        String finalFileKey = fileKey.replace(SAFE_STORAGE_PREFIX, "");
        return pnSafeStorageClient.getFile(finalFileKey)
                .flatMap(fileCreationResponseDto -> {
                    if(Objects.nonNull(fileCreationResponseDto.getDownload()) && Objects.nonNull(fileCreationResponseDto.getDownload().getUrl())){
                        return Mono.just(fileCreationResponseDto.getDownload().getUrl());
                    }
                    return Mono.error(new RuntimeException("Download url is null"));
                })
                .onErrorResume(e -> {
                    log.error("failed to download file", e);
                    return Mono.error(e);
                });
    }

    public Mono<Void> downloadContent(String downloadUrl, Path path) {
        //TODO: PROVARE DI NUOVO DOPO AVER MESSO VOI
        return uploadDownloadClient.downloadContent(downloadUrl, path);
    }
}
