package it.pagopa.pn.paper.event.enricher.middleware.db;

import it.pagopa.pn.paper.event.enricher.middleware.db.entities.CON020ArchiveEntity;
import reactor.core.publisher.Mono;

public interface Con020ArchiveDao {
    Mono<CON020ArchiveEntity> putIfAbsent(CON020ArchiveEntity entity);
    Mono<CON020ArchiveEntity> updateIfExists(CON020ArchiveEntity entity);
}
