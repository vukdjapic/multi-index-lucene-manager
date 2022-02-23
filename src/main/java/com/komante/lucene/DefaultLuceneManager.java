package com.komante.lucene;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class DefaultLuceneManager implements LuceneManager {

    private LuceneConfig luceneConfig;
    private Path indexPath;
    private Map<Index, IndexHandle> indexHandles;
    private boolean recreateIndexes;

    public DefaultLuceneManager(LuceneConfig luceneConfig) {
        String indexLocation = luceneConfig.getIndexLocation();
        boolean recreateIndexes = luceneConfig.isRecreateIndexes();
        indexPath = Paths.get(indexLocation);
        indexHandles = new HashMap<>();
        this.recreateIndexes = recreateIndexes;
        this.luceneConfig = luceneConfig;
        log.debug("Starting Lucene from: {}, with recreate: {}", indexLocation, recreateIndexes);
        if (!Files.isDirectory(indexPath)) {
            throw new IllegalArgumentException("Directory for indices doesn't exist: " + indexPath);
        }
    }

    @Override
    public void closeLuceneResources() {
        log.debug("Closing writers and readers");
        for (IndexHandle indexHandle : indexHandles.values()) {
            indexHandle.closeResources();
        }
    }

    @Override
    public void openIndex(Index index, Analyzer analyzer) {
        openIndex(index, analyzer, recreateIndexes);
    }


    @SneakyThrows(IOException.class)
    @Override
    public void openIndex(Index index, Analyzer analyzer, boolean recreate) {
        Directory dir;
        dir = FSDirectory.open(indexPath.resolve(index.getName()));
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        if (recreate) {
            log.info("Opening {} in create mode. Recreated.", index);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        } else {
            log.info("Opening {} in append mode", index);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        }

        // Optional: for better indexing performance, if you
        // are indexing many documents, increase the RAM
        // buffer. But if you do this, increase the max heap
        // size to the JVM (eg add -Xmx512m or -Xmx1g):
        // TODO
        // iwc.setRAMBufferSizeMB(256.0);

        if (indexHandles.containsKey(index)) {
            throw new IllegalStateException("Index already present in index handles:" + index);
        }
        indexHandles.put(index, new IndexHandle(index, dir, iwc, analyzer));
    }

    @Override
    @SneakyThrows(IOException.class)
    public void indexDocument(Index index, Document document, boolean commit) {
        IndexHandle indexHandle = getIndexHandle(index);
        IndexWriter indexWriter = indexHandle.getWriter();
        indexWriter.addDocument(document);
        if (commit) {
            indexWriter.commit();
        }
    }

    @Override
    @SneakyThrows(IOException.class)
    public void indexDocuments(Index index, List<Document> documents, boolean commit) {
        IndexHandle indexHandle = getIndexHandle(index);
        IndexWriter indexWriter = indexHandle.getWriter();
        for (Document doc : documents) {
            indexWriter.addDocument(doc);
        }
        if (commit) {
            indexWriter.commit();
        }
    }

    @Override
    public void refreshSearcher(Index index) {
        getIndexHandle(index).openSearcher();
    }

    @Override
    public List<Document> search(Index index, Query query) {
        return search(index, query, false);
    }

    @Override
    @SneakyThrows(IOException.class)
    public List<Document> search(Index index, Query query, boolean refreshSearcher) {
        IndexHandle indexHandle = getIndexHandle(index);
        if (refreshSearcher) {
            refreshSearcher(index);
        }
        IndexSearcher searcher = indexHandle.getSearcher();
        TopDocs topDocs = searcher.search(query, luceneConfig.getMaxSearchSize());
        ScoreDoc[] hits = topDocs.scoreDocs;
        int numTotalHits = Math.toIntExact(topDocs.totalHits.value);
        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < hits.length; i++) {
            docs.add(searcher.doc(hits[i].doc));
        }
        return docs;
    }

    @Override
    @SneakyThrows(IOException.class)
    public void deleteDocuments(Index index, Query query) {
        IndexHandle indexHandle = getIndexHandle(index);
        indexHandle.getWriter().deleteDocuments(query);
    }

    @Override
    @SneakyThrows(IOException.class)
    public List<LuceneHighlightResult<Document>> getHighlights(Index index, Query query) {
        List<LuceneHighlightResult<Document>> highlightResults = new ArrayList<>();
        IndexHandle indexHandle = getIndexHandle(index);
        IndexSearcher searcher = indexHandle.getSearcher();
        TopDocs topDocs = searcher.search(query, luceneConfig.getMaxSearchSize());
        Formatter formatter = new SimpleHTMLFormatter("<em>", "</em>");
        //It scores text fragments by the number of unique query terms found
        //Basically the matching score in layman terms
        QueryScorer scorer = new QueryScorer(query);
        //used to markup highlighted terms found in the best sections of a text
        Highlighter highlighter = new Highlighter(formatter, scorer);
        //It breaks text up into same-size texts but does not split up spans
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, luceneConfig.getHighlightsConfig().getFragmentSizeChars());
        highlighter.setTextFragmenter(fragmenter);
        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            int docid = topDocs.scoreDocs[i].doc;
            Document doc = searcher.doc(docid);
            String text = doc.get("content");
            TokenStream stream = TokenSources.getAnyTokenStream(indexHandle.getReader(), docid, "content", indexHandle.getAnalyzer());
            try {
                TextFragment[] bestTextFragments = highlighter.getBestTextFragments(stream, text, false, luceneConfig.getHighlightsConfig()
                    .getNumberOfDocumentFragments());
                List<String> fragmentsList = Stream.of(bestTextFragments).map(TextFragment::toString).collect(Collectors.toList());
                highlightResults.add(new LuceneHighlightResult(doc, fragmentsList));
            } catch (InvalidTokenOffsetsException e) {
                throw new RuntimeException("Error getting highlighted fragments", e);
            }
        }
        return highlightResults;
    }

    /**
     * Full implementation of highlights without Lucene Highlighter and Fragmenter - which haven't produced satisfying results. Implemented
     * with TokenStream, source text, and custom made sentence aware highlighter and fragmenter.
     * @param query                  used to search for documents
     * @param highlightTermPredicate used to determine term to highlight. Another form of query, to return boolean
     */
    @Override
    @SneakyThrows(IOException.class)
    public List<LuceneHighlightResult<Document>> getCustomHighlights(Index index, Query query, Predicate<String> highlightTermPredicate) {
        List<LuceneHighlightResult<Document>> highlightResults = new ArrayList<>();
        SentenceFragmenter sentenceFragmenter = new SentenceFragmenter(luceneConfig.getHighlightsConfig());
        IndexHandle indexHandle = getIndexHandle(index);
        IndexSearcher searcher = indexHandle.getSearcher();
        TopDocs topDocs = searcher.search(query, luceneConfig.getMaxSearchSize());

        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            int docId = topDocs.scoreDocs[i].doc;
            Document doc = searcher.doc(docId);
            String text = doc.get("content");
            int textLength = text.length();
            Fields termVectors = indexHandle.getReader().getTermVectors(docId);
            TokenStream stream = TokenSources.getTermVectorTokenStreamOrNull("content", termVectors, -1);
            CharTermAttribute charTermAttribute = stream.addAttribute(CharTermAttribute.class);
            OffsetAttribute offsetAttribute = stream.addAttribute(OffsetAttribute.class);

            stream.reset();
            List<String> fragmentsList = new ArrayList<>();
            while (stream.incrementToken()) {
                String term = charTermAttribute.toString();
                if (highlightTermPredicate.test(term)) {
                    int startOffset = offsetAttribute.startOffset();
                    int endOffset = offsetAttribute.endOffset();
                    String fragment = sentenceFragmenter.getSentenceFragment(text, term, startOffset, endOffset);
                    fragmentsList.add(fragment);
                    if (fragmentsList.size() >= luceneConfig.getHighlightsConfig().getNumberOfDocumentFragments()) {
                        break;
                    }
                }
            }
            highlightResults.add(new LuceneHighlightResult<Document>(doc, fragmentsList));
        }

        return highlightResults;
    }

    /**
     * Returns data for each term in one document from one field
     * @param index     document's index
     * @param idField   for a document, with unique values
     * @param idValue   of a document
     * @param termField from where to take terms
     */
    @Override
    @SneakyThrows
    public List<TermVectorData> getDocumentTerms(Index index, String idField, String idValue, String termField, boolean includeOffsets) {
        IndexHandle indexHandle = getIndexHandle(index);
        IndexReader reader = indexHandle.getReader();
        TermQuery query = new TermQuery(new Term(idField, idValue));
        Optional<Integer> uniqueDocumentNumber = getUniqueDocumentNumber(index, query);
        if (uniqueDocumentNumber.isEmpty()) {
            return new ArrayList<>();
        }
        int docNumber = uniqueDocumentNumber.get();
        Terms termVector = reader.getTermVector(docNumber, termField);
        TermsEnum termsEnum = termVector.iterator();
        BytesRef term = null;
        List<TermVectorData> termsData = new ArrayList<>();
        while ((term = termsEnum.next()) != null) {
            String termStr = term.utf8ToString();
            long termFreq = termsEnum.totalTermFreq();   //this only return frequency in this doc
            //long docCount = termsEnum.docFreq();   //docCount = 1 in all cases
            Term termInstance = new Term(termField, termStr);
            long totalTermFreq = reader.totalTermFreq(termInstance);
            long docFreq = reader.docFreq(termInstance);
            TermVectorData termVectorData = new TermVectorData(termStr, termFreq, docFreq, totalTermFreq);
            if (includeOffsets) {
                PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.OFFSETS);
                postingsEnum.nextDoc();
                int freq = postingsEnum.freq();
                for (int i = 0; i < freq; i++) {
                    postingsEnum.nextPosition();
                    termVectorData.addOffset(postingsEnum.startOffset(), postingsEnum.endOffset());
                }
            }
            termsData.add(termVectorData);
        }
        return termsData;
    }

    @SneakyThrows(IOException.class)
    private List<Integer> getDocumentNumbers(Index index, Query query) {
        IndexHandle indexHandle = getIndexHandle(index);
        IndexSearcher searcher = indexHandle.getSearcher();
        TopDocs topDocs = searcher.search(query, luceneConfig.getMaxSearchSize());
        ScoreDoc[] hits = topDocs.scoreDocs;
        List<Integer> docNumbers = Stream.of(hits).map(sd -> sd.doc).collect(Collectors.toList());
        return docNumbers;
    }

    private Optional<Integer> getUniqueDocumentNumber(Index index, Query uniqueQuery) {
        List<Integer> docNumbers = getDocumentNumbers(index, uniqueQuery);
        int docNumber;
        switch (docNumbers.size()) {
            case 0:
                return Optional.empty();
            case 1:
                return Optional.of(docNumbers.get(0));
            default:
                throw new IllegalStateException("Query should return unique results for term vectors");
        }
    }


    private IndexHandle getIndexHandle(Index index) {
        IndexHandle indexHandle = indexHandles.get(index);
        if (indexHandle == null) {
            throw new IllegalStateException("Index doesn't exist in indexHandles:" + index);
        }
        return indexHandle;
    }
}
