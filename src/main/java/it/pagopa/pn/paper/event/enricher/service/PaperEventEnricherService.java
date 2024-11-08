package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.db.Con020ArchiveDao;
import it.pagopa.pn.paper.event.enricher.middleware.db.Con020EnricherDao;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020ArchiveEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020BaseEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperArchiveEvent;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperEventEnricherInputEvent;
import it.pagopa.pn.paper.event.enricher.model.CON020ArchiveStatusEnum;
import it.pagopa.pn.paper.event.enricher.model.FileCounter;
import it.pagopa.pn.paper.event.enricher.model.FileDetail;
import it.pagopa.pn.paper.event.enricher.model.IndexData;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static it.pagopa.pn.paper.event.enricher.model.FileTypeEnum.BIN;
import static it.pagopa.pn.paper.event.enricher.model.FileTypeEnum.PDF;
import static it.pagopa.pn.paper.event.enricher.utils.PaperEventEnricherUtils.*;

@Service
@CustomLog
@RequiredArgsConstructor
public class PaperEventEnricherService {

    private final Con020ArchiveDao con020ArchiveDao;
    private final Con020EnricherDao con020EnricherDao;
    private final FileService fileService;
    private final PnPaperEventEnricherConfig config;


    public Mono<Void> handleInputEventMessage(PaperEventEnricherInputEvent.Payload payload) {
        return Mono.just(createArchiveEntity(payload.getAnalogMail()))
                .flatMap(con020ArchiveEntity -> con020ArchiveDao.putIfAbsent(con020ArchiveEntity)
                        .doOnNext(entity -> log.info("Created archive entity: {}", entity.getHashKey()))
                        .doOnError(throwable -> log.warn("Error while creating archive entity: {}", throwable.getMessage()))
                        .onErrorReturn(PaperEventEnricherException.class, con020ArchiveEntity))
                .map(con020ArchiveEntity -> createEnricherEntityForMetadata(payload))
                .flatMap(con020EnricherDao::updateMetadata)
                .doOnNext(entity -> log.info("Updated CON020EnrichedEntity: {}", entity.getHashKey()))
                .then()
                .doOnError(throwable -> log.error("Unexpected error while creating entities: {}", throwable.getMessage(), throwable))
                .onErrorResume(PaperEventEnricherException.class, throwable -> Mono.empty());
    }

    public Mono<CON020ArchiveEntity> handlePaperEventEnricherEvent(PaperArchiveEvent.Payload payload) {
        Map<String, IndexData> indexDataMap = new HashMap<>();
        String archiveFileKey = payload.getArchiveFileKey();
        Path path = fileService.createTmpFile(archiveFileKey, BIN.getValue());
        FileCounter fileCounter = new FileCounter(new AtomicInteger(0), new AtomicInteger(0), 0);

        return con020ArchiveDao.updateIfExists(createArchiveEntityForStatusUpdate(payload, CON020ArchiveStatusEnum.PROCESSING.name(), fileCounter))
                .doOnNext(con020ArchiveEntity -> log.info("Updated archive entity  [{}] to PROCESSING", con020ArchiveEntity.getHashKey()))
                .flatMapMany(con020ArchiveEntity -> fileService.downloadFile(archiveFileKey, path))
                .then(Mono.just(path))
                .flatMap(file -> fileService.extractFileFromBin(file)
                        .doFinally(fileDetails -> fileService.deleteFileTmp(file))
                        .flatMapMany(newFile -> extractUploadAndUpdates(newFile, indexDataMap, archiveFileKey, fileCounter))
                        .then(Mono.defer(() -> Mono.just(fileCounter))))
                .map(this::checkProcessedFiles)
                .flatMap(counter -> con020ArchiveDao.updateIfExists(createArchiveEntityForStatusUpdate(payload, CON020ArchiveStatusEnum.PROCESSED.name(), counter)))
                .doOnNext(con020ArchiveEntity -> log.info("Updated archive entity  [{}] to PROCESSED", con020ArchiveEntity.getHashKey()));
    }

    private FileCounter checkProcessedFiles(FileCounter counter) {
        if(counter.getUploadedFiles().get() != counter.getUpdatedItems().get()){
            log.error("Uploaded files are different from updated items --> Uploaded files: [{}], Updated items: [{}]", counter.getUploadedFiles().get(), counter.getUpdatedItems().get());
        }
        return counter;
    }

    private Flux<String> extractUploadAndUpdates(Path path, Map<String, IndexData> indexDataMap, String archiveFileKey, FileCounter fileCounter){
        return fileService.extractFileFromArchive(path, indexDataMap, fileCounter)
                .collectList()
                .doFinally(fileDetails -> fileService.deleteFileTmp(path))
                .flatMapMany(fileDetails -> updateEnrichedEntities(fileDetails, indexDataMap, archiveFileKey, fileCounter));

    }

    private Flux<String> updateEnrichedEntities(List<FileDetail> fileDetails, Map<String, IndexData> indexDataMap, String archiveFileKey, FileCounter fileCounter) {
        return Flux.fromIterable(fileDetails)
                .filter(fileDetail -> fileDetail.getFilename().endsWith(PDF.getValue()))
                .flatMap(detail -> updatePrintedPdf(detail, indexDataMap, archiveFileKey), config.getUpdateItemMaxConcurrentRequest())
                .doOnNext(hashKey -> log.debug("Updated {} entity for archiveFileKey: [{}]", fileCounter.getUpdatedItems().incrementAndGet(), archiveFileKey));
    }

    private Mono<String> updatePrintedPdf(FileDetail fileDetail, Map<String, IndexData> indexDataMap, String archiveFileKey) {
        IndexData indexData = indexDataMap.get(fileDetail.getFilename());
        if (Objects.nonNull(indexData)) {
            CON020EnrichedEntity con020EnrichedEntity = createEnricherEntityForPrintedPdf(fileDetail.getFileKey(), fileDetail.getSha256(), archiveFileKey, indexData.getRequestId(), indexData.getRegisteredLetterCode());
            return con020EnricherDao.updatePrintedPdf(con020EnrichedEntity)
                    .map(CON020BaseEntity::getHashKey)
                    .doOnError(throwable -> log.error("Error during update Item: {}", throwable.getMessage(), throwable));
        }
        log.fatal("[{}] is not present in file bol", fileDetail.getFilename());
        return Mono.just(fileDetail.getFilename());
    }
}
