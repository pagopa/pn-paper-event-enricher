package it.pagopa.pn.paper.event.enricher.middleware.queue.event;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import lombok.*;

import javax.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PaperEventEnricherInputEvent implements GenericEvent<StandardEventHeader, PaperEventEnricherInputEvent.Payload> {

    private StandardEventHeader header;

    private Payload payload;

    @Getter
    @Builder
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {

        @NotEmpty
        private String clientId;

        @NotEmpty
        private Instant eventTimestamp;

        @NotEmpty
        private AnalogMailDetail analogMail;

        @Getter
        @Builder
        @ToString
        @EqualsAndHashCode
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AnalogMailDetail {

            @NotEmpty
            private String requestId;

            @NotEmpty
            private String registeredLetterCode;

            @NotEmpty
            private String productType;

            private String iun;

            @NotEmpty
            private String statusCode;

            @NotEmpty
            private String statusDescription;

            @NotEmpty
            private Instant statusDateTime;

            @NotEmpty
            private List<Attachment> attachments;

            @NotEmpty
            private String clientRequestTimeStamp;
        }

        @Getter
        @Builder
        @ToString
        @EqualsAndHashCode
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Attachment {

            @NotEmpty
            private String id;

            @NotEmpty
            private String documentType;

            @NotEmpty
            private String uri;

            @NotEmpty
            private String sha256;

            @NotEmpty
            private String date;
        }
    }
}
