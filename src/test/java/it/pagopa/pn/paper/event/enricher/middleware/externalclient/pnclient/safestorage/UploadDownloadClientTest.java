package it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage;

import io.netty.handler.timeout.TimeoutException;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.*;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileSystemProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(SpringExtension.class)
class UploadDownloadClientTest {


    private ExchangeFunction exchangeFunction;
    private UploadDownloadClient uploadDownloadClient;


    @BeforeEach
    void setUp() {
        exchangeFunction = mock(ExchangeFunction.class);
        uploadDownloadClient = new UploadDownloadClient();
        uploadDownloadClient.webClient = mock(WebClient.class);
    }

    @Test
    void testUploadContentPOST() {
        FileCreationResponse fileCreationResponse = mock(FileCreationResponse.class);
        when(fileCreationResponse.getSecret()).thenReturn("Secret");
        when(fileCreationResponse.getUploadUrl()).thenReturn("https://example.org/example");
        when(fileCreationResponse.getKey()).thenReturn("key");
        when(fileCreationResponse.getUploadMethod()).thenReturn(FileCreationResponse.UploadMethodEnum.POST);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        when(uploadDownloadClient.webClient.method(any())).thenReturn(requestBodyUriSpec);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        when(requestBodyUriSpec.uri(URI.create("https://example.org/example"))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());
        StepVerifier.create(uploadDownloadClient.uploadContent("test".getBytes(), fileCreationResponse, "Sha256"))
                .expectNext("key")
                .verifyComplete();
    }

    @Test
    void testUploadContentPUT() {
        FileCreationResponse fileCreationResponse = mock(FileCreationResponse.class);
        when(fileCreationResponse.getSecret()).thenReturn("Secret");
        when(fileCreationResponse.getUploadUrl()).thenReturn("https://example.org/example");
        when(fileCreationResponse.getKey()).thenReturn("key");
        when(fileCreationResponse.getUploadMethod()).thenReturn(FileCreationResponse.UploadMethodEnum.PUT);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        when(uploadDownloadClient.webClient.method(any())).thenReturn(requestBodyUriSpec);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        when(requestBodyUriSpec.uri(URI.create("https://example.org/example"))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());
        StepVerifier.create(uploadDownloadClient.uploadContent("test".getBytes(), fileCreationResponse, "Sha256"))
                .expectNext("key")
                .verifyComplete();
    }


