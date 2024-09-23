package it.pagopa.pn.paper.event.enricher.exception;



import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class PnSafeStorageException extends Exception {

    private final WebClientResponseException webClientEx;

    public PnSafeStorageException(WebClientResponseException webClientEx){
        super(StringUtils.hasText(webClientEx.getMessage()) ? webClientEx.getMessage() : "Web Client Generic Error");
        this.webClientEx = webClientEx;
    }

    public WebClientResponseException getWebClientEx() {
        return webClientEx;
    }

}