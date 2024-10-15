package it.pagopa.pn.paper.event.enricher.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
@Data
public class FileCounter {
    private AtomicInteger uploadedFiles;
    private AtomicInteger updatedItems;
    private int totalFiles;
}
