package it.pagopa.pn.paper.event.enricher.utils;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020ArchiveEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntityMetadata;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperArchiveEvent;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperEventEnricherInputEvent;
import it.pagopa.pn.paper.event.enricher.model.CON020ArchiveStatusEnum;
import it.pagopa.pn.paper.event.enricher.model.IndexData;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.pagopa.pn.paper.event.enricher.constant.PaperEventEnricherConstant.*;
import static it.pagopa.pn.paper.event.enricher.exception.PnPaperEventEnricherExceptionCode.*;
import static it.pagopa.pn.paper.event.enricher.model.FileTypeEnum.PDF;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaperEventEnricherUtils {


    public static Mono<CON020ArchiveEntity> createArchiveEntity(PaperEventEnricherInputEvent.Payload.AnalogMailDetail analogMailDetail) {
        CON020ArchiveEntity con020ArchiveEntity = new CON020ArchiveEntity();

        Instant now = Instant.now();
        String taskId = System.getenv(TASK_ID_ENV);

        if (!checkIfAttachmentIsPresent(analogMailDetail)) {
            return Mono.error(new PaperEventEnricherException("Archive attachment uri not found.", 400, "ARCHIVE_ATTACHMENT_NOT_FOUND_IN_EVENT"));
        }

        String archiveUri = analogMailDetail.getAttachments().get(0).getUri();

        con020ArchiveEntity.setHashKey(CON020ArchiveEntity.buildHashKeyForCon020ArchiveEntity(archiveUri));
        con020ArchiveEntity.setSortKey(SORT_KEY);
        con020ArchiveEntity.setEntityName(ARCHIVE_ENTITY_NAME);
        con020ArchiveEntity.setArchiveStatus(CON020ArchiveStatusEnum.NEW.name());
        con020ArchiveEntity.setRecordCreationTime(now);
        con020ArchiveEntity.setTtl(now.plus(365, ChronoUnit.DAYS).toEpochMilli());
        con020ArchiveEntity.setArchiveFileKey(archiveUri);
        con020ArchiveEntity.setProcessingTask(taskId);
        con020ArchiveEntity.setLastModificationTime(now);

        return Mono.just(con020ArchiveEntity);
    }

    private static boolean checkIfAttachmentIsPresent(PaperEventEnricherInputEvent.Payload.AnalogMailDetail analogMailDetail) {
        return Objects.nonNull(analogMailDetail) &&
                !CollectionUtils.isEmpty(analogMailDetail.getAttachments()) &&
                Objects.nonNull(analogMailDetail.getAttachments().get(0)) &&
                StringUtils.hasText(analogMailDetail.getAttachments().get(0).getUri());
    }

    public static Mono<CON020EnrichedEntity> createEnricherEntity(PaperEventEnricherInputEvent.Payload payload) {
        Instant now = Instant.now();

        String archiveUri = payload.getAnalogMail().getAttachments().get(0).getUri();
        String requestId = payload.getAnalogMail().getRequestId();
        String registeredLetterCode = payload.getAnalogMail().getRegisteredLetterCode();

        CON020EnrichedEntity con020EnrichedEntity = new CON020EnrichedEntity();
        con020EnrichedEntity.setHashKey(CON020EnrichedEntity.buildHashKeyForCon020EnrichedEntity(archiveUri, requestId, registeredLetterCode));
        con020EnrichedEntity.setSortKey(SORT_KEY);
        con020EnrichedEntity.setEntityName(ENRICHED_ENTITY_NAME);
        con020EnrichedEntity.setRecordCreationTime(now);
        con020EnrichedEntity.setLastModificationTime(now);
        con020EnrichedEntity.setMetadataPresent(true);
        con020EnrichedEntity.setTtl(now.plus(365, ChronoUnit.DAYS).toEpochMilli());

        CON020EnrichedEntityMetadata metadata = getCon020EnrichedEntityMetadata(payload, requestId, archiveUri);
        con020EnrichedEntity.setMetadata(metadata);

        return Mono.just(con020EnrichedEntity);
    }

    private static CON020EnrichedEntityMetadata getCon020EnrichedEntityMetadata(PaperEventEnricherInputEvent.Payload payload, String requestId, String archiveUri) {
        CON020EnrichedEntityMetadata metadata = new CON020EnrichedEntityMetadata();

        retrieveIunFromRequestId(metadata, requestId);
        retrieveRecIndexFromRequestId(metadata, requestId);

        metadata.setEventTime(payload.getEventTimestamp());
        metadata.setGenerationTime(payload.getAnalogMail().getStatusDateTime());
        metadata.setSendRequestId(payload.getAnalogMail().getRequestId());
        metadata.setRegisteredLetterCode(payload.getAnalogMail().getRegisteredLetterCode());
        metadata.setArchiveFileKey(archiveUri);
        return metadata;
    }

    public static void retrieveIunFromRequestId(CON020EnrichedEntityMetadata metadata, String requestId) {
        if (StringUtils.hasText(requestId)) {
            String regex = "IUN_([^.]+)\\.";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(requestId);

            if (matcher.find()) {
                metadata.setIun(matcher.group(1));
            } else {
                log.warn(ERROR_CODE_INVALID_REQUESTID + ": Iun format not valid in requestId [{}]", requestId);
            }
        }
    }

    public static void retrieveRecIndexFromRequestId(CON020EnrichedEntityMetadata metadata, String requestId) {
        if (StringUtils.hasText(requestId)) {
            String regex = "RECINDEX_([^.]+)\\.";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(requestId);

            if (matcher.find()) {
                metadata.setRecIndex(Integer.getInteger(matcher.group(1)));
            } else {
                log.warn(ERROR_CODE_INVALID_REQUESTID + ": RecIndex format not valid in requestId [{}]", requestId);
            }
        }
    }

    public static CON020ArchiveEntity createArchiveEntityForStatusUpdate(PaperArchiveEvent.Payload paperArchiveEvent, String status) {
        Instant now = Instant.now();
        String taskId = System.getenv("ECS_AGENT_URI");

        CON020ArchiveEntity con020ArchiveEntity = new CON020ArchiveEntity();
        con020ArchiveEntity.setHashKey(CON020ArchiveEntity.buildHashKeyForCon020ArchiveEntity(paperArchiveEvent.getArchiveFileKey()));
        con020ArchiveEntity.setSortKey("-");
        con020ArchiveEntity.setArchiveStatus(status);
        con020ArchiveEntity.setTtl(now.plus(365, ChronoUnit.DAYS).toEpochMilli());
        con020ArchiveEntity.setProcessingTask(taskId);
        con020ArchiveEntity.setLastModificationTime(now);

        return con020ArchiveEntity;
    }

    public static CON020EnrichedEntity createEnricherEntityForPrintedPdf(String fileKey, String archiveFileKey, String requestId, String registeredLetterCode) {
        CON020EnrichedEntity con020EnrichedEntity = new CON020EnrichedEntity();

        Instant now = Instant.now();

        con020EnrichedEntity.setHashKey(CON020EnrichedEntity.buildHashKeyForCon020EnrichedEntity(archiveFileKey, requestId, registeredLetterCode));
        con020EnrichedEntity.setSortKey(SORT_KEY);
        con020EnrichedEntity.setEntityName(ENRICHED_ENTITY_NAME);
        con020EnrichedEntity.setRecordCreationTime(now);
        con020EnrichedEntity.setLastModificationTime(now);
        con020EnrichedEntity.setMetadataPresent(Boolean.FALSE);
        con020EnrichedEntity.setPrintedPdf(fileKey);
        con020EnrichedEntity.setTtl(now.plus(365, ChronoUnit.DAYS).toEpochMilli());

        return con020EnrichedEntity;
    }

    public static byte[] getContent(ZipArchiveInputStream zipInputStream, String fileName) {
        try {
            return zipInputStream.readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read file [{}]", fileName, e);
            throw new PaperEventEnricherException(String.format("Failed to read file [%s]", fileName), 500, FAILED_TO_READ_FILE);
        }
    }

    public static byte[] getContent(InputStream zipInputStream, String fileName) {
        try {
            return zipInputStream.readAllBytes();
        } catch (IOException e) {
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
                String p7mEntryName = cells[0];
                String requestId = cells[3];
                String registeredLetterCode = cells[6];

                if (p7mEntryName.toLowerCase().endsWith(PDF.getValue())) {
                    IndexData indexData = new IndexData(requestId, registeredLetterCode, p7mEntryName);
                    archiveDetails.put(p7mEntryName, indexData);
                }
            }
        }
        return archiveDetails;
    }

}
