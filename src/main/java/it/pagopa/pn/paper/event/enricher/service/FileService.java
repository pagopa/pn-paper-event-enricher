package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.model.FileDetail;
import it.pagopa.pn.paper.event.enricher.model.IndexData;
import it.pagopa.pn.paper.event.enricher.utils.Sha256Handler;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static it.pagopa.pn.paper.event.enricher.model.FileTypeEnum.*;

@Component
@CustomLog
@RequiredArgsConstructor
public class FileService {

    private final SafeStorageService safeStorageService;

    public Flux<FileDetail> extractFilesFromArchive(InputStream archiveInputStream, Map<String, IndexData> indexDataMap) {
        ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(archiveInputStream);
        return Flux.generate(sink -> {
            try {
                ZipArchiveEntry entry = zipInputStream.getNextEntry();
                if (entry == null) {
                    sink.complete();
                } else {
                    if (entry.getName().endsWith(BOL.getValue()) && CollectionUtils.isEmpty(indexDataMap)) {
                        indexDataMap.putAll(parseBol(zipInputStream.readAllBytes()));
                        sink.next(new FileDetail(entry.getName(), null));
                    } else if (entry.getName().endsWith(PDF.getValue())) {
                        sink.next(new FileDetail(entry.getName(), zipInputStream.readAllBytes()));
                    } else {
                        sink.next(new FileDetail(entry.getName(), zipInputStream.readAllBytes()));
                    }
                }
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }


    private Map<String, IndexData> parseBol(byte[] bolBytes) {
        String bolString = new String(bolBytes);
        Map<String, IndexData> archiveDetails = new HashMap<>();
        for (String line : bolString.split("\n")) {
            if (!line.isEmpty()) {
                String[] cells = line.split("\\|");
                String p7mEntryName = cells[0];
                String requestId = cells[3];
                String registeredLetterCode = cells[6];

                if (p7mEntryName.toLowerCase().endsWith(PDF.name())) {
                    IndexData indexData = new IndexData(requestId, registeredLetterCode, p7mEntryName);
                    archiveDetails.put(p7mEntryName, indexData);
                }
            }
        }
        return archiveDetails;
    }

    public Mono<byte[]> downloadFile(String archiveFileKey) {
        ClassPathResource classPathResource = new ClassPathResource("PN_EXTERNAL_LEGAL_FACTS-2f1465cb10754f9cb47de16f15d59cff.zip");
        try {
            FileInputStream fis = new FileInputStream(classPathResource.getFile());

            return Mono.just(fis.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
       //return safeStorageService.callSafeStorageGetFileAndDownload(archiveFileKey);
    }

    public Mono<String> uploadPdf(byte[] pdfBytes) {
        return safeStorageService.callSelfStorageCreateFileAndUpload(pdfBytes, Sha256Handler.computeSha256(pdfBytes));
    }
}
