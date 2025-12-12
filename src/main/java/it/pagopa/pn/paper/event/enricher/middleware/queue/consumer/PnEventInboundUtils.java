package it.pagopa.pn.paper.event.enricher.middleware.queue.consumer;

import it.pagopa.pn.commons.utils.MDCUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor(access = AccessLevel.NONE)
public class PnEventInboundUtils {

    public static void enrichMDC(Map<String, Object> headers) {
        String traceId = (String) headers.getOrDefault("X-Amzn-Trace-Id", "traceId:" + UUID.randomUUID());
        String messageId = (String) headers.get("aws_messageId");

        MDCUtils.clearMDCKeys();
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);
        MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, messageId);
    }

}
