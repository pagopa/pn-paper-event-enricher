package it.pagopa.pn.paper.event.enricher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class PaperEventEnricherApplication {


    public static void main(String[] args) {
        SpringApplication.run(PaperEventEnricherApplication.class, args);
    }


    @RestController
    @RequestMapping("")
    public static class RootController {

        @GetMapping("")
        public String home() {
            return "";
        }
    }
}