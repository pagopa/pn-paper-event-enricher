package it.pagopa.pn.paper.event.enricher.service;

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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static it.pagopa.pn.paper.event.enricher.model.FileTypeEnum.BOL;
import static it.pagopa.pn.paper.event.enricher.model.FileTypeEnum.PDF;
import static it.pagopa.pn.paper.event.enricher.utils.P7mUtils.findSignedData;
import static it.pagopa.pn.paper.event.enricher.utils.PaperEventEnricherUtils.getContent;
import static it.pagopa.pn.paper.event.enricher.utils.PaperEventEnricherUtils.parseBol;

@Component
@CustomLog
@RequiredArgsConstructor
public class FileService {

    private final SafeStorageService safeStorageService;

    public Flux<FileDetail> extractFilesFromArchive(ZipArchiveInputStream zipInputStream, Map<String, IndexData> indexDataMap) {
        Iterable<ZipArchiveEntry> iterable = () -> zipInputStream.iterator().asIterator();
        return Flux.fromIterable(iterable)
                .flatMap(zipArchiveEntry -> {
                    FileDetail fileDetail = getFileDetail(zipInputStream, indexDataMap, zipArchiveEntry);
                    return Mono.just(fileDetail);
                });
    }

    public List<FileDetail> extractFilesFromArchiveP7m(ZipArchiveInputStream zipInputStream, Map<String, IndexData> indexDataMap) {
        Iterable<ZipArchiveEntry> iterable = () -> zipInputStream.iterator().asIterator();
        List<FileDetail> list = new ArrayList<>();
        iterable.forEach(zipArchiveEntry -> {
            FileDetail fileDetail = getFileDetail(zipInputStream, indexDataMap, zipArchiveEntry);
            list.add(fileDetail);
        });
        return list;
    }

    private static FileDetail getFileDetail(ZipArchiveInputStream zipInputStream, Map<String, IndexData> indexDataMap, ZipArchiveEntry zipArchiveEntry) {
        FileDetail fileDetail;
        if (zipArchiveEntry.getName().endsWith(BOL.getValue()) && Objects.nonNull(indexDataMap)) {
            indexDataMap.putAll(parseBol(getContent(zipInputStream, zipArchiveEntry.getName())));
            fileDetail = new FileDetail(zipArchiveEntry.getName(), null, null, null);
        } else if (zipArchiveEntry.getName().endsWith(PDF.getValue())) {
            log.info("Extracting pdf file from archive");
            byte[] content = getContent(zipInputStream, zipArchiveEntry.getName());
            log.info("pdf: {}, {}", zipArchiveEntry.getName(), content.length);
            fileDetail = new FileDetail(zipArchiveEntry.getName(), null, null, content);
        } else if (zipArchiveEntry.getName().endsWith(".p7m")) {
            log.info("Extracting p7m file from archive");
            InputStream p7mContent = findSignedData(zipInputStream);
            fileDetail = new FileDetail(zipArchiveEntry.getName(), p7mContent, null, null);
        } else {
            fileDetail = new FileDetail(zipArchiveEntry.getName(), null, null, null);
        }
        return fileDetail;
    }

    public Mono<String> retrieveDownloadUrl(String archiveFileKey) {
        return safeStorageService.callSafeStorageGetFileAndDownload(archiveFileKey);
    }

    public Flux<byte[]> downloadFile(String url) {
        return safeStorageService.downloadContent(url);
    }

    public Mono<String> uploadPdf(byte[] pdfBytes) {
        return safeStorageService.callSelfStorageCreateFileAndUpload(pdfBytes, Sha256Handler.computeSha256(pdfBytes));
    }
}
