package it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperEventEnricherInputEvent;
import it.pagopa.pn.paper.event.enricher.service.PaperEventEnricherService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

class PaperEventEnricherInputEventHandlerTest {

    @Mock
    private PaperEventEnricherService paperEventEnricherService;

    @Mock
    private Message<PaperEventEnricherInputEvent.Payload> message;

    @Mock
    private PaperEventEnricherInputEvent.Payload payload;

    @InjectMocks
    private PaperEventEnricherInputEventHandler paperEventEnricherInputEventHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldHandleMessageSuccessfully() {

        PaperEventEnricherInputEvent.Payload inputMessage = PaperEventEnricherInputEvent.Payload.builder()
                .analogMail(PaperEventEnricherInputEvent.Payload.AnalogMailDetail.builder()
                        .requestId("requestId")
                        .attachments(List.of(PaperEventEnricherInputEvent.Payload.Attachment.builder()
                                .build()))
                        .build())
                .build();

        PaperEventEnricherInputEvent paperEventEnricherInputEvent =PaperEventEnricherInputEvent.builder()
                .payload(inputMessage).build();

        when(message.getPayload()).thenReturn(paperEventEnricherInputEvent.getPayload());

        when(paperEventEnricherService.handleInputEventMessage(any())).thenReturn(Mono.empty());

        // When
        paperEventEnricherInputEventHandler.pnPaperEventEnricherConsumer().accept(message);

        Assertions.assertEquals("requestId", MDC.get(MDCUtils.MDC_PN_CTX_REQUEST_ID));
    }

    @Test
    void shouldHandleMessageWithError() {


        PaperEventEnricherInputEvent.Payload inputMessage = PaperEventEnricherInputEvent.Payload.builder()
                .analogMail(PaperEventEnricherInputEvent.Payload.AnalogMailDetail.builder()
                        .requestId("requestId")
                        .attachments(List.of(PaperEventEnricherInputEvent.Payload.Attachment.builder()
                                .build()))
                        .build())
                .build();

        PaperEventEnricherInputEvent paperEventEnricherInputEvent =PaperEventEnricherInputEvent.builder()
                .payload(inputMessage).build();

        when(message.getPayload()).thenReturn(paperEventEnricherInputEvent.getPayload());

        when(paperEventEnricherService.handleInputEventMessage(any())).thenReturn(Mono.error(new PaperEventEnricherException("error", 400, "error")));

        Executable executable = () -> paperEventEnricherInputEventHandler.pnPaperEventEnricherConsumer().accept(message);

        Assertions.assertThrows(PaperEventEnricherException.class, executable);

    }
}
