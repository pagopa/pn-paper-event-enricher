package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.model.FileDetail;
import it.pagopa.pn.paper.event.enricher.model.IndexData;
import it.pagopa.pn.paper.event.enricher.utils.Sha256Handler;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static it.pagopa.pn.paper.event.enricher.model.FileTypeEnum.*;
import static it.pagopa.pn.paper.event.enricher.utils.P7mUtils.findSignedData;
import static it.pagopa.pn.paper.event.enricher.utils.PaperEventEnricherUtils.getContent;
import static it.pagopa.pn.paper.event.enricher.utils.PaperEventEnricherUtils.parseBol;

@Component
@CustomLog
@RequiredArgsConstructor
public class FileService {

    private final SafeStorageService safeStorageService;
    private final PnPaperEventEnricherConfig pnPaperEventEnricherConfig;

    public Flux<FileDetail> extractFilesFromArchive(ZipArchiveInputStream zipInputStream, Map<String, IndexData> indexDataMap, AtomicInteger uploadedFileCounter) {
        Iterable<ZipArchiveEntry> iterable = () -> zipInputStream.iterator().asIterator();
        return Flux.fromIterable(iterable)
                .flatMap(zipArchiveEntry -> getFileDetail(zipInputStream, indexDataMap, zipArchiveEntry), pnPaperEventEnricherConfig.getSafeStorageUploadMaxConcurrentRequest())
                .doOnNext(s -> log.info("Uploaded files count={}", uploadedFileCounter.incrementAndGet()));
    }

    public Mono<FileDetail> extractFileFromBin(ZipArchiveInputStream zipInputStream) {
        try {
            return Mono.just(zipInputStream.getNextEntry())
                    .flatMap(zipArchiveEntry -> getFileDetail(zipInputStream, null, zipArchiveEntry));
        } catch (IOException e) {
            throw new PaperEventEnricherException(e.getMessage(), 500, e.getCause().toString());
        }
    }

    private Mono<FileDetail> getFileDetail(ZipArchiveInputStream zipInputStream, Map<String, IndexData> indexDataMap, ZipArchiveEntry zipArchiveEntry) {
        FileDetail fileDetail;
        if (zipArchiveEntry.getName().endsWith(BOL.getValue()) && Objects.nonNull(indexDataMap)) {
            indexDataMap.putAll(parseBol(getContent(zipInputStream, zipArchiveEntry.getName())));
            fileDetail = new FileDetail(zipArchiveEntry.getName(), null, null);
        } else if (zipArchiveEntry.getName().endsWith(PDF.getValue())) {
            log.info("Extracting pdf file from archive");
            byte[] content = getContent(zipInputStream, zipArchiveEntry.getName());
            log.info("pdf: {}, {}", zipArchiveEntry.getName(), content.length);
            return safeStorageService.callSelfStorageCreateFileAndUpload(content, Sha256Handler.computeSha256(content))
                    .map(fileKey -> new FileDetail(zipArchiveEntry.getName(), fileKey, null));
        } else if (zipArchiveEntry.getName().endsWith(P7M.getValue())) {
            log.info("Extracting p7m file from archive");
            InputStream p7mContent = findSignedData(zipInputStream);
            fileDetail = new FileDetail(zipArchiveEntry.getName(), null, p7mContent);
        } else {
            fileDetail = new FileDetail(zipArchiveEntry.getName(), null, null);
        }
        return Mono.just(fileDetail);
    }

    public Mono<String> retrieveDownloadUrl(String archiveFileKey) {
        return safeStorageService.callSafeStorageGetFileAndDownload(archiveFileKey);
    }

    public Flux<byte[]> downloadFile(String url) {
        return safeStorageService.downloadContent(url);
    }

}
