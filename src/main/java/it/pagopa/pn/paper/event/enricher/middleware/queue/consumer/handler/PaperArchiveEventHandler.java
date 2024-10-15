package it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.HandleEventUtils;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperArchiveEvent;
import it.pagopa.pn.paper.event.enricher.service.PaperEventEnricherService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;

@Configuration
@CustomLog
@RequiredArgsConstructor
public class PaperArchiveEventHandler {

    private final PaperEventEnricherService paperEventEnricherService;

    private static final String HANDLER_REQUEST = "pnPaperEventEnricherNewArchiveConsumer";

    @Bean
    public Consumer<Message<PaperArchiveEvent.Payload>> pnPaperEventEnricherNewArchiveConsumer() {
        return message -> {
            Instant start = Instant.now();
            log.debug("Handle message from {} with content {}", PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_CHANNELS, message);
            log.info("Start handling message at: {}", start);
            message.getPayload();
            if(Objects.nonNull(message.getPayload().getArchiveFileKey())) {
                MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, message.getPayload().getArchiveFileKey());
            }
            var handledMessage = paperEventEnricherService.handlePaperEventEnricherEvent(message.getPayload())
                    .doOnSuccess(unused -> {
                        log.logEndingProcess(HANDLER_REQUEST);
                        log.info("End handling message duration: {} ms", Instant.now().toEpochMilli() - start.toEpochMilli());
                    })
                    .doOnError(throwable -> {
                        log.logEndingProcess(HANDLER_REQUEST, false, throwable.getMessage());
                        HandleEventUtils.handleException(message.getHeaders(), throwable);
                    });
            MDCUtils.addMDCToContextAndExecute(handledMessage).block();
        };
    }
}
