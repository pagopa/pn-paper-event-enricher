package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
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
import it.pagopa.pn.paper.event.enricher.utils.Sha256Handler;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.io.*;
import java.util.*;
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
    private final PnPaperEventEnricherConfig pnPaperEventEnricherConfig;


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

    public Mono<Void> handlePaperEventEnricherEvent(PaperArchiveEvent.Payload payload) {
        Map<String, IndexData> indexDataMap = new HashMap<>();
        String archiveFileKey = payload.getArchiveFileKey();
        return con020ArchiveDao.updateIfExists(createArchiveEntityForStatusUpdate(payload, CON020ArchiveStatusEnum.PROCESSING.name()))
                .flatMap(con020ArchiveEntity -> fileService.retrieveDownloadUrl(archiveFileKey))
                .flatMapMany(fileService::downloadFile)
                .collectList()
                .flatMap(bytes -> Mono.just(createInputStreamFromByteArray(bytes)))
                .flatMapMany(inputStream -> fileService.extractFilesFromArchive(new ZipArchiveInputStream(inputStream), indexDataMap))
                .doOnNext(fileDetail -> log.info("FileDetail: {}", fileDetail.getFilename()))
                .flatMap(p7mContent -> {
                    List<FileDetail> fileDetails = fileService.extractFilesFromArchiveP7m(new ZipArchiveInputStream(p7mContent.getContent()), indexDataMap);
                    log.info("Archive extraction end: archiveFileKey={} extractedFileCount={}", archiveFileKey, fileDetails.size() );

                    AtomicInteger uploadedFileCounter = new AtomicInteger( 0 );
                    return Flux.fromStream(fileDetails.stream().filter(fileDetail -> fileDetail.getFilename().endsWith(PDF.getValue())))
                            .flatMap(fileDetail -> uploadAndUpdatePrintedPdf(fileDetail, indexDataMap, archiveFileKey, uploadedFileCounter), pnPaperEventEnricherConfig.getSafeStorageUploadMaxConcurrentRequest());
                })
                .collectList()
                .flatMap(con020EnrichedEntities -> con020ArchiveDao.updateIfExists(createArchiveEntityForStatusUpdate(payload, CON020ArchiveStatusEnum.PROCESSED.name())))
                .then();
    }

    private Mono<String> uploadAndUpdatePrintedPdf(FileDetail fileDetail, Map<String, IndexData> indexDataMap, String archiveFileKey, AtomicInteger uploadedFileCounter) {
        String sha256 = Sha256Handler.computeSha256(fileDetail.getContentBytes());
        return fileService.uploadPdf(fileDetail.getContentBytes(), sha256)
                .doOnNext(s -> log.info("Uploaded files count={}", uploadedFileCounter.incrementAndGet()))
                .flatMap(fileKey -> updatePrintedPdf(fileDetail, indexDataMap, archiveFileKey, fileKey, sha256));
    }

    public static InputStream createInputStreamFromByteArray(List<byte[]> byteData) {
        return new SequenceInputStream(new Enumeration<>() {
            private int index = 0;

            @Override
            public boolean hasMoreElements() {
                return index < byteData.size();
            }

            @Override
            public InputStream nextElement() {
                return new ByteArrayInputStream(byteData.get(index++));
            }
        });
    }


    private Mono<String> updatePrintedPdf(FileDetail fileDetail, Map<String, IndexData> indexDataMap, String archiveFileKey, String fileKey, String sha256) {
        IndexData indexData = indexDataMap.get(fileDetail.getFilename());
        if (Objects.nonNull(indexData)) {
            CON020EnrichedEntity con020EnrichedEntity = createEnricherEntityForPrintedPdf(fileKey, archiveFileKey, indexData.getRequestId(), indexData.getRegisteredLetterCode(), sha256);
            return con020EnricherDao.updatePrintedPdf(con020EnrichedEntity)
                    .doOnError(throwable -> log.error("Error during update Item: {}", throwable.getMessage(), throwable))
                    .map(CON020BaseEntity::getHashKey);
        }
        log.info("Index data not found for file: {}", fileDetail.getFilename());
        return Mono.just("empty");
    }
}
