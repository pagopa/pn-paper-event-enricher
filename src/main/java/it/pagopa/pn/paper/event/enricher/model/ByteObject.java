package it.pagopa.pn.paper.event.enricher.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ByteObject {
    FileTypeEnum fileType;
    byte[] content;
}
