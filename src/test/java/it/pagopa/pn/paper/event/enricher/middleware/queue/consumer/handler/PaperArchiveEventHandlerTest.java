package it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperArchiveEvent;
import it.pagopa.pn.paper.event.enricher.service.PaperEventEnricherService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PaperArchiveEventHandlerTest {
    @Mock
    private PaperEventEnricherService paperEventEnricherService;

    @InjectMocks
    private PaperArchiveEventHandler paperArchiveEventHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldHandleMessageSuccessfully() {

        PaperArchiveEvent.Payload inputMessage = PaperArchiveEvent.Payload.builder()
                .archiveFileKey("archiveFileKey")
                .archiveStatus("archiveStatus")
                .build();

        when(paperEventEnricherService.handlePaperEventEnricherEvent(any())).thenReturn(Mono.empty());

        // When
        paperArchiveEventHandler.paperArchiveConsumer(inputMessage, new HashMap<>());

        Assertions.assertEquals("archiveFileKey", MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
    }

    @Test
    void shouldHandleMessageWithError() {

        PaperArchiveEvent.Payload inputMessage = PaperArchiveEvent.Payload.builder()
                .archiveFileKey("archiveFileKey")
                .archiveStatus("archiveStatus")
                .build();

        when(paperEventEnricherService.handlePaperEventEnricherEvent(any())).thenReturn(Mono.error(new PaperEventEnricherException("error", 400, "error")));

        Executable executable = () -> paperArchiveEventHandler.paperArchiveConsumer(inputMessage, new HashMap<>());
        Assertions.assertThrows(PaperEventEnricherException.class, executable);
    }

}
