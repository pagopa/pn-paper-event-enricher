package it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paper.event.enricher.middleware.queue.consumer.HandleEventUtils;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperEventEnricherInputEvent;
import it.pagopa.pn.paper.event.enricher.service.PaperEventEnricherService;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.Objects;
import java.util.function.Consumer;

@Configuration
@CustomLog
public class PaperEventEnricherInputEventHandler {

    private final PaperEventEnricherService paperEventEnricherService;

    private static final String HANDLER_REQUEST = "pnPaperEventEnricherConsumer";


    public PaperEventEnricherInputEventHandler(PaperEventEnricherService paperEventEnricherInputService) {
        this.paperEventEnricherService = paperEventEnricherInputService;
    }

    @Bean
    public Consumer<Message<PaperEventEnricherInputEvent.Payload>> pnPaperEventEnricherConsumer() {
        return message -> {
            log.debug("Handle message from {} with content {}", PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_CHANNELS, message);
            PaperEventEnricherInputEvent.Payload response = message.getPayload();
            if(Objects.nonNull(response.getAnalogMail())) {
                MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, response.getAnalogMail().getRequestId());
            }
            var handledMessage = paperEventEnricherService.handleInputEventMessage(response)
                    .doOnSuccess(unused -> log.logEndingProcess(HANDLER_REQUEST))
                    .doOnError(throwable -> {
                        log.logEndingProcess(HANDLER_REQUEST, false, throwable.getMessage());
                        HandleEventUtils.handleException(message.getHeaders(), throwable);
                    });
            MDCUtils.addMDCToContextAndExecute(handledMessage).block();
        };
    }
}
