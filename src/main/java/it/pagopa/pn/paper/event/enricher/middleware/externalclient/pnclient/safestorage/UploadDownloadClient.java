package it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage;

import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import lombok.CustomLog;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.*;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Objects;

@Component
@CustomLog
public class UploadDownloadClient {

    private static final int RETRY_MAX_ATTEMPTS_PRESIGNED_URL = 3;

    protected WebClient webClient;

    public UploadDownloadClient() {
        this.webClient = WebClient.builder()
                .filter(buildRetryExchangeFilterFunction())
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
                .header("x-amz-checksum-sha256", sha256)
                .bodyValue(content)
                .retrieve()
                .toBodilessEntity()
                .thenReturn(fileCreationResponse.getKey())
                .onErrorResume(ee -> {
                    log.error("Error during upload file", ee);
                    return Mono.error(new PaperEventEnricherException(ee.getMessage(), 500, "UPLOAD_ERROR"));
                });
    }

    public Mono<Void> downloadContent(String downloadUrl, Path path) {
        log.info("start to download file from: {}", downloadUrl);
        WritableByteChannel channel = null;
        try {
            channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            WritableByteChannel finalChannel = channel;
            return webClient
                    .get()
                    .uri(new URI(downloadUrl))
                    .retrieve()
                    .bodyToFlux(DataBuffer.class)
                    .flatMap(dataBuffer -> DataBufferUtils.write(Flux.just(dataBuffer), finalChannel)
                            .doOnError(e -> log.error("Error during file writing"))
                            .doFinally(signalType -> DataBufferUtils.release(dataBuffer)))
                    .doOnComplete(() -> closeWritableByteChannel(finalChannel))
                    .doOnError(throwable -> closeWritableByteChannel(finalChannel))
                    .then();

        } catch (Exception ex) {
            log.error("error in URI ", ex);
            closeWritableByteChannel(channel);
            return Mono.error(new PaperEventEnricherException(ex.getMessage(), 500, "DOWNLOAD_ERROR"));
        }
    }

    public void closeWritableByteChannel(WritableByteChannel channel) {
        try {
            if(Objects.nonNull(channel) && channel.isOpen())
                channel.close();
            log.info("Download and file writing completed successfully");
        } catch (IOException e) {
            log.error("Error closing channel", e);
        }
    }


    protected ExchangeFilterFunction buildRetryExchangeFilterFunction() {
        return (request, next) -> next.exchange(request)
                .flatMap(clientResponse -> Mono.just(clientResponse)
                        .filter(response -> clientResponse.statusCode().isError())
                        .flatMap(response -> clientResponse.createException())
                        .flatMap(Mono::error)
                        .thenReturn(clientResponse))
                .retryWhen( Retry.backoff(RETRY_MAX_ATTEMPTS_PRESIGNED_URL, Duration.ofMillis(25)).jitter(0.75)
                        .filter(this::isRetryableException)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            Throwable lastExceptionInRetry = retrySignal.failure();
                            log.warn("Retries exhausted {}, with last Exception: {}", retrySignal.totalRetries(), lastExceptionInRetry.getMessage());
                            return lastExceptionInRetry;
                        })
                );
    }

    protected boolean isRetryableException(Throwable throwable) {
        boolean retryable = throwable instanceof TimeoutException ||
                throwable instanceof SocketException ||
                throwable instanceof SocketTimeoutException ||
                throwable instanceof SSLHandshakeException ||
                throwable instanceof UnknownHostException ||
                throwable instanceof WebClientRequestException ||
                throwable instanceof WebClientResponseException.TooManyRequests ||
                throwable instanceof WebClientResponseException.GatewayTimeout ||
                throwable instanceof WebClientResponseException.BadGateway ||
                throwable instanceof WebClientResponseException.ServiceUnavailable
                ;
        if(retryable) {
            log.warn("Exception caught by retry", throwable);
        }
        return retryable;
    }
}
