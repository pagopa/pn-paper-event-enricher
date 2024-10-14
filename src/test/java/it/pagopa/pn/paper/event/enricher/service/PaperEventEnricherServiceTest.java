package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.db.Con020ArchiveDao;
import it.pagopa.pn.paper.event.enricher.middleware.db.Con020EnricherDao;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020ArchiveEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperArchiveEvent;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperEventEnricherInputEvent;
import it.pagopa.pn.paper.event.enricher.model.FileCounter;
import it.pagopa.pn.paper.event.enricher.model.FileDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static it.pagopa.pn.paper.event.enricher.model.FileTypeEnum.BIN;
import static org.mockito.Mockito.*;

class PaperEventEnricherServiceTest {

    @Mock
    private Con020ArchiveDao con020ArchiveDao;

    @Mock
    private Con020EnricherDao con020EnricherDao;

    @Mock
    private FileService fileService;

    private PaperEventEnricherService paperEventEnricherService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        PnPaperEventEnricherConfig config = new PnPaperEventEnricherConfig();
        config.setUpdateItemMaxConcurrentRequest(10);
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

        Mockito.when(con020ArchiveDao.putIfAbsent(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(archiveEntity));
        Mockito.when(con020EnricherDao.updateMetadata(any(CON020EnrichedEntity.class))).thenReturn(Mono.just(enricherEntity));

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

        Mockito.when(con020ArchiveDao.putIfAbsent(any(CON020ArchiveEntity.class))).thenReturn(Mono.error(new PaperEventEnricherException("Test exception", 400, "exception")));
        Mockito.when(con020EnricherDao.updateMetadata(any(CON020EnrichedEntity.class))).thenReturn(Mono.just(new CON020EnrichedEntity()));

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

        Mockito.when(con020ArchiveDao.putIfAbsent(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(archiveEntity));
        Mockito.when(con020EnricherDao.updateMetadata(any(CON020EnrichedEntity.class))).thenReturn(Mono.error(new RuntimeException("Test exception")));

        Mono<Void> result = paperEventEnricherService.handleInputEventMessage(inputMessage);

        StepVerifier.create(result)
                .verifyError(RuntimeException.class);
    }

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
        Mockito.when(con020ArchiveDao.putIfAbsent(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(archiveEntity));
        Mockito.when(con020EnricherDao.updateMetadata(any(CON020EnrichedEntity.class))).thenReturn(Mono.just(new CON020EnrichedEntity()));

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
        Mockito.when(con020ArchiveDao.putIfAbsent(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(archiveEntity));
        Mockito.when(con020EnricherDao.updateMetadata(any(CON020EnrichedEntity.class))).thenReturn(Mono.just(new CON020EnrichedEntity()));

        Mono<Void> result = paperEventEnricherService.handleInputEventMessage(inputMessage);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void handlePaperEventEnricherEventWithValidPayload() {
        PaperArchiveEvent.Payload payload = mock(PaperArchiveEvent.Payload.class);
        Mockito.when(payload.getArchiveFileKey()).thenReturn("validArchiveFileKey");
        Path path = mock(Path.class);

        Mockito.when(fileService.createTmpFile("validArchiveFileKey", BIN.getValue())).thenReturn(path);
        Mockito.when(con020ArchiveDao.updateIfExists(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(mock(CON020ArchiveEntity.class)));
        Mockito.when(fileService.downloadFile("validArchiveFileKey", path)).thenReturn(Flux.empty());
        Mockito.when(fileService.extractFileFromBin(path)).thenReturn(Mono.just(path));
        FileDetail fileDetail = FileDetail.builder().fileKey("fileKey").filename("filename.pdf").build();
        Mockito.when(fileService.extractFileFromArchive(eq(path), any(), any())).thenReturn(Flux.just(fileDetail));
        doNothing().when(fileService).deleteFileTmp(any());
        Mockito.when(con020EnricherDao.updatePrintedPdf(any())).thenReturn(Mono.just(new CON020EnrichedEntity()));
        Mockito.when(con020ArchiveDao.updateIfExists(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(mock(CON020ArchiveEntity.class)));

        Mono<CON020ArchiveEntity> result = paperEventEnricherService.handlePaperEventEnricherEvent(payload);

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void handlePaperEventEnricherEventWithDownloadError() {
        PaperArchiveEvent.Payload payload = mock(PaperArchiveEvent.Payload.class);
        Mockito.when(payload.getArchiveFileKey()).thenReturn("archiveFileKey");

        Path path = mock(Path.class);
        Mockito.when(fileService.createTmpFile("archiveFileKey", BIN.getValue())).thenReturn(path);
        Mockito.when(con020ArchiveDao.updateIfExists(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(mock(CON020ArchiveEntity.class)));
        Mockito.when(fileService.downloadFile("archiveFileKey", path)).thenReturn(Flux.error(new RuntimeException("Download error")));

        Mono<CON020ArchiveEntity> result = paperEventEnricherService.handlePaperEventEnricherEvent(payload);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Download error"))
                .verify();
    }

    @Test
    void handlePaperEventEnricherEventWithExtractionError() {
        PaperArchiveEvent.Payload payload = mock(PaperArchiveEvent.Payload.class);
        Mockito.when(payload.getArchiveFileKey()).thenReturn("archiveFileKey");

        Path path = mock(Path.class);
        Mockito.when(fileService.createTmpFile("archiveFileKey", BIN.getValue())).thenReturn(path);
        Mockito.when(con020ArchiveDao.updateIfExists(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(mock(CON020ArchiveEntity.class)));
        Mockito.when(fileService.downloadFile("archiveFileKey", path)).thenReturn(Flux.empty());
        Mockito.when(fileService.extractFileFromBin(path)).thenReturn(Mono.error(new RuntimeException("Extraction error")));

        Mono<CON020ArchiveEntity> result = paperEventEnricherService.handlePaperEventEnricherEvent(payload);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Extraction error"))
                .verify();
    }

    @Test
    void handlePaperEventEnricherEventWithUpdateError() {
        PaperArchiveEvent.Payload payload = mock(PaperArchiveEvent.Payload.class);
        Mockito.when(payload.getArchiveFileKey()).thenReturn("archiveFileKey");

        Path path = mock(Path.class);
        Mockito.when(fileService.createTmpFile("archiveFileKey", BIN.getValue())).thenReturn(path);
        Mockito.when(con020ArchiveDao.updateIfExists(any(CON020ArchiveEntity.class))).thenReturn(Mono.just(mock(CON020ArchiveEntity.class)));
        Mockito.when(fileService.downloadFile("archiveFileKey", path)).thenReturn(Flux.empty());
        Mockito.when(fileService.extractFileFromBin(path)).thenReturn(Mono.just(path));
        Mockito.when(fileService.extractFileFromArchive(path, new HashMap<>(), new FileCounter(new AtomicInteger(0), new AtomicInteger(0), 0))).thenReturn(Flux.empty());
        Mockito.when(con020ArchiveDao.updateIfExists(any(CON020ArchiveEntity.class))).thenReturn(Mono.error(new RuntimeException("Update error")));

        Mono<CON020ArchiveEntity> result = paperEventEnricherService.handlePaperEventEnricherEvent(payload);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Update error"))
                .verify();
    }
}