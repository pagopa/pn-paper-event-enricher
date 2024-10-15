package it.pagopa.pn.paper.event.enricher.middleware.queue.consumer;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.UUID;

import static it.pagopa.pn.paper.event.enricher.exception.PnPaperEventEnricherExceptionConstant.ERROR_CODE_PAPER_EVENT_ENRICHER_EVENTTYPENOTSUPPORTED;
import static it.pagopa.pn.paper.event.enricher.exception.PnPaperEventEnricherExceptionConstant.ERROR_MESSAGE_PAPER_EVENT_ENRICHER_EVENTTYPENOTSUPPORTED;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class PnEventInboundService {
    private final EventHandler eventHandler;
    private final PnPaperEventEnricherConfig pnPaperEventEnricherConfig;

    @Bean
    public MessageRoutingCallback customRouter() {
        return new MessageRoutingCallback() {
            @Override
            public FunctionRoutingResult routingResult(Message<?> message) {
                MessageHeaders messageHeaders = message.getHeaders();

                String traceId = null;
                String messageId = null;

                if (messageHeaders.containsKey("aws_messageId"))
                    messageId = messageHeaders.get("aws_messageId", String.class);
                if (messageHeaders.containsKey("X-Amzn-Trace-Id"))
                    traceId = messageHeaders.get("X-Amzn-Trace-Id", String.class);

                traceId = Objects.requireNonNullElseGet(traceId, () -> "traceId:" + UUID.randomUUID());
                MDCUtils.clearMDCKeys();
                MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);
                MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, messageId);
                return new FunctionRoutingResult(handleMessage(message));
            }
        };
    }

    private String handleMessage(Message<?> message) {
        log.debug("Message received from customRouter {}", message);
        String eventType = routeBasedOnQueue(message);

        String handlerName = eventHandler.getHandler().get(eventType);
        if (!StringUtils.hasText(handlerName)) {
            log.error("undefined handler for eventType={}", eventType);
        }

        log.debug("Handler for eventType={} is {}", eventType, handlerName);

        return handlerName;
    }

    private String routeBasedOnQueue(Message<?> message) {
        String eventType;
        String queueName = (String) message.getHeaders().get("aws_receivedQueue");
        if (Objects.equals(queueName, pnPaperEventEnricherConfig.getSqs().getPaperEventEnrichmentInputQueueName())) {
            eventType = "PAPER_EVENT_ENRICHER_INPUT_EVENTS";
        }
        else if(Objects.equals(queueName, pnPaperEventEnricherConfig.getSqs().getPaperArchivesQueueName())) {
            eventType = "PAPER_EVENT_ENRICHER_ARCHIVES_EVENTS";
        }
        else {
            log.error("No handler found for queue: {}", queueName);
            throw new PnInternalException(ERROR_MESSAGE_PAPER_EVENT_ENRICHER_EVENTTYPENOTSUPPORTED, ERROR_CODE_PAPER_EVENT_ENRICHER_EVENTTYPENOTSUPPORTED);
        }
        return eventType;
    }
}
