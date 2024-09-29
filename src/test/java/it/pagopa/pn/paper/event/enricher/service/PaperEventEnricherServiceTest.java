package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.db.Con020ArchiveDao;
import it.pagopa.pn.paper.event.enricher.middleware.db.Con020EnricherDao;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020ArchiveEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperEventEnricherInputEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

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
        paperEventEnricherService = new PaperEventEnricherService(con020ArchiveDao, con020EnricherDao, fileService);
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
}