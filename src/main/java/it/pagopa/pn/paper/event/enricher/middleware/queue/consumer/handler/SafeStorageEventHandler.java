package it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.handler;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.HandleEventUtils;
import it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.PnEventInboundUtils;
import it.pagopa.pn.paper.event.enricher.service.PaperEventEnricherService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.paper.event.enricher.exception.PnPaperEventEnricherExceptionConstant.INVALID_SAFE_STORAGE_EVENT;

@Component
@CustomLog
@RequiredArgsConstructor
public class SafeStorageEventHandler {

    private final PaperEventEnricherService paperEventEnricherService;
    private static final String HANDLER_REQUEST = "safeStorageConsumer";
    private static final String CON020ENRICHED_HASH_KEY = "con020EnrichedHashKey";

    @SqsListener(value = "${pn.paper-event-enricher.sqs.safe-storage-to-paper-event-enricher-queue-name}", acknowledgementMode = SqsListenerAcknowledgementMode.ON_SUCCESS)
    public void safeStorageConsumer(@Payload FileDownloadResponse payload, @Headers Map<String, Object> headers) {
        Instant start = Instant.now();
        PnEventInboundUtils.enrichMDC(headers);
        log.info("Start handling message on [{}] at {}", HANDLER_REQUEST, start);
        log.debug("Handle message from {} with content {}", PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, payload);
        if (StringUtils.hasText(payload.getKey())) {
            MDC.put(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY, payload.getKey());
        }
        Map<String, List<String>> tags = payload.getTags();
        if(CollectionUtils.isEmpty(tags) || !tags.containsKey(CON020ENRICHED_HASH_KEY)){
            throw new PaperEventEnricherException("con020EnrichedHashKey tag is not present", 400, INVALID_SAFE_STORAGE_EVENT);
        }
        String con020EnrichedHashKey = tags.get(CON020ENRICHED_HASH_KEY).getFirst();
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, con020EnrichedHashKey);
        var handledMessage = paperEventEnricherService.handleSafeStorageEvent(con020EnrichedHashKey, payload.getKey())
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
