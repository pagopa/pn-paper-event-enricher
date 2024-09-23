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

import java.util.Map;
import java.util.Objects;

import static it.pagopa.pn.paper.event.enricher.model.FileTypeEnum.BOL;
import static it.pagopa.pn.paper.event.enricher.model.FileTypeEnum.PDF;
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
                    if (zipArchiveEntry.getName().endsWith(BOL.getValue()) && Objects.nonNull(indexDataMap)) {
                        indexDataMap.putAll(parseBol(getContent(zipInputStream, zipArchiveEntry.getName())));
                        return Mono.just(new FileDetail(zipArchiveEntry.getName(), null, null));
                    } else if (zipArchiveEntry.getName().endsWith(PDF.getValue())) {
                        return uploadPdf(getContent(zipInputStream, zipArchiveEntry.getName()))
                                .map(fileKey -> new FileDetail(zipArchiveEntry.getName(), null, fileKey));
                    } else {
                        return Mono.just(new FileDetail(zipArchiveEntry.getName(), getContent(zipInputStream, zipArchiveEntry.getName()), null));
                    }
                });
    }

    public Mono<byte[]> downloadFile(String archiveFileKey) {
        return safeStorageService.callSafeStorageGetFileAndDownload(archiveFileKey);
    }

    public Mono<String> uploadPdf(byte[] pdfBytes) {
        return safeStorageService.callSelfStorageCreateFileAndUpload(pdfBytes, Sha256Handler.computeSha256(pdfBytes));
    }
}
