package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage.UploadDownloadClient;
import it.pagopa.pn.paper.event.enricher.model.FileCounter;
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
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static it.pagopa.pn.paper.event.enricher.exception.PnPaperEventEnricherExceptionConstant.*;
import static it.pagopa.pn.paper.event.enricher.model.FileTypeEnum.*;
import static it.pagopa.pn.paper.event.enricher.utils.P7mUtils.findSignedData;
import static it.pagopa.pn.paper.event.enricher.utils.PaperEventEnricherUtils.getContent;
import static it.pagopa.pn.paper.event.enricher.utils.PaperEventEnricherUtils.parseBol;
import static it.pagopa.pn.paper.event.enricher.utils.PdfUtils.cutPdf;

@Component
@CustomLog
@RequiredArgsConstructor
public class FileService {

    public static final String UPLOADED_FILES_COUNT = "Uploaded files count={}";
    public static final String DATA_MAP_WITH_ENTRY = "Parsed bol file: [{}] from archive and enriched indexDataMap with {} entry";
    public static final String PDF_EXTRATED = "pdf: {} extrated with content lenght: {}";

    public static final String TMP_FILE_PREFIX = "tmp_";


    private final SafeStorageService safeStorageService;
    private final UploadDownloadClient uploadDownloadClient;
    private final PnPaperEventEnricherConfig pnPaperEventEnricherConfig;
    private static final byte[] ZIP_SIGNATURE = new byte[]{0x50, 0x4B, 0x03, 0x04};
    private static final byte[] SEVEN_ZIP_SIGNATURE = new byte[]{0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C};

    public Mono<Path> extractFileFromBin(Path path) {
        return retrieveFileType(path)
                .flatMap(fileTypeEnum -> {
                    if (ZIP.equals(fileTypeEnum)) {
                        return extractZipFileFromBin(path);
                    } else {
                        return extractSevenZipFileFromBin(path);
                    }
                });
    }

    public Mono<Path> extractZipFileFromBin(Path path) {
        try {
            ZipFile zipFile = ZipFile.builder().setFile(path.toFile()).get();
            ZipArchiveEntry zipArchiveEntry = zipFile.getEntries().nextElement();
            Path newFile = createTmpFile(zipArchiveEntry.getName(), ZIP.getValue());
            return findSignedData(zipFile.getInputStream(zipArchiveEntry))
                    .flatMap(input -> writeInputStreamToFile(input, newFile))
                    .thenReturn(newFile);
        } catch (Exception e) {
            log.error("Error during file extraction from zip file: {}", e.getMessage(), e);
            throw new PaperEventEnricherException(e.getMessage(), 500, UNABLE_TO_WRITE_ON_TMP_FILE);
        }
    }

    private Mono<Path> extractSevenZipFileFromBin(Path path) {
        try {
            SevenZFile sevenZFile = SevenZFile.builder().setFile(path.toFile()).get();
            SevenZArchiveEntry sevenZArchiveEntry = sevenZFile.getEntries().iterator().next();
            Path newFile = createTmpFile(sevenZArchiveEntry.getName(), SEVENZIP.getValue());
            return findSignedData(sevenZFile.getInputStream(sevenZArchiveEntry))
                    .flatMap(input -> writeInputStreamToFile(input, newFile))
                    .thenReturn(newFile);
        } catch (IOException e) {
            throw new PaperEventEnricherException(e.getMessage(), 500, UNABLE_TO_WRITE_ON_TMP_FILE);
        }
    }

    public Flux<FileDetail> extractFileFromArchive(Path path, Map<String, IndexData> indexDataMap, FileCounter fileCounter) {
        return retrieveFileType(path)
                .flatMapMany(fileTypeEnum -> {
                    if (ZIP.equals(fileTypeEnum)) {
                        return extractZipFilesFromArchive(path, indexDataMap, fileCounter);
                    } else if (SEVENZIP.equals(fileTypeEnum)) {
                        return extract7ZipFilesFromArchive(path, indexDataMap, fileCounter);
                    } else {
                        return Flux.error(new PaperEventEnricherException(UNSUPPORTED_FILE_TYPE, 500, UNSUPPORTED_FILE_TYPE));
                    }
                });

    }

    public Flux<FileDetail> extractZipFilesFromArchive(Path path, Map<String, IndexData> indexDataMap, FileCounter fileCounter) {
        try {
            ZipFile zipFile = ZipFile.builder().setFile(path.toFile()).get();
            List<ZipArchiveEntry> entries = Collections.list(zipFile.getEntries());
            fileCounter.setTotalFiles(entries.size());
            return Flux.fromIterable(entries)
                    .flatMap(zipArchiveEntry -> {
                        try {
                            return getFileDetail(zipFile.getInputStream(zipArchiveEntry), indexDataMap, zipArchiveEntry.getName());
                        } catch (IOException e) {
                            throw new PaperEventEnricherException(e.getMessage(), 500, ERROR_DURING_FILE_EXTRACTION_FROM_ARCHIVE);
                        }
                    }, pnPaperEventEnricherConfig.getSafeStorageUploadMaxConcurrentRequest())
                    .filter(fileDetail -> StringUtils.hasText(fileDetail.getFilename()) && fileDetail.getFilename().endsWith(PDF.getValue()))
                    .doOnNext(s -> log.info(UPLOADED_FILES_COUNT, fileCounter.getUploadedFiles().incrementAndGet()));
        } catch (IOException e) {
            throw new PaperEventEnricherException(e.getMessage(), 500, ERROR_DURING_FILE_EXTRACTION_FROM_ARCHIVE);
        }
    }

