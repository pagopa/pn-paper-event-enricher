package it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import lombok.CustomLog;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;

@Component
@CustomLog
public class UploadDownloadClient {

    private final WebClient webClient;

    public UploadDownloadClient() {
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector())
                .build();
    }

    public Mono<String> uploadContent(byte[] content, FileCreationResponse fileCreationResponse, String sha256) {
        HttpMethod httpMethod = fileCreationResponse.getUploadMethod() == FileCreationResponse.UploadMethodEnum.POST ? HttpMethod.POST : HttpMethod.PUT;
        log.info("start to upload file to: {}", fileCreationResponse.getUploadUrl());
        return webClient.method(httpMethod)
                .uri(URI.create(fileCreationResponse.getUploadUrl()))
                .header("Content-Type", "application/pdf")
                .header("x-amz-meta-secret", fileCreationResponse.getSecret())
                .header("x-amz-checksum-sha256",sha256)
                .bodyValue(content)
                .retrieve()
                .toBodilessEntity()
                .thenReturn(fileCreationResponse.getKey())
                .onErrorResume(ee -> {
                    log.error("Normalize Address - uploadContent Exception uploading file", ee);
                    return Mono.error(new PaperEventEnricherException(ee.getMessage(), 500, "UPLOAD_ERROR"));
                });
    }

    public Flux<byte[]> downloadContent(String downloadUrl) {
        log.info("start to download file to: {}", downloadUrl);
        try {
            return WebClient.create()
                    .get()
                    .uri(new URI(downloadUrl))
                    .retrieve()
                    .bodyToFlux(DataBuffer.class)
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer); // Release buffer
                        return bytes;
                    })
                    .doOnError(ex -> log.error("Error in WebClient", ex))
                    .onErrorMap(ex -> {
                        log.error("downloadContent Exception downloading content", ex);
                        return new PaperEventEnricherException(ex.getMessage(), 500, "DOWNLOAD_ERROR");
                    });
        } catch (URISyntaxException ex) {
            log.error("error in URI ", ex);
            return Flux.error(new PaperEventEnricherException(ex.getMessage(), 500, "DOWNLOAD_ERROR"));
        }
    }
}
