package com.komante.lucene;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LuceneConfigTest {

    @Test
    public void creation() {
        LuceneConfig luceneConfig = new LuceneConfig("/a/b", false);
        Assertions.assertEquals("/a/b", luceneConfig.getIndexLocation());
        Assertions.assertEquals(10000, luceneConfig.getMaxSearchSize());
        Assertions.assertEquals(12, luceneConfig.getHighlightsConfig().getNumberOfDocumentFragments());

        luceneConfig = new LuceneConfig("/a/c", true).toBuilder().maxSearchSize(20)
            .highlightsConfig(LuceneConfig.highlightsConfigBuilder().numberOfDocumentFragments(21).build()).build();
        Assertions.assertEquals(20, luceneConfig.getMaxSearchSize());
        Assertions.assertEquals(21, luceneConfig.getHighlightsConfig().getNumberOfDocumentFragments());

    }
}
