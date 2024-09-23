package it.pagopa.pn.paper.event.enricher;

import it.pagopa.pn.paper.event.enricher.middleware.queue.event.PaperArchiveEvent;
import it.pagopa.pn.paper.event.enricher.service.FileService;
import it.pagopa.pn.paper.event.enricher.service.PaperEventEnricherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;

@SpringBootApplication
public class PaperEventEnricherApplication {


    public static void main(String[] args) {
        SpringApplication.run(PaperEventEnricherApplication.class, args);
    }


    @RestController
    @RequestMapping("")
    public static class RootController {

        @Autowired
        private FileService fileService;

        @Autowired
        private PaperEventEnricherService paperEventEnricherService;

        @GetMapping("")
        public Mono<Void> home() {
            PaperArchiveEvent.Payload payload = PaperArchiveEvent.Payload.builder().archiveFileKey("PN_EXTERNAL_LEGAL_FACTS-4dc9cdda948b454dba02eb59b7dd7b93.bin").build();
            return paperEventEnricherService.handlePaperEventEnricherEvent(payload);
        }
    }
}