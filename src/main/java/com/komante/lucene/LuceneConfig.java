package com.komante.lucene;

import lombok.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class LuceneConfig {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Builder(toBuilder = true)
    @ToString
    public static class HighlightsConfig {
        private int fragmentSizeChars = 240;
        /** Max number of fragments per document */
        private int numberOfDocumentFragments = 12;
    }

    public static HighlightsConfig.HighlightsConfigBuilder highlightsConfigBuilder() {
        return new HighlightsConfig().toBuilder();
    }

    /** For IndexSearcher.search() methods. Default result size */
    private int maxSearchSize = 10000;

    /** Directory where all the indices will be stored (in their own directories) */
    private final String indexLocation;

    /** Should all indexes be recreated, unless parameter not overriden on index level. Deletes all index data!! */
    private final boolean recreateIndexes;

    private HighlightsConfig highlightsConfig;

    public LuceneConfig(@NonNull String indexLocation, boolean recreateIndexes) {
        this.indexLocation = indexLocation;
        this.recreateIndexes = recreateIndexes;
        highlightsConfig = new HighlightsConfig();
    }

    @Builder(toBuilder = true)
    private LuceneConfig optionals(int maxSearchSize, HighlightsConfig highlightsConfig) {
        this.maxSearchSize = maxSearchSize;
        this.highlightsConfig = highlightsConfig;
        return this;
    }
}
