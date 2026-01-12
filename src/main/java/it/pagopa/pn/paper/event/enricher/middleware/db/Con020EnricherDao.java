package it.pagopa.pn.paper.event.enricher.middleware.db;

import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020EnrichedEntity;
import it.pagopa.pn.paper.event.enricher.model.UpdateTypeEnum;
import reactor.core.publisher.Mono;

public interface Con020EnricherDao {

    Mono<CON020EnrichedEntity> update(CON020EnrichedEntity entity, UpdateTypeEnum type);

}
