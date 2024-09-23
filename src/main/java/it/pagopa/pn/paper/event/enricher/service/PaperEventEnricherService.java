package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.db.Con020ArchiveDao;
import it.pagopa.pn.paper.event.enricher.middleware.db.Con020EnricherDao;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020BaseEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperArchiveEvent;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperEventEnricherInputEvent;
import it.pagopa.pn.paper.event.enricher.model.CON020ArchiveStatusEnum;
import it.pagopa.pn.paper.event.enricher.model.FileDetail;
import it.pagopa.pn.paper.event.enricher.model.IndexData;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
        String archiveFileKey = payload.getArchiveFileKey();
        return con020ArchiveDao.updateIfExists(createArchiveEntityForStatusUpdate(payload, CON020ArchiveStatusEnum.PROCESSING.name()))
                .flatMap(con020ArchiveEntity -> fileService.downloadFile(archiveFileKey))
                .doOnNext(bytes -> log.info("Downloaded file with key: {}", archiveFileKey))
                .flatMapMany(this::extractP7MFromBin)
                .flatMap(p7mDetail -> fileService.extractFilesFromArchive(new ZipArchiveInputStream(new ByteArrayInputStream(p7mDetail)), indexDataMap))
                .flatMap(fileDetail -> updatePrintedPdf(fileDetail, indexDataMap, archiveFileKey))
                .collectList()
                .flatMap(con020EnrichedEntities -> con020ArchiveDao.updateIfExists(createArchiveEntityForStatusUpdate(payload, CON020ArchiveStatusEnum.PROCESSED.name())))
                .then();
    }

    private Mono<String> updatePrintedPdf(FileDetail fileDetail, Map<String, IndexData> indexDataMap, String archiveFileKey) {
        IndexData indexData = indexDataMap.get(fileDetail.getFilename());
        if (Objects.nonNull(indexData)) {
            CON020EnrichedEntity con020EnrichedEntity = createEnricherEntityForPrintedPdf(fileDetail.getFileKey(), archiveFileKey, indexData.getRequestId(), indexData.getRegisteredLetterCode());
            return con020EnricherDao.updatePrintedPdf(con020EnrichedEntity)
                    .doOnError(throwable -> log.error("Error during update Item: {}", throwable.getMessage(), throwable))
                    .map(CON020BaseEntity::getHashKey);
        }
        log.info("Index data not found for file: {}", fileDetail.getFilename());
        return Mono.just("empty");
    }

    private Flux<byte[]> extractP7MFromBin(byte[] bytes) {
        return fileService.extractFilesFromArchive(new ZipArchiveInputStream(new ByteArrayInputStream(bytes)), null)
                .doOnNext(fileDetail -> log.info("Extracted file: {}", fileDetail.getFilename()))
                .map(fileDetail -> extractFromP7m(fileDetail.getContent()));
    }
}
