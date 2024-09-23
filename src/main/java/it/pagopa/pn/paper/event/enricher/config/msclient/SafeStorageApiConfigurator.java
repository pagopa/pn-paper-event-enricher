package it.pagopa.pn.paper.event.enricher.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.ApiClient;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.api.FileDownloadApi;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.api.FileMetadataUpdateApi;
import it.pagopa.pn.paper.event.enricher.generated.openapi.msclient.safestorage.api.FileUploadApi;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SafeStorageApiConfigurator extends CommonBaseClient {

    @Bean
    public FileUploadApi fileUploadApi(PnPaperEventEnricherConfig cfg){
        return new FileUploadApi( getNewApiClient(cfg) );
    }

    @Bean
    public FileDownloadApi fileDownloadApi(PnPaperEventEnricherConfig cfg){
        return new FileDownloadApi( getNewApiClient(cfg) );
    }

    @Bean
    public FileMetadataUpdateApi fileMetadataUpdateApi(PnPaperEventEnricherConfig cfg){
        return new FileMetadataUpdateApi( getNewApiClient(cfg) );
    }
    
    @NotNull
    private ApiClient getNewApiClient(PnPaperEventEnricherConfig cfg) {
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
        newApiClient.setBasePath( cfg.getSafeStorageBaseUrl() );
        return newApiClient;
    }
}
