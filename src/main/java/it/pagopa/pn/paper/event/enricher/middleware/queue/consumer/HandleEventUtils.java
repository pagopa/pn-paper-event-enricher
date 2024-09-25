package it.pagopa.pn.paper.event.enricher.middleware.queue.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;

@Slf4j
public class HandleEventUtils {
    private static final String PAPER_EVENT_ASYNC = "PAPER_EVENT_ENRICHER ASYNC - ";

    private HandleEventUtils() {
    }

    public static void handleException(MessageHeaders headers, Throwable t) {
        if (headers != null) {
            log.error(PAPER_EVENT_ASYNC + "Generic exception ex= {}", t.getMessage(), t);
        } else {
            log.error(PAPER_EVENT_ASYNC + "Generic exception ex ", t);
        }
    }
}
