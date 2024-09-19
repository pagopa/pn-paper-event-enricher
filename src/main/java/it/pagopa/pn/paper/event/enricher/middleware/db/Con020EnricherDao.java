package it.pagopa.pn.paper.event.enricher.middleware.db;

import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import reactor.core.publisher.Mono;

public interface Con020EnricherDao {
    Mono<CON020EnrichedEntity> updateMetadata(CON020EnrichedEntity entity);

    Mono<CON020EnrichedEntity> updatePrintedPdf(CON020EnrichedEntity entity);

}
