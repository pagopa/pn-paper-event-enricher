package it.pagopa.pn.paper.event.enricher.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConfigurationProperties( prefix = "pn.paper-event-enricher")
@Data
@Import({SharedAutoConfiguration.class})
public class PnPaperEventEnricherConfig {

    private String safeStorageBaseUrl;
    private Dao dao;
    private Sqs sqs;
    private String cxId;
    private int safeStorageUploadMaxConcurrentRequest;
    private int updateItemMaxConcurrentRequest;
    private boolean pdfTwoPagesEnabled;
    private int pdfPageSize;

    @Data
    public static class Dao {
        private String paperEventEnrichmentTable;
    }

    @Data
    public static class Sqs {
        private String paperArchivesQueueName;
        private String paperEventEnrichmentInputQueueName;
    }



}
