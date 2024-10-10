package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.db.Con020ArchiveDao;
import it.pagopa.pn.paper.event.enricher.middleware.db.Con020EnricherDao;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020ArchiveEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperArchiveEvent;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperEventEnricherInputEvent;
import it.pagopa.pn.paper.event.enricher.model.FileDetail;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.Mockito.*;

class PaperEventEnricherServiceTest {

    @Mock
    private Con020ArchiveDao con020ArchiveDao;

    @Mock
    private Con020EnricherDao con020EnricherDao;

    @Mock
    private FileService fileService;

    @Mock
    private PnPaperEventEnricherConfig config;

    private PaperEventEnricherService paperEventEnricherService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(config.getSafeStorageUploadMaxConcurrentRequest()).thenReturn(10);
        paperEventEnricherService = new PaperEventEnricherService(con020ArchiveDao, con020EnricherDao, fileService, config);
    }

    @Test
    void testHandleInputEventMessageSuccess() {
        PaperEventEnricherInputEvent.Payload inputMessage = PaperEventEnricherInputEvent.Payload.builder()
                .analogMail(PaperEventEnricherInputEvent.Payload.AnalogMailDetail.builder()
                        .requestId("PREPARE_SIMPLE_REGISTERED_LETTER.IUN_NATN-YGVJ-YXAG-202402-R-1.RECINDEX_0.PCRETRY_0")
                        .attachments(List.of(PaperEventEnricherInputEvent.Payload.Attachment.builder()
                                .uri("testUri")
                                .build()))
                        .build())
                .build();
        CON020ArchiveEntity archiveEntity = mock(CON020ArchiveEntity.class);
        CON020EnrichedEntity enricherEntity = mock(CON020EnrichedEntity.class);

        when(con020ArchiveDao.putIfAbsent(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(archiveEntity));
        when(con020EnricherDao.updateMetadata(any(CON020EnrichedEntity.class))).thenReturn(Mono.just(enricherEntity));

        Mono<Void> result = paperEventEnricherService.handleInputEventMessage(inputMessage);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void testHandleInputEventMessageArchiveDaoAlreadyExists() {
        PaperEventEnricherInputEvent.Payload inputMessage = PaperEventEnricherInputEvent.Payload.builder()
                .analogMail(PaperEventEnricherInputEvent.Payload.AnalogMailDetail.builder()
                        .attachments(List.of(PaperEventEnricherInputEvent.Payload.Attachment.builder()
                                .uri("testUri")
                                .build()))
                        .build())
                .build();

        when(con020ArchiveDao.putIfAbsent(any(CON020ArchiveEntity.class))).thenReturn(Mono.error(new PaperEventEnricherException("Test exception", 400, "exception")));
        when(con020EnricherDao.updateMetadata(any(CON020EnrichedEntity.class))).thenReturn(Mono.just(new CON020EnrichedEntity()));

        Mono<Void> result = paperEventEnricherService.handleInputEventMessage(inputMessage);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void testHandleInputEventMessageEnricherDaoError() {
        PaperEventEnricherInputEvent.Payload inputMessage = PaperEventEnricherInputEvent.Payload.builder()
                .analogMail(PaperEventEnricherInputEvent.Payload.AnalogMailDetail.builder()
                        .requestId("PREPARE_SIMPLE_REGISTERED_LETTER.IUN_NATN-YGVJ-YXAG-202402-R-1.RECINDEX_0.PCRETRY_0")
                        .attachments(List.of(PaperEventEnricherInputEvent.Payload.Attachment.builder()
                                .uri("testUri")
                                .build()))
                        .build())
                .build();
        CON020ArchiveEntity archiveEntity = mock(CON020ArchiveEntity.class);

        when(con020ArchiveDao.putIfAbsent(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(archiveEntity));
        when(con020EnricherDao.updateMetadata(any(CON020EnrichedEntity.class))).thenReturn(Mono.error(new RuntimeException("Test exception")));

        Mono<Void> result = paperEventEnricherService.handleInputEventMessage(inputMessage);

        StepVerifier.create(result)
                .verifyError(RuntimeException.class);
    }

    @Test
    void testHandleInputEventMessageInvalidUriError() {
        PaperEventEnricherInputEvent.Payload inputMessage = PaperEventEnricherInputEvent.Payload.builder()
                .analogMail(PaperEventEnricherInputEvent.Payload.AnalogMailDetail.builder()
                        .requestId("PREPARE_SIMPLE_REGISTERED_LETTER.IUN_NATN-YGVJ-YXAG-202402-R-1.RECINDEX_0.PCRETRY_0")
                        .attachments(List.of(PaperEventEnricherInputEvent.Payload.Attachment.builder()
                                .build()))
                        .build())
                .build();

        Mono<Void> result = paperEventEnricherService.handleInputEventMessage(inputMessage);

        StepVerifier.create(result).verifyComplete();    }

    @Test
    void testHandleInputEventMessageInvalidIunError() {
        PaperEventEnricherInputEvent.Payload inputMessage = PaperEventEnricherInputEvent.Payload.builder()
                .analogMail(PaperEventEnricherInputEvent.Payload.AnalogMailDetail.builder()
                        .requestId("PREPARE_SIMPLE_REGISTERED_LETTER.IUN_.RECINDEX_0.PCRETRY_0")
                        .attachments(List.of(PaperEventEnricherInputEvent.Payload.Attachment.builder()
                                .uri("testUri")
                                .build()))
                        .build())
                .build();

        CON020ArchiveEntity archiveEntity = mock(CON020ArchiveEntity.class);
        when(con020ArchiveDao.putIfAbsent(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(archiveEntity));
        when(con020EnricherDao.updateMetadata(any(CON020EnrichedEntity.class))).thenReturn(Mono.just(new CON020EnrichedEntity()));

        Mono<Void> result = paperEventEnricherService.handleInputEventMessage(inputMessage);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    void testHandleInputEventMessageInvalidRecIndexError() {
        PaperEventEnricherInputEvent.Payload inputMessage = PaperEventEnricherInputEvent.Payload.builder()
                .analogMail(PaperEventEnricherInputEvent.Payload.AnalogMailDetail.builder()
                        .requestId("PREPARE_SIMPLE_REGISTERED_LETTER.IUN_NATN-YGVJ-YXAG-202402-R-1.RECINDEX_.PCRETRY_0")
                        .attachments(List.of(PaperEventEnricherInputEvent.Payload.Attachment.builder()
                                .uri("testUri")
                                .build()))
                        .build())
                .build();

        CON020ArchiveEntity archiveEntity = mock(CON020ArchiveEntity.class);
        when(con020ArchiveDao.putIfAbsent(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(archiveEntity));
        when(con020EnricherDao.updateMetadata(any(CON020EnrichedEntity.class))).thenReturn(Mono.just(new CON020EnrichedEntity()));

        Mono<Void> result = paperEventEnricherService.handleInputEventMessage(inputMessage);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void handlePaperEventEnricherEventSuccess() {
        PaperArchiveEvent.Payload payload = mock(PaperArchiveEvent.Payload.class);
        when(payload.getArchiveFileKey()).thenReturn("testArchiveFileKey");

        CON020ArchiveEntity archiveEntity = mock(CON020ArchiveEntity.class);
        when(con020ArchiveDao.updateIfExists(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(archiveEntity));
        when(fileService.retrieveDownloadUrl(anyString())).thenReturn(Mono.just("testDownloadUrl"));
        when(fileService.downloadFile(anyString())).thenReturn(Flux.just("testBytes".getBytes()));
        when(fileService.extractFilesFromArchive(any(ZipArchiveInputStream.class), anyMap()))
                .thenReturn(Flux.just(new FileDetail("testFile.pdz.p7m", new ByteArrayInputStream("testContent".getBytes()), null, "testContent".getBytes(StandardCharsets.UTF_8))));
        when(fileService.extractFilesFromArchiveP7m(any(ZipArchiveInputStream.class), anyMap()))
                .thenReturn(List.of(new FileDetail("testFile.pdf", new ByteArrayInputStream("testContent".getBytes()), null, "testContent".getBytes(StandardCharsets.UTF_8))));
        when(fileService.uploadPdf(any(byte[].class), anyString())).thenReturn(Mono.just("testFileKey"));
        when(con020ArchiveDao.updateIfExists(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(archiveEntity));

        Mono<Void> result = paperEventEnricherService.handlePaperEventEnricherEvent(payload);

        StepVerifier.create(result).verifyComplete();
    }

    @Test
    void handlePaperEventEnricherEventDownloadError() {
        PaperArchiveEvent.Payload payload = mock(PaperArchiveEvent.Payload.class);
        when(payload.getArchiveFileKey()).thenReturn("testArchiveFileKey");

        when(con020ArchiveDao.updateIfExists(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(mock(CON020ArchiveEntity.class)));
        when(fileService.retrieveDownloadUrl(anyString())).thenReturn(Mono.error(new RuntimeException("Download error")));

        Mono<Void> result = paperEventEnricherService.handlePaperEventEnricherEvent(payload);

        StepVerifier.create(result).verifyError(RuntimeException.class);
    }

    @Test
    void handlePaperEventEnricherEventExtractionError() {
        PaperArchiveEvent.Payload payload = mock(PaperArchiveEvent.Payload.class);
        when(payload.getArchiveFileKey()).thenReturn("testArchiveFileKey");

        when(con020ArchiveDao.updateIfExists(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(mock(CON020ArchiveEntity.class)));
        when(fileService.retrieveDownloadUrl(anyString())).thenReturn(Mono.just("testDownloadUrl"));
        when(fileService.downloadFile(anyString())).thenReturn(Flux.just("testBytes".getBytes()));
        when(fileService.extractFilesFromArchive(any(ZipArchiveInputStream.class), anyMap())).thenReturn(Flux.error(new RuntimeException("Extraction error")));

        Mono<Void> result = paperEventEnricherService.handlePaperEventEnricherEvent(payload);

        StepVerifier.create(result).verifyError(RuntimeException.class);
    }
}