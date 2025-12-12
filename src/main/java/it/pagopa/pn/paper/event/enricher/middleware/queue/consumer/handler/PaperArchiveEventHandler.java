package it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.handler;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.HandleEventUtils;
import it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.PnEventInboundUtils;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperArchiveEvent;
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
public class PaperArchiveEventHandler {

    private final PaperEventEnricherService paperEventEnricherService;
    private static final String HANDLER_REQUEST = "paperArchiveConsumer";

    @SqsListener(value = "${pn.paper-event-enricher.sqs.paper-archives-queue-name}",
            maxConcurrentMessages = "${pn.paper-event-enricher.sqs.archive-queue-concurrency}",
            maxMessagesPerPoll = "${pn.paper-event-enricher.sqs.archive-queue-max-number-of-messages}",
            acknowledgementMode = SqsListenerAcknowledgementMode.ON_SUCCESS)
    public void paperArchiveConsumer(@Payload PaperArchiveEvent.Payload payload, @Headers Map<String, Object> headers) {
        Instant start = Instant.now();
        PnEventInboundUtils.enrichMDC(headers);
        log.debug("Handle message from {} with content {}", PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_CHANNELS, payload);
        log.info("Start handling message on [{}] at {}", HANDLER_REQUEST, start);
        if (Objects.nonNull(payload.getArchiveFileKey())) {
            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, payload.getArchiveFileKey());
        }
        var handledMessage = paperEventEnricherService.handlePaperEventEnricherEvent(payload)
                .doOnSuccess(unused -> {
                    log.logEndingProcess(HANDLER_REQUEST);
                    log.info("End handling message on [{}], duration: {} ms", HANDLER_REQUEST, Instant.now().toEpochMilli() - start.toEpochMilli());
                })
                .doOnError(throwable -> {
                    log.logEndingProcess(HANDLER_REQUEST, false, throwable.getMessage());
                    HandleEventUtils.handleException(headers, throwable);
                });
        MDCUtils.addMDCToContextAndExecute(handledMessage).block();
    }
}
