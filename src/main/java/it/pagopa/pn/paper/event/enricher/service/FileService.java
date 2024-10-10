package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.model.FileDetail;
import it.pagopa.pn.paper.event.enricher.model.FileTypeEnum;
import it.pagopa.pn.paper.event.enricher.model.IndexData;
import it.pagopa.pn.paper.event.enricher.utils.Sha256Handler;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private static final byte[] ZIP_SIGNATURE = new byte[]{0x50, 0x4B, 0x03, 0x04};
    private static final byte[] SEVEN_ZIP_SIGNATURE = new byte[]{0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C};

    //TODO: cercare di unificare i vari metodi
    public Mono<Path> extractFileFromBin(Path path) {
        Path newFile;
        FileTypeEnum fileTypeEnum = retrieveFileType(path);
        if (ZIP.equals(fileTypeEnum)) {
            newFile = extractZipFileFromBin(path);
        } else if (SEVENZIP.equals(fileTypeEnum)) {
            newFile = extractSevenZipFileFromBin(path);
        } else {
            throw new PaperEventEnricherException("Unsupported file type", 500, "Unsupported file type");
        }
        return Mono.just(newFile);
    }

    public Path extractZipFileFromBin(Path path) {
        try {
            Path newFile = createTmpFile();
            ZipFile zipFile = ZipFile.builder().setFile(path.toFile()).get();
            ZipArchiveEntry zipArchiveEntry = zipFile.getEntries().nextElement();
            InputStream inputStream = findSignedData(zipFile.getInputStream(zipArchiveEntry));
            writeInputStreamToFile(inputStream, newFile.toString());
            return newFile;
        } catch (IOException e) {
            throw new PaperEventEnricherException(e.getMessage(), 500, e.getCause().toString());
        }
    }

    private Path extractSevenZipFileFromBin(Path path) {
        try {
            Path newFile = createTmpFile();
            SevenZFile sevenZFile = SevenZFile.builder().setFile(path.toFile()).get();
            SevenZArchiveEntry sevenZArchiveEntry = sevenZFile.getEntries().iterator().next();
            InputStream inputStream = findSignedData(sevenZFile.getInputStream(sevenZArchiveEntry));
            writeInputStreamToFile(inputStream, newFile.toString());
            return newFile;
        } catch (IOException e) {
            throw new PaperEventEnricherException("Unable to write on tmp file", 500, e.getMessage());
        }
    }

    public Flux<FileDetail> extractFileFromArchive(Path path, Map<String, IndexData> indexDataMap, AtomicInteger counter) {
        FileTypeEnum fileTypeEnum = retrieveFileType(path);
        if (ZIP.equals(fileTypeEnum)) {
            try {
                return extractZipFilesFromArchive(ZipFile.builder().setFile(path.toFile()).get(), indexDataMap, counter);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (SEVENZIP.equals(fileTypeEnum)) {
            try {
                return extract7ZipFilesFromArchive(SevenZFile.builder().setFile(path.toFile()).get(), indexDataMap, counter);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new PaperEventEnricherException("Unsupported file type", 500, "Unsupported file type");
        }
    }

    public Flux<FileDetail> extractZipFilesFromArchive(ZipFile zipInputStream, Map<String, IndexData> indexDataMap, AtomicInteger uploadedFileCounter) {
        Iterable<ZipArchiveEntry> iterable = () -> zipInputStream.getEntries().asIterator();
        return Flux.fromIterable(iterable)
                .flatMap(zipArchiveEntry -> {
                    try {
                        return getFileDetail(zipInputStream.getInputStream(zipArchiveEntry), indexDataMap, zipArchiveEntry.getName());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, pnPaperEventEnricherConfig.getSafeStorageUploadMaxConcurrentRequest())
                .doOnNext(s -> log.info("Uploaded files count={}", uploadedFileCounter.incrementAndGet()));
    }

    public Flux<FileDetail> extract7ZipFilesFromArchive(SevenZFile sevenZFile, Map<String, IndexData> indexDataMap, AtomicInteger uploadedFileCounter) {
        Iterable<SevenZArchiveEntry> iterable = () -> sevenZFile.getEntries().iterator();
        return Flux.fromIterable(iterable)
                .flatMap(zipArchiveEntry -> {
                    try {
                        return getFileDetail(sevenZFile.getInputStream(zipArchiveEntry), indexDataMap, zipArchiveEntry.getName());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, pnPaperEventEnricherConfig.getSafeStorageUploadMaxConcurrentRequest())
                .doOnNext(s -> log.info("Uploaded files count={}", uploadedFileCounter.incrementAndGet()));
    }

    private FileTypeEnum retrieveFileType(Path file) {
        try (FileInputStream fileInputStream = new FileInputStream(file.toFile());) {
            return getFileType(fileInputStream.readNBytes(6));
        } catch (IOException e) {
            throw new PaperEventEnricherException("Unable to read file", 500, e.getMessage());
        }
    }

    private FileTypeEnum getFileType(byte[] file) {
        if (startsWith(file, ZIP_SIGNATURE)) {
            return ZIP;
        } else if (startsWith(file, SEVEN_ZIP_SIGNATURE)) {
            return SEVENZIP;
        } else {
            throw new PaperEventEnricherException("Unsupported file type", 400, "UNSUPPORTED_FILE_TYPE");
        }
    }

    private static boolean startsWith(byte[] file, byte[] signature) {
        if (file.length < 4) {
            throw new PaperEventEnricherException("File is too short", 400, "FILE_TOO_SHORT");
        }
        for (int i = 0; i < signature.length; i++) {
            if (file[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    public static void writeInputStreamToFile(InputStream inputStream, String filePath) throws IOException {
        //TODO: OTTIMIZZARE LA SCRITTURA SUL SECONDO FILE
        byte[] buffer = new byte[4096];
        int bytesRead;

        try (OutputStream outputStream = new FileOutputStream(filePath)) {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    public Path createTmpFile() {
        //TODO: USE CONSTANT
        try {
            return Files.createTempFile("tmp", ".bin");
        } catch (IOException e) {
            throw new PaperEventEnricherException("Unable to create tmp file", 500, e.getMessage());
        }
    }

    private Mono<FileDetail> getFileDetail(InputStream zipInputStream, Map<String, IndexData> indexDataMap, String name) {
        FileDetail fileDetail;
        if (name.endsWith(BOL.getValue()) && Objects.nonNull(indexDataMap)) {
            indexDataMap.putAll(parseBol(getContent(zipInputStream, name)));
            log.info("Parsed bol file: [{}] from archive and enriched indexDataMap with {} entry", name, indexDataMap.size());
            fileDetail = new FileDetail(name, null, null);
        } else if (name.endsWith(PDF.getValue())) {
            byte[] content = getContent(zipInputStream, name);
            log.info("pdf: {} extrated with content lenght: {}", name, content.length);
            return safeStorageService.callSafeStorageCreateFileAndUpload(content, Sha256Handler.computeSha256(content))
                    .map(fileKey -> new FileDetail(name, null, fileKey));
        } else {
            fileDetail = new FileDetail(name, null, null);
        }
        return Mono.just(fileDetail);
    }
}
