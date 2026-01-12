package it.pagopa.pn.paper.event.enricher.utils;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.model.IndexData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static it.pagopa.pn.paper.event.enricher.exception.PnPaperEventEnricherExceptionConstant.*;
import static it.pagopa.pn.paper.event.enricher.model.FileTypeEnum.BOL;
import static it.pagopa.pn.paper.event.enricher.model.FileTypeEnum.PDF;

@Slf4j
public class FileUtils {

    public static byte[] getContent(InputStream in, String fileName) {
        try {
            return in.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to read file [{}]", fileName, e);
            throw new PaperEventEnricherException(String.format("Failed to read file [%s]", fileName), 500, FAILED_TO_READ_FILE);
        }
    }

    public static Map<String, IndexData> parseBol(byte[] bolBytes) {
        String bolString = new String(bolBytes);
        Map<String, IndexData> archiveDetails = new HashMap<>();
        for (String line : bolString.split("\n")) {
            if (!line.isEmpty()) {
                String[] cells = line.split("\\|");
                if(cells.length > 6) {
                    String p7mEntryName = cells[0];
                    String requestId = cells[3];
                    String registeredLetterCode = cells[6];

                    if (p7mEntryName.toLowerCase().endsWith(PDF.getValue())) {
                        IndexData indexData = new IndexData(requestId, registeredLetterCode, p7mEntryName);
                        archiveDetails.put(p7mEntryName, indexData);
                    }
                }
            }
        }
        return archiveDetails;
    }

    public static InputStream getInputStreamFromEntry(ZipArchiveEntry zipArchiveEntry, ZipFile zipFile) {
        try {
            return zipFile.getInputStream(zipArchiveEntry);
        } catch (IOException e) {
            throw new PaperEventEnricherException(e.getMessage(), 500, ERROR_DURING_FILE_EXTRACTION_FROM_ARCHIVE);
        }
    }

    public static InputStream getInputStreamFromEntry(SevenZArchiveEntry zipArchiveEntry, SevenZFile zipFile) {
        try {
            return zipFile.getInputStream(zipArchiveEntry);
        } catch (IOException e) {
            throw new PaperEventEnricherException(e.getMessage(), 500, ERROR_DURING_FILE_EXTRACTION_FROM_ARCHIVE);
        }
    }

    public static void readAndParseBol(Map<String, IndexData> indexDataMap, List<ZipArchiveEntry> entries, ZipFile zipFile) {
        Stream<ZipArchiveEntry> bolEntries = entries.getLast().getName().endsWith(BOL.getValue())
                ? Stream.of(entries.getLast())
                : entries.stream().filter(e -> e.getName().endsWith(BOL.getValue()));
        bolEntries.map(zipArchiveEntry -> parseBol(getContent(getInputStreamFromEntry(zipArchiveEntry, zipFile), zipArchiveEntry.getName())))
                .forEach(indexDataMap::putAll);
    }

    public static void readAndParseBol(Map<String, IndexData> indexDataMap, List<SevenZArchiveEntry> entries, SevenZFile zipFile) {
        Stream<SevenZArchiveEntry> bolEntries = entries.getLast().getName().endsWith(BOL.getValue())
                ? Stream.of(entries.getLast())
                : entries.stream().filter(e -> e.getName().endsWith(BOL.getValue()));
        bolEntries.map(zipArchiveEntry -> parseBol(getContent(getInputStreamFromEntry(zipArchiveEntry, zipFile), zipArchiveEntry.getName())))
                .forEach(indexDataMap::putAll);
    }

    public static void closeZipFile(ZipFile zipFile) {
        try { zipFile.close(); } catch (IOException ignored) {}
    }

    public static void close7zFile(SevenZFile zipFile) {
        try { zipFile.close(); } catch (IOException ignored) {}
    }

    public static boolean startsWith(byte[] file, byte[] signature) {
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
}
