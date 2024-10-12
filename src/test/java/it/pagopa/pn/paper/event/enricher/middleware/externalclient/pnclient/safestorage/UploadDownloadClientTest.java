package it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage;

import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(SpringExtension.class)
class UploadDownloadClientTest {
    private UploadDownloadClient uploadDownloadClient;


    @BeforeEach
    void setUp() {
        uploadDownloadClient = new UploadDownloadClient();
        uploadDownloadClient.webClient = mock(WebClient.class);
    }

    @Test
    void testUploadContent() {
        FileCreationResponse fileCreationResponse = mock(FileCreationResponse.class);
        when(fileCreationResponse.getSecret()).thenReturn("Secret");
        when(fileCreationResponse.getUploadUrl()).thenReturn("https://example.org/example");
        when(fileCreationResponse.getKey()).thenReturn("key");
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
}