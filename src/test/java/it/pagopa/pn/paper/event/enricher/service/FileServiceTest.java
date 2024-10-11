package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage.UploadDownloadClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import static org.mockito.Mockito.mock;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileServiceTest {

    FileService fileService;

    SafeStorageService safeStorageService;
    UploadDownloadClient uploadDownloadClient;
    PnPaperEventEnricherConfig config;

    @BeforeAll
    void setUp() {
        safeStorageService = mock(SafeStorageService.class);
        uploadDownloadClient = mock(UploadDownloadClient.class);
        config = new PnPaperEventEnricherConfig();
        fileService = new FileService(safeStorageService, uploadDownloadClient, config);
    }

}
