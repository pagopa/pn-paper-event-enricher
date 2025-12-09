package it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.handler;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.HandleEventUtils;
import it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.PnEventInboundUtils;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperEventEnricherInputEvent;
import it.pagopa.pn.paper.event.enricher.service.PaperEventEnricherService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Component
@CustomLog
@RequiredArgsConstructor
public class PaperInputEventHandler {

    private final PaperEventEnricherService paperEventEnricherService;
    private static final String HANDLER_REQUEST = "paperInputConsumer";

    @SqsListener(value = "${pn.paper-event-enricher.sqs.paper-event-enrichment-input-queue-name}",
            maxConcurrentMessages = "${spring.cloud.aws.sqs.paperInputConsumer.max-concurrent-messages}",
            maxMessagesPerPoll = "${spring.cloud.aws.sqs.paperInputConsumer.max-messages-per-poll}",
            acknowledgementMode = SqsListenerAcknowledgementMode.ON_SUCCESS)
    public void paperInputConsumer(@Payload PaperEventEnricherInputEvent.Payload payload, @Headers Map<String, Object> headers) {
        Instant start = Instant.now();
        PnEventInboundUtils.enrichMDC(headers);
        log.info("Start handling message on [{}] at {}", HANDLER_REQUEST, start);
        log.debug("Handle message from {} with content {}", PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_CHANNELS, payload);
        if (Objects.nonNull(payload.getAnalogMail())) {
            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, payload.getAnalogMail().getRequestId());
        }
        var handledMessage = paperEventEnricherService.handleInputEventMessage(payload)
                .doOnSuccess(unused -> {
                    log.logEndingProcess(HANDLER_REQUEST);
                    log.info("End handling message on [{}], duration: {} ms", HANDLER_REQUEST, Instant.now().toEpochMilli() - start.toEpochMilli());
                }).doOnError(throwable -> {
                    log.logEndingProcess(HANDLER_REQUEST, false, throwable.getMessage());
                    HandleEventUtils.handleException(headers, throwable);
                });
        MDCUtils.addMDCToContextAndExecute(handledMessage).block();
    }
}
