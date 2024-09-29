package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.db.Con020ArchiveDao;
import it.pagopa.pn.paper.event.enricher.middleware.db.Con020EnricherDao;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020ArchiveEntity;
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

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

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
                        .doOnError(throwable -> log.warn("Error while creating archive entity: {}", throwable.getMessage()))
                        .onErrorReturn(PaperEventEnricherException.class, con020ArchiveEntity))
                .flatMap(con020ArchiveEntity -> createEnricherEntity(payload))
                .flatMap(con020EnricherDao::updateMetadata)
                .then()
                .doOnError(throwable -> log.error("Unexpected error while creating entities: {}", throwable.getMessage(), throwable))
                .onErrorResume(PaperEventEnricherException.class, throwable -> Mono.empty());
    }

    public Mono<CON020ArchiveEntity> handlePaperEventEnricherEvent(PaperArchiveEvent.Payload payload) {
        Map<String, IndexData> indexDataMap = new HashMap<>();
        String archiveFileKey = payload.getArchiveFileKey();
        PipedInputStream pipedInputStream = new PipedInputStream();
        PipedOutputStream pipedOutputStream = fileService.initializePipedOutputStream(pipedInputStream);
        log.info("inizialized PipedOutputStream");

        con020ArchiveDao.updateIfExists(createArchiveEntityForStatusUpdate(payload, CON020ArchiveStatusEnum.PROCESSING.name()))
                .doOnNext(con020ArchiveEntity -> log.info("Archive entity updated: {} to PROCESSING", con020ArchiveEntity.getHashKey()))
                .flatMap(con020ArchiveEntity -> fileService.retrieveDownloadUrl(archiveFileKey))
                .flatMapMany(fileService::downloadFile)
                .doOnNext(bytes -> fileService.writeToPipedOutputStream(bytes, pipedOutputStream))
                .doFinally(signalType -> fileService.closeStream(pipedOutputStream))
                .subscribe();

        return fileService.extractFileFromBin(new ZipArchiveInputStream(pipedInputStream))
                .doOnNext(fileDetail -> log.info("FileDetail: {}", fileDetail.getFilename()))
                .flatMapMany(fileDetail -> {
                    AtomicInteger uploadedFileCounter = new AtomicInteger(0);
                    return fileService.extractFilesFromArchive(new ZipArchiveInputStream(fileDetail.getContent()), indexDataMap, uploadedFileCounter);
                })
                .collectList()
                .flatMap(fileDetails -> updateEnrichedEntities(fileDetails, indexDataMap, archiveFileKey))
                .flatMap(unused -> con020ArchiveDao.updateIfExists(createArchiveEntityForStatusUpdate(payload, CON020ArchiveStatusEnum.PROCESSED.name())));
    }

    private Mono<List<String>> updateEnrichedEntities(List<FileDetail> fileDetails, Map<String, IndexData> indexDataMap, String archiveFileKey) {
        return Flux.fromIterable(fileDetails)
                .filter(fileDetail -> fileDetail.getFilename().endsWith(PDF.getValue()))
                .flatMap(detail -> {
                    AtomicInteger updatedFileCounter = new AtomicInteger(0);
                    return updatePrintedPdf(detail, indexDataMap, archiveFileKey)
                            .doOnNext(s -> log.info("Updated files count={}", updatedFileCounter.incrementAndGet()));
                })
                .collectList();
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
}