    @Test
    void testDownloadContent() throws IOException {
        Path path = mock(Path.class);
        FileSystem fileSystem = mock(FileSystem.class);
        when(path.getFileSystem()).thenReturn(fileSystem);
        when(fileSystem.provider()).thenReturn(mock(FileSystemProvider.class));
        WritableByteChannel channel = mock(WritableByteChannel.class);
        doNothing().when(channel).close();
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        when(uploadDownloadClient.webClient.get()).thenReturn(requestHeadersUriSpec);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        when(requestHeadersUriSpec.uri(URI.create("https://example.org/example"))).thenReturn(requestBodySpec);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.empty());
        StepVerifier.create(uploadDownloadClient.downloadContent("https://example.org/example", path))
                .verifyComplete();
    }

    @Test
    void testDownloadContentUriSintaxException() throws IOException {
        Path path = mock(Path.class);
        FileSystem fileSystem = mock(FileSystem.class);
        when(path.getFileSystem()).thenReturn(fileSystem);
        when(fileSystem.provider()).thenReturn(mock(FileSystemProvider.class));
        WritableByteChannel channel = mock(WritableByteChannel.class);
        doNothing().when(channel).close();
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        when(uploadDownloadClient.webClient.get()).thenReturn(requestHeadersUriSpec);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        when(requestHeadersUriSpec.uri(URI.create("https://example"))).thenThrow(new RuntimeException("https://example"));
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(DataBuffer.class)).thenReturn(Flux.empty());
        StepVerifier.create(uploadDownloadClient.downloadContent("https://example.org/example", path))
                .verifyError(PaperEventEnricherException.class);
    }

    @Test
    void closeChannelTest() throws IOException {
        Path path = Paths.get("src/test/resources/archive.zip");
        WritableByteChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        Assertions.assertDoesNotThrow(() -> uploadDownloadClient.closeWritableByteChannel(channel));
    }

    @Test
    void buildRetryExchangeFilterFunction() {
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.createException()).thenReturn(Mono.just(mock(WebClientResponseException.class)));
        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));
        ClientRequest request = mock(ClientRequest.class);
        StepVerifier.create(uploadDownloadClient.buildRetryExchangeFilterFunction().filter(request, exchangeFunction))
                .expectError(WebClientResponseException.class)
                .verify();

        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    void buildRetryExchangeFilterFunction_retriesOnSocketTimeoutException() {
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exchangeFunction.exchange(any())).thenReturn(Mono.error(new SocketTimeoutException("Timeout")));
        ClientRequest request = mock(ClientRequest.class);
        StepVerifier.create(uploadDownloadClient.buildRetryExchangeFilterFunction().filter(request, exchangeFunction))
                .expectError(SocketTimeoutException.class)
                .verify();

        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    void buildRetryExchangeFilterFunction_retriesOnTimeoutException() {
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exchangeFunction.exchange(any())).thenReturn(Mono.error(mock(TimeoutException.class)));
        ClientRequest request = mock(ClientRequest.class);
        StepVerifier.create(uploadDownloadClient.buildRetryExchangeFilterFunction().filter(request, exchangeFunction))
                .expectError(TimeoutException.class)
                .verify();

        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    void buildRetryExchangeFilterFunction_retriesOnSSLHandshakeException() {
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exchangeFunction.exchange(any())).thenReturn(Mono.error(new SSLHandshakeException("error")));
        ClientRequest request = mock(ClientRequest.class);
        StepVerifier.create(uploadDownloadClient.buildRetryExchangeFilterFunction().filter(request, exchangeFunction))
                .expectError(SSLHandshakeException.class)
                .verify();

        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    void buildRetryExchangeFilterFunction_retriesOnUnknownHostException() {
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exchangeFunction.exchange(any())).thenReturn(Mono.error(new UnknownHostException("error")));
        ClientRequest request = mock(ClientRequest.class);
        StepVerifier.create(uploadDownloadClient.buildRetryExchangeFilterFunction().filter(request, exchangeFunction))
                .expectError(UnknownHostException.class)
                .verify();

        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    void buildRetryExchangeFilterFunction_retriesOnWebClientRequestException() {
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exchangeFunction.exchange(any())).thenReturn(Mono.error(mock(WebClientRequestException.class)));
        ClientRequest request = mock(ClientRequest.class);
        StepVerifier.create(uploadDownloadClient.buildRetryExchangeFilterFunction().filter(request, exchangeFunction))
                .expectError(WebClientRequestException.class)
                .verify();

        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    void buildRetryExchangeFilterFunction_retriesOnWebClientResponseExceptionTooManyRequests() {
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exchangeFunction.exchange(any())).thenReturn(Mono.error(mock(WebClientResponseException.TooManyRequests.class)));
        ClientRequest request = mock(ClientRequest.class);
        StepVerifier.create(uploadDownloadClient.buildRetryExchangeFilterFunction().filter(request, exchangeFunction))
                .expectError(WebClientResponseException.TooManyRequests.class)
                .verify();

        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    void buildRetryExchangeFilterFunction_retriesOnWebClientResponseExceptionGatewayTimeout() {
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exchangeFunction.exchange(any())).thenReturn(Mono.error(mock(WebClientResponseException.GatewayTimeout.class)));
        ClientRequest request = mock(ClientRequest.class);
        StepVerifier.create(uploadDownloadClient.buildRetryExchangeFilterFunction().filter(request, exchangeFunction))
                .expectError(WebClientResponseException.GatewayTimeout.class)
                .verify();

        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    void buildRetryExchangeFilterFunction_retriesOnWebClientResponseExceptionBadGateway() {
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exchangeFunction.exchange(any())).thenReturn(Mono.error(mock(WebClientResponseException.BadGateway.class)));
        ClientRequest request = mock(ClientRequest.class);
        StepVerifier.create(uploadDownloadClient.buildRetryExchangeFilterFunction().filter(request, exchangeFunction))
                .expectError(WebClientResponseException.BadGateway.class)
                .verify();

        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    void buildRetryExchangeFilterFunction_retriesOnWebClientResponseExceptionServiceUnavailable() {
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exchangeFunction.exchange(any())).thenReturn(Mono.error(mock(WebClientResponseException.ServiceUnavailable.class)));
        ClientRequest request = mock(ClientRequest.class);
        StepVerifier.create(uploadDownloadClient.buildRetryExchangeFilterFunction().filter(request, exchangeFunction))
                .expectError(WebClientResponseException.ServiceUnavailable.class)
                .verify();

        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    void buildRetryExchangeFilterFunction_retriesOnSocketException() {
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exchangeFunction.exchange(any())).thenReturn(Mono.error(new SocketException("Socket error")));
        ClientRequest request = mock(ClientRequest.class);
        StepVerifier.create(uploadDownloadClient.buildRetryExchangeFilterFunction().filter(request, exchangeFunction))
                .expectError(SocketException.class)
                .verify();

        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    void buildRetryExchangeFilterFunction_doesNotRetryOnNonRetryableException() {
        ClientResponse clientResponse = mock(ClientResponse.class);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(exchangeFunction.exchange(any())).thenReturn(Mono.error(new RuntimeException("Non-retryable error")));
        ClientRequest request = mock(ClientRequest.class);
        StepVerifier.create(uploadDownloadClient.buildRetryExchangeFilterFunction().filter(request, exchangeFunction))
                .expectError(RuntimeException.class)
                .verify();

        verify(exchangeFunction, times(1)).exchange(any());
    }

}