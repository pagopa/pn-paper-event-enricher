package it.pagopa.pn.paper.event.enricher.exception;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PnPaperEventEnricherExceptionConstant {
    public static final String ERROR_CODE_PAPER_EVENT_ENRICHER_EVENTTYPENOTSUPPORTED = "PN_PAPER_EVENT_ENRICHER_EVENTTYPENOTSUPPORTED";
    public static final String ERROR_CODE_INVALID_REQUESTID = "PN_PAPER_EVENT_ENRICHER_INVALID_REQUESTID";
    public static final String ERROR_GET_FILE = "PN_PAPER_EVENT_ENRICHER_SAFE_STORAGE_GET_FILE_ERROR";
    public static final String FAILED_TO_READ_FILE = "FAILED_TO_READ_FILE";
    public static final String ERROR_MESSAGE_PAPER_EVENT_ENRICHER_EVENTTYPENOTSUPPORTED = "eventType not present, cannot start scheduled action.";
    public static final String UNABLE_TO_CREATE_TMP_FILE = "Unable to create tmp file";
    public static final String FILE_IS_TOO_SHORT = "File is too short";
    public static final String UNSUPPORTED_FILE_TYPE = "Unsupported file type";
    public static final String UNABLE_TO_WRITE_ON_TMP_FILE = "Unable to write on tmp file";
    public static final String ERROR_WHILE_COMPUTING_SHA_256_HASH = "Error while computing SHA-256 hash";
    public static final String DOWNLOAD_URL_IS_NULL = "Download url is null";

    public static final String ERROR_DELETING_TMP_FILE = "ERROR_DELETING_TMP_FILE";
    public static final String ERROR_DURING_WRITE_FILE = "ERROR_DURING_WRITE_FILE";
    public static final String ERROR_DURING_FILE_EXTRACTION_FROM_ARCHIVE = "ERROR_DURING_FILE_EXTRACTION_FROM_ARCHIVE";
    public static final String ERROR_WHILE_PARSING_P7M = "ERROR_WHILE_PARSING_P7M";

    public static final String INVALID_COUNTER_VALUE = "INVALID_COUNTER_VALUE";


}
