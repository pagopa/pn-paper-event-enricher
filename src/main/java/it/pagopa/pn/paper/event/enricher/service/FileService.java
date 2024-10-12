package it.pagopa.pn.paper.event.enricher.service;

import it.pagopa.pn.paper.event.enricher.config.PnPaperEventEnricherConfig;
import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.externalclient.pnclient.safestorage.UploadDownloadClient;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static it.pagopa.pn.paper.event.enricher.exception.PnPaperEventEnricherExceptionConstant.*;
import static it.pagopa.pn.paper.event.enricher.model.FileTypeEnum.*;
import static it.pagopa.pn.paper.event.enricher.utils.P7mUtils.findSignedData;
import static it.pagopa.pn.paper.event.enricher.utils.PaperEventEnricherUtils.getContent;
import static it.pagopa.pn.paper.event.enricher.utils.PaperEventEnricherUtils.parseBol;

@Component
@CustomLog
@RequiredArgsConstructor
public class FileService {

    public static final String UPLOADED_FILES_COUNT = "Uploaded files count={}";
    public static final String DATA_MAP_WITH_ENTRY = "Parsed bol file: [{}] from archive and enriched indexDataMap with {} entry";
    public static final String PDF_EXTRATED = "pdf: {} extrated with content lenght: {}";


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
        } catch (IOException e) {
            throw new PaperEventEnricherException(e.getMessage(), 500, e.getCause().toString());
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

    public Flux<FileDetail> extractFileFromArchive(Path path, Map<String, IndexData> indexDataMap, AtomicInteger counter) {
        return retrieveFileType(path)
                .flatMapMany(fileTypeEnum -> {
                    if (ZIP.equals(fileTypeEnum)) {
                        return extractZipFilesFromArchive(path, indexDataMap, counter);
                    } else if (SEVENZIP.equals(fileTypeEnum)) {
                        return extract7ZipFilesFromArchive(path, indexDataMap, counter);
                    } else {
                        return Flux.error(new PaperEventEnricherException(UNSUPPORTED_FILE_TYPE, 500, UNSUPPORTED_FILE_TYPE));
                    }
                });

    }

    public Flux<FileDetail> extractZipFilesFromArchive(Path path, Map<String, IndexData> indexDataMap, AtomicInteger uploadedFileCounter) {
        try {
            ZipFile zipFile = ZipFile.builder().setFile(path.toFile()).get();
            Iterable<ZipArchiveEntry> iterable = () -> zipFile.getEntries().asIterator();
            return Flux.fromIterable(iterable)
                    .flatMap(zipArchiveEntry -> {
                        try {
                            return getFileDetail(zipFile.getInputStream(zipArchiveEntry), indexDataMap, zipArchiveEntry.getName());
                        } catch (IOException e) {
                            throw new PaperEventEnricherException(e.getMessage(), 500, "Error during file extraction from archive");
                        }
                    }, pnPaperEventEnricherConfig.getSafeStorageUploadMaxConcurrentRequest())
                    .filter(fileDetail -> StringUtils.hasText(fileDetail.getFilename()) && fileDetail.getFilename().endsWith(PDF.getValue()))
                    .doOnNext(s -> log.info(UPLOADED_FILES_COUNT, uploadedFileCounter.incrementAndGet()));
        } catch (IOException e) {
            throw new PaperEventEnricherException(e.getMessage(), 500, "Error during file extraction from archive");
        }
    }

    public Flux<FileDetail> extract7ZipFilesFromArchive(Path path, Map<String, IndexData> indexDataMap, AtomicInteger uploadedFileCounter) {
        try {
            SevenZFile sevenZFile = SevenZFile.builder().setFile(path.toFile()).get();
            Iterable<SevenZArchiveEntry> iterable = () -> sevenZFile.getEntries().iterator();
            return Flux.fromIterable(iterable)
                    .flatMap(zipArchiveEntry -> {
                        try {
                            return getFileDetail(sevenZFile.getInputStream(zipArchiveEntry), indexDataMap, zipArchiveEntry.getName());
                        } catch (IOException e) {
                            throw new PaperEventEnricherException(e.getMessage(), 500, "Error during file extraction from archive");
                        }
                    }, pnPaperEventEnricherConfig.getSafeStorageUploadMaxConcurrentRequest())
                    .filter(fileDetail -> StringUtils.hasText(fileDetail.getFilename()) && fileDetail.getFilename().endsWith(PDF.getValue()))
                    .doOnNext(s -> log.info(UPLOADED_FILES_COUNT, uploadedFileCounter.incrementAndGet()));
        } catch (IOException e) {
            throw new PaperEventEnricherException(e.getMessage(), 500, "Error during file extraction from archive");
        }
    }

    private Mono<FileDetail> getFileDetail(InputStream zipInputStream, Map<String, IndexData> indexDataMap, String name) {
        if (name.endsWith(BOL.getValue()) && Objects.nonNull(indexDataMap)) {
            indexDataMap.putAll(parseBol(getContent(zipInputStream, name)));
            log.info(DATA_MAP_WITH_ENTRY, name, indexDataMap.size());
            return Mono.just(FileDetail.builder().filename(name).build());
        } else if (name.endsWith(PDF.getValue())) {
            byte[] content = getContent(zipInputStream, name);
            log.info(PDF_EXTRATED, name, content.length);
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
        try {
            WritableByteChannel channel = FileChannel.open(newFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            int bufferSize = 4096;
            DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
            return DataBufferUtils.readInputStream(() -> inputStream, bufferFactory, bufferSize)
                    .flatMap(dataBuffer -> DataBufferUtils.write(Flux.just(dataBuffer), channel)
                            .doOnError(e -> log.error("Error during file writing: {}", e.getMessage()))
                            .doFinally(signalType -> DataBufferUtils.release(dataBuffer)))
                    .doFinally(signal -> uploadDownloadClient.closeWritableByteChannel(channel))
                    .then(Mono.just(newFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Path createTmpFile(String prefix, String suffix) {
        try {
            return Files.createTempFile(prefix, suffix);
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
            throw new RuntimeException(e);
        }
        log.info("File {} deleted", fileName);
    }
}
