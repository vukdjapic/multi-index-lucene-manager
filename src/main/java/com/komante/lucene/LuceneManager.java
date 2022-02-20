package com.komante.lucene;

import lombok.SneakyThrows;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main entry point to MILManager operations.
 * Holds map of index handles, one per index. And uses them to do index/search operations. Clients access this service, not indexHandles
 * directly.
 */

public interface LuceneManager {

    /** Closes writers and readers */
    void closeLuceneResources();

    /** Overloaded method, uses predefined recreate option */
    void openIndex(Index index, Analyzer analyzer);

    void openIndex(Index index, Analyzer analyzer, boolean recreate);

    void indexDocument(Index index, Document document, boolean commit);

    void indexDocuments(Index index, List<Document> documents, boolean commit);

    void refreshSearcher(Index index);

    /**Searches index without opening new searcher */
    List<Document> search(Index index, Query query);

    List<Document> search(Index index, Query query, boolean newSearcher);

    void deleteDocuments(Index index, Query query);

    List<LuceneHighlightResult<Document>> getHighlights(Index index, Query query);

    /**
     * Full implementation of highlights without Lucene Highlighter and Fragmenter - which haven't produced satisfying results. Implemented
     * with TokenStream, source text, and custom made sentence aware highlighter and fragmenter.
     * @param query                  used to search for documents
     * @param highlightTermPredicate used to determine term to highlight. Another form of query, to return boolean
     */
    List<LuceneHighlightResult<Document>> getCustomHighlights(Index index, Query query, Predicate<String> highlightTermPredicate);

    /**
     * Returns data for each term in one document from one field
     * @param index     document's index
     * @param idField   for a document, with unique values
     * @param idValue   of a document
     * @param termField from where to take terms
     */
    List<TermVectorData> getDocumentTerms(Index index, String idField, String idValue, String termField);
}
