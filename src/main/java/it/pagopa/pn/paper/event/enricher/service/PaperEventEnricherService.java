package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.db.Con020ArchiveDao;
import it.pagopa.pn.paper.event.enricher.middleware.db.Con020EnricherDao;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperArchiveEvent;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperEventEnricherInputEvent;
import it.pagopa.pn.paper.event.enricher.model.CON020ArchiveStatusEnum;
import it.pagopa.pn.paper.event.enricher.model.IndexData;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static it.pagopa.pn.paper.event.enricher.model.FileTypeEnum.PDF;
import static it.pagopa.pn.paper.event.enricher.utils.PaperEventEnricherUtils.*;

@Service
@CustomLog
@RequiredArgsConstructor
public class PaperEventEnricherService {

    private final Con020ArchiveDao con020ArchiveDao;
    private final Con020EnricherDao con020EnricherDao;
    private final FileService fileService;

    public Mono<Void> handleInputEventMessage(PaperEventEnricherInputEvent.Payload payload) {
        return createArchiveEntity(payload.getAnalogMail())
                .flatMap(con020ArchiveEntity -> con020ArchiveDao.putIfAbsent(con020ArchiveEntity)
                        .doOnError(throwable -> {
                            log.warn("Error while creating archive entity: {}", throwable.getMessage());
                            log.warn("Error: ", throwable);
                        })
                        .onErrorReturn(PaperEventEnricherException.class, con020ArchiveEntity))
                .flatMap(con020ArchiveEntity -> createEnricherEntity(payload))
                .flatMap(con020EnricherDao::updateMetadata)
                .then()
                .doOnError(throwable -> log.error("Unexpected error while creating entities: {}", throwable.getMessage(), throwable))
                .onErrorResume(PaperEventEnricherException.class, throwable -> Mono.empty());
    }

    public Mono<Void> handlePaperEventEnricherEvent(PaperArchiveEvent.Payload payload) {
        Map<String, IndexData> indexDataMap = new HashMap<>();
        Map<String, String> pdfFileKeyMap = new HashMap<>();
        String archiveFileKey = payload.getArchiveFileKey();
      /*  return con020ArchiveDao.updateIfExists(createArchiveEntityForStatusUpdate(payload, CON020ArchiveStatusEnum.PROCESSING.name()))
                .flatMap(con020ArchiveEntity ->*/

        return fileService.downloadFile(archiveFileKey)
                .doOnNext(bytes -> log.info("Downloaded file with key: {}", archiveFileKey))
                .flatMap(bytes -> fileService.extractFilesFromArchive(new ByteArrayInputStream(bytes), indexDataMap)
                        .flatMap(p7mDetail -> fileService.extractFilesFromArchive(new ByteArrayInputStream(p7mDetail.getContent()), indexDataMap)
                                .flatMap(fileDetail -> {
                                    if (fileDetail.getFilename().endsWith(PDF.getValue())) {
                                        return Mono.just(UUID.randomUUID().toString());
                                        /*return fileService.uploadPdf(fileDetail.getContent())
                                                .map(s -> pdfFileKeyMap.put(fileDetail.getFilename(), s));*/
                                    }
                                    return null;
                                }))
                        .collectList())
                        .flatMapMany(list -> updateEnrichedEntity(pdfFileKeyMap, indexDataMap, archiveFileKey))
                        .then();
    }


    private Flux<CON020EnrichedEntity> updateEnrichedEntity(Map<String, String> pdfFileKeyMap, Map<String, IndexData> indexDataMap, String archiveFileKey) {
        return Flux.fromIterable(pdfFileKeyMap.entrySet())
                .flatMap(stringStringEntry -> {
                    IndexData indexData = indexDataMap.get(stringStringEntry.getKey());
                    if (Objects.nonNull(indexData)) {
                        CON020EnrichedEntity con020EnrichedEntity =
                                createEnricherEntityForPrintedPdf(stringStringEntry.getValue(), archiveFileKey, indexData.getRequestId(), indexData.getRegisteredLetterCode());
                        return con020EnricherDao.updatePrintedPdf(con020EnrichedEntity)
                                .doOnError(throwable -> log.error("Error during uoload pdf and update Item: {}", throwable.getMessage(), throwable));
                    }
                    return Mono.empty();
                });
    }
}
