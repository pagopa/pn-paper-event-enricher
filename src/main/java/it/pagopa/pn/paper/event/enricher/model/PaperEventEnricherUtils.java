package it.pagopa.pn.paper.event.enricher.model;

import it.pagopa.pn.paper.event.enricher.exception.PaperEventEnricherException;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020ArchiveEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntityMetadata;
import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperEventEnricherInputEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.pagopa.pn.paper.event.enricher.exception.PnPaperEventEnricherExceptionCode.ERROR_CODE_INVALID_REQUESTID;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaperEventEnricherUtils {
    public static final String SORT_KEY = "-";
    public static final String ARCHIVE_HASH_KEY_PREFIX = "CON020AR~";
    public static final String ENRICHED_HASH_KEY_PREFIX = "CON020EN~";
    public static final String TASK_ID_ENV = "ECS_AGENT_URI";
    public static final String ARCHIVE_ENTITY_NAME = "CON020Archive";
    public static final String ENRICHED_ENTITY_NAME = "CON020Enriched";
    public static final String SAFE_STORAGE_PREFIX = "safestorage://";

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
                metadata.setRecIndex(matcher.group(1));
            } else {
                log.warn(ERROR_CODE_INVALID_REQUESTID + ": RecIndex format not valid in requestId [{}]", requestId);
            }
        }
    }
}