    public Flux<FileDetail> extract7ZipFilesFromArchive(Path path, Map<String, IndexData> indexDataMap, FileCounter fileCounter) {
        try {
            SevenZFile sevenZFile = SevenZFile.builder().setFile(path.toFile()).get();
            List<SevenZArchiveEntry> entries = (List<SevenZArchiveEntry>) sevenZFile.getEntries();
            fileCounter.setTotalFiles(entries.size());
            return Flux.fromIterable(entries)
                    .flatMap(zipArchiveEntry -> {
                        try {
                            return getFileDetail(sevenZFile.getInputStream(zipArchiveEntry), indexDataMap, zipArchiveEntry.getName());
                        } catch (IOException e) {
                            throw new PaperEventEnricherException(e.getMessage(), 500, ERROR_DURING_FILE_EXTRACTION_FROM_ARCHIVE);
                        }
                    }, pnPaperEventEnricherConfig.getSafeStorageUploadMaxConcurrentRequest())
                    .filter(fileDetail -> StringUtils.hasText(fileDetail.getFilename()) && fileDetail.getFilename().endsWith(PDF.getValue()))
                    .doOnNext(s -> log.debug(UPLOADED_FILES_COUNT, fileCounter.getUploadedFiles().incrementAndGet()));
        } catch (IOException e) {
            throw new PaperEventEnricherException(e.getMessage(), 500, ERROR_DURING_FILE_EXTRACTION_FROM_ARCHIVE);
        }
    }

    private Mono<FileDetail> getFileDetail(InputStream zipInputStream, Map<String, IndexData> indexDataMap, String name) {
        if (name.endsWith(BOL.getValue()) && Objects.nonNull(indexDataMap)) {
            indexDataMap.putAll(parseBol(getContent(zipInputStream, name)));
            log.info(DATA_MAP_WITH_ENTRY, name, indexDataMap.size());
            return Mono.just(FileDetail.builder().filename(name).build());
        } else if (name.endsWith(PDF.getValue())) {
            byte[] content = getContent(zipInputStream, name);
            log.debug(PDF_EXTRATED, name, content.length);
            if(pnPaperEventEnricherConfig.isPdfTwoPagesEnabled()){
                content = cutPdf(content, pnPaperEventEnricherConfig.getPdfPageSize());
            }
            String sha256 = Sha256Handler.computeSha256(content);
            return safeStorageService.callSafeStorageCreateFileAndUpload(content, sha256)
                    .map(fileKey -> FileDetail.builder().filename(name).fileKey(fileKey).sha256(sha256).build());
        } else {
            return Mono.just(FileDetail.builder().filename(name).build());
        }
    }

    public Flux<Void> downloadFile(String archiveFileKey, Path file) {
        return safeStorageService.callSafeStorageGetFile(archiveFileKey)
                .flatMapMany(url -> uploadDownloadClient.downloadContent(url, file));
    }

    public Mono<Path> writeInputStreamToFile(InputStream inputStream, Path newFile) {
        WritableByteChannel channel = null;
        try {
            channel = FileChannel.open(newFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            int bufferSize = 4096;
            DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
            WritableByteChannel finalChannel = channel;
            return DataBufferUtils.readInputStream(() -> inputStream, bufferFactory, bufferSize)
                    .flatMap(dataBuffer -> DataBufferUtils.write(Flux.just(dataBuffer), finalChannel)
                            .doOnError(e -> log.error("Error during file writing: {}", e.getMessage()))
                            .doFinally(signalType -> DataBufferUtils.release(dataBuffer)))
                    .doOnComplete(() -> uploadDownloadClient.closeWritableByteChannel(finalChannel))
                    .doOnError(throwable -> uploadDownloadClient.closeWritableByteChannel(finalChannel))
                    .then(Mono.just(newFile));
        } catch (Exception e) {
            log.error("error in URI ", e);
            uploadDownloadClient.closeWritableByteChannel(channel);
            throw new PaperEventEnricherException(e.getMessage(),500, ERROR_DURING_WRITE_FILE);
        }
    }

    public Path createTmpFile(String prefix, String suffix) {
        try {
            return File.createTempFile(TMP_FILE_PREFIX + prefix, suffix).toPath();
        } catch (IOException e) {
            throw new PaperEventEnricherException(e.getMessage(), 500, UNABLE_TO_CREATE_TMP_FILE);
        }
    }

    private Mono<FileTypeEnum> retrieveFileType(Path file) {
        try (FileInputStream fileInputStream = new FileInputStream(file.toFile())) {
            byte[] header = fileInputStream.readNBytes(6);
            if (startsWith(header, ZIP_SIGNATURE)) {
                return Mono.just(ZIP);
            } else if (startsWith(header, SEVEN_ZIP_SIGNATURE)) {
                return Mono.just(SEVENZIP);
            } else {
                return Mono.error(new PaperEventEnricherException(UNSUPPORTED_FILE_TYPE, 400, UNSUPPORTED_FILE_TYPE));
            }
        } catch (IOException e) {
            return Mono.error(new PaperEventEnricherException(e.getMessage(), 500, FAILED_TO_READ_FILE));
        }
    }

    private boolean startsWith(byte[] file, byte[] signature) {
        if (file.length < 4) {
            throw new PaperEventEnricherException(FILE_IS_TOO_SHORT, 400, FILE_IS_TOO_SHORT);
        }
        for (int i = 0; i < signature.length; i++) {
            if (file[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    public void deleteFileTmp(Path path) {
        String fileName = path.getFileName().toString();
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new PaperEventEnricherException(e.getMessage(),500, ERROR_DELETING_TMP_FILE);
        }
        log.info("File {} deleted", fileName);
    }
}
