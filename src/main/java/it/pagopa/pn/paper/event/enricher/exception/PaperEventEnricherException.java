package it.pagopa.pn.paper.event.enricher.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import lombok.Getter;

@Getter
public class PaperEventEnricherException extends PnRuntimeException {

    private final String code;

    public PaperEventEnricherException(String message, int status, String code){
        super(message, message, status, code, null, null);
        this.code = code;
    }

}
