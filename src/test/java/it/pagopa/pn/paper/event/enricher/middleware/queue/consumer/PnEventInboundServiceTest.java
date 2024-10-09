package it.pagopa.pn.paper.event.enricher.middleware.queue.consumer;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.EventHandler;
import it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.PnEventInboundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PnEventInboundServiceTest {

    @Mock
    private EventHandler eventHandler;

    @Mock
    private PnPaperEventEnricherConfig pnPaperEventEnricherConfig;

    private PnEventInboundService pnEventInboundService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pnEventInboundService = new PnEventInboundService(eventHandler, pnPaperEventEnricherConfig);
    }

    @Test
    void testCustomRouterWithValidMessageIdAndTraceId() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("aws_messageId", "testMessageId");
        headers.put("X-Amzn-Trace-Id", "testTraceId");
        headers.put("aws_receivedQueue", "testQueue");

        MessageHeaders messageHeaders = new MessageHeaders(headers);
        Message<?> message = mock(Message.class);
        when(message.getHeaders()).thenReturn(messageHeaders);

        PnPaperEventEnricherConfig.Sqs sqsConfig = mock(PnPaperEventEnricherConfig.Sqs.class);
        when(pnPaperEventEnricherConfig.getSqs()).thenReturn(sqsConfig);
        when(sqsConfig.getPaperEventEnrichmentInputQueueName()).thenReturn("testQueue");
        when(eventHandler.getHandler()).thenReturn(Map.of("PAPER_EVENT_ENRICHER_INPUT_EVENTS", "testHandler"));

        MessageRoutingCallback callback = pnEventInboundService.customRouter();
        MessageRoutingCallback.FunctionRoutingResult result = callback.routingResult(message);

        assertEquals("testHandler", result.getFunctionDefinition());
    }

    @Test
    void testCustomRouterWithMissingMessageIdAndTraceId() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("aws_receivedQueue", "testQueue");

        MessageHeaders messageHeaders = new MessageHeaders(headers);
        Message<?> message = mock(Message.class);
        when(message.getHeaders()).thenReturn(messageHeaders);

        PnPaperEventEnricherConfig.Sqs sqsConfig = mock(PnPaperEventEnricherConfig.Sqs.class);
        when(pnPaperEventEnricherConfig.getSqs()).thenReturn(sqsConfig);
        when(sqsConfig.getPaperEventEnrichmentInputQueueName()).thenReturn("testQueue");
        when(eventHandler.getHandler()).thenReturn(Map.of("PAPER_EVENT_ENRICHER_INPUT_EVENTS", "testHandler"));

        MessageRoutingCallback callback = pnEventInboundService.customRouter();
        MessageRoutingCallback.FunctionRoutingResult result = callback.routingResult(message);

        assertEquals("testHandler", result.getFunctionDefinition());
    }

    @Test
    void testCustomRouterWithPaperEventEnrichmentInputQueue() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("aws_receivedQueue", "inputQueue");

        MessageHeaders messageHeaders = new MessageHeaders(headers);
        Message<?> message = mock(Message.class);
        when(message.getHeaders()).thenReturn(messageHeaders);

        PnPaperEventEnricherConfig.Sqs sqsConfig = mock(PnPaperEventEnricherConfig.Sqs.class);
        when(pnPaperEventEnricherConfig.getSqs()).thenReturn(sqsConfig);
        when(sqsConfig.getPaperEventEnrichmentInputQueueName()).thenReturn("inputQueue");
        when(eventHandler.getHandler()).thenReturn(Map.of("PAPER_EVENT_ENRICHER_INPUT_EVENTS", "inputHandler"));

        MessageRoutingCallback callback = pnEventInboundService.customRouter();
        MessageRoutingCallback.FunctionRoutingResult result = callback.routingResult(message);

        assertEquals("inputHandler", result.getFunctionDefinition());
    }

    @Test
    void testCustomRouterWithPaperArchivesQueueName() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("aws_receivedQueue", "archivesQueue");

        MessageHeaders messageHeaders = new MessageHeaders(headers);
        Message<?> message = mock(Message.class);
        when(message.getHeaders()).thenReturn(messageHeaders);

        PnPaperEventEnricherConfig.Sqs sqsConfig = mock(PnPaperEventEnricherConfig.Sqs.class);
        when(pnPaperEventEnricherConfig.getSqs()).thenReturn(sqsConfig);
        when(sqsConfig.getPaperArchivesQueueName()).thenReturn("archivesQueue");
        when(eventHandler.getHandler()).thenReturn(Map.of("PAPER_EVENT_ENRICHER_ARCHIVES_EVENTS", "archivesHandler"));

        MessageRoutingCallback callback = pnEventInboundService.customRouter();
        MessageRoutingCallback.FunctionRoutingResult result = callback.routingResult(message);

        assertEquals("archivesHandler", result.getFunctionDefinition());
    }

    @Test
    void testCustomRouterWithInvalidQueue() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("aws_receivedQueue", "invalidQueue");

        MessageHeaders messageHeaders = new MessageHeaders(headers);
        Message<?> message = mock(Message.class);
        when(message.getHeaders()).thenReturn(messageHeaders);

        PnPaperEventEnricherConfig.Sqs sqsConfig = mock(PnPaperEventEnricherConfig.Sqs.class);
        when(pnPaperEventEnricherConfig.getSqs()).thenReturn(sqsConfig);
        when(sqsConfig.getPaperEventEnrichmentInputQueueName()).thenReturn("inputQueue");
        when(sqsConfig.getPaperArchivesQueueName()).thenReturn("archivesQueue");

        MessageRoutingCallback callback = pnEventInboundService.customRouter();

        assertThrows(PnInternalException.class, () -> callback.routingResult(message));
    }

    @Test
    void testCustomRouterWithUndefinedHandler() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("aws_receivedQueue", "inputQueue");

        MessageHeaders messageHeaders = new MessageHeaders(headers);
        Message<?> message = mock(Message.class);
        when(message.getHeaders()).thenReturn(messageHeaders);

        PnPaperEventEnricherConfig.Sqs sqsConfig = mock(PnPaperEventEnricherConfig.Sqs.class);
        when(pnPaperEventEnricherConfig.getSqs()).thenReturn(sqsConfig);
        when(sqsConfig.getPaperEventEnrichmentInputQueueName()).thenReturn("inputQueue");
        when(eventHandler.getHandler()).thenReturn(Map.of());

        MessageRoutingCallback callback = pnEventInboundService.customRouter();
        MessageRoutingCallback.FunctionRoutingResult result = callback.routingResult(message);

        assertEquals(null, result.getFunctionDefinition());
    }
}