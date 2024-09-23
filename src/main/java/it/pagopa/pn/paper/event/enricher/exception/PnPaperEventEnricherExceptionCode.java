package it.pagopa.pn.paper.event.enricher.exception;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PnPaperEventEnricherExceptionCode {
    public static final String ERROR_CODE_PAPER_EVENT_ENRICHER_EVENTTYPENOTSUPPORTED = "PN_PAPER_EVENT_ENRICHER_EVENTTYPENOTSUPPORTED";
    public static final String ERROR_MESSAGE_PAPER_EVENT_ENRICHER_EVENTTYPENOTSUPPORTED = "eventType not present, cannot start scheduled action.";

    public static final String ERROR_CODE_INVALID_REQUESTID = "PN_PAPER_EVENT_ENRICHER_INVALID_REQUESTID";
}
