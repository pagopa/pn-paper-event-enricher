package it.pagopa.pn.paper.event.enricher.middleware.queue.consumer;

import it.pagopa.pn.commons.utils.MDCUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PnEventInboundUtilsTest {

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void enrichMDC_withValidTraceIdAndMessageId() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("aws_messageId", "testMessageId");
        headers.put("X-Amzn-Trace-Id", "testTraceId");

        PnEventInboundUtils.enrichMDC(headers);

        assertEquals("testTraceId", MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
        assertEquals("testMessageId", MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
    }

    @Test
    void enrichMDC_withMissingTraceId() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("aws_messageId", "testMessageId");

        PnEventInboundUtils.enrichMDC(headers);

        assertEquals("testMessageId", MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
        assertTrue(MDC.get(MDCUtils.MDC_TRACE_ID_KEY).startsWith("traceId:"));
    }

    @Test
    void enrichMDC_withMissingMessageId() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("X-Amzn-Trace-Id", "testTraceId");

        PnEventInboundUtils.enrichMDC(headers);

        assertEquals("testTraceId", MDC.get(MDCUtils.MDC_TRACE_ID_KEY));
        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
    }

    @Test
    void enrichMDC_withMissingTraceIdAndMessageId() {
        PnEventInboundUtils.enrichMDC(new HashMap<>());

        assertTrue(MDC.get(MDCUtils.MDC_TRACE_ID_KEY).startsWith("traceId:"));
        assertNull(MDC.get(MDCUtils.MDC_PN_CTX_MESSAGE_ID));
    }
}