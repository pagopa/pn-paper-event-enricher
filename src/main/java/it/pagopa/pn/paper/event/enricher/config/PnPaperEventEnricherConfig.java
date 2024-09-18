package it.pagopa.pn.paper.event.enricher.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConfigurationProperties( prefix = "pn.paper.event.enricher")
@Data
@Import({SharedAutoConfiguration.class})
public class PnPaperEventEnricherConfig {

    private String safeStorageBaseUrl;


    
    @Data
    public static class Topics {

    }



}
