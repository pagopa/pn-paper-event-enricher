package it.pagopa.pn.paper.event.enricher.model;

import lombok.Getter;

@Getter
public enum FileTypeEnum {
    BIN(".bin"),
    BOL(".bol"),
    PDF(".pdf"),
    P7M(".p7m"),
    ZIP(".zip"),
    SEVENZIP(".7z");

    private final String value;

    FileTypeEnum(String value) {
        this.value = value;
    }

}
