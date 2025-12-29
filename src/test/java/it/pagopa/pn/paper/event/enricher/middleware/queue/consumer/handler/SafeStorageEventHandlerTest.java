package it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperEventEnricherInputEvent;
import it.pagopa.pn.paper.event.enricher.service.PaperEventEnricherService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SafeStorageEventHandlerTest {
    @Mock
    private PaperEventEnricherService paperEventEnricherService;

    @InjectMocks
    private SafeStorageEventHandler safeStorageEventHandler;

    @Test
    void shouldHandleMessageSuccessfully() {

        FileDownloadResponse inputMessage = new FileDownloadResponse();
        inputMessage.setKey("requestId");
        inputMessage.setTags(Map.of("archiveFileKey", List.of("archiveFileKeyValue")));

        when(paperEventEnricherService.handleSafeStorageEvent(any(), any())).thenReturn(Mono.empty());

        // When
        safeStorageEventHandler.safeStorageConsumer(inputMessage, new HashMap<>());

        Assertions.assertEquals("requestId", MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
    }

    @Test
    void shouldHandleMessageWithError() {

        FileDownloadResponse inputMessage = new FileDownloadResponse();
        inputMessage.setKey("requestId");
        inputMessage.setTags(Map.of("archiveFileKey", List.of("archiveFileKeyValue")));

        when(paperEventEnricherService.handleSafeStorageEvent(any(), any())).thenReturn(Mono.error(new PaperEventEnricherException("error", 400, "error")));

        Executable executable = () -> safeStorageEventHandler.safeStorageConsumer(inputMessage, new HashMap<>());

        Assertions.assertThrows(PaperEventEnricherException.class, executable);

    }
}
