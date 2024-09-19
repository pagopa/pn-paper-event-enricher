package it.pagopa.pn.paper.event.enricher.constant;

public class PaperEventEnricherConstant {
    public static final String X_CHECKSUM = "SHA-256";
    public static final String SORT_KEY = "-";
    public static final String ARCHIVE_HASH_KEY_PREFIX = "CON020AR~";
    public static final String ENRICHED_HASH_KEY_PREFIX = "CON020EN~";
    public static final String TASK_ID_ENV = "ECS_AGENT_URI";
    public static final String ARCHIVE_ENTITY_NAME = "CON020Archive";
    public static final String ENRICHED_ENTITY_NAME = "CON020Enriched";
    public static final String SAFE_STORAGE_PREFIX = "safestorage://";
    // SafeStorage status
    public static final String PRELOADED = "PRELOADED";
    public static final String SAVED = "SAVED";
    public static final String ATTACHED = "ATTACHED";


}
