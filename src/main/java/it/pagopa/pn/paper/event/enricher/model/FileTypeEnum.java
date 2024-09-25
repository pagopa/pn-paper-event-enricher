package it.pagopa.pn.paper.event.enricher.model;

import lombok.Getter;

@Getter
public enum FileTypeEnum {
    BOL(".bol"),
    PDF(".pdf"),
    P7M(".p7m");

    private final String value;

    FileTypeEnum(String value) {
        this.value = value;
    }

}
