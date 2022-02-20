package com.komante.lucene;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;

import java.io.IOException;

/**
 * One IndexHandle is created per directory to hold one open IndexWriter, and IndexSearcher that can be recreated
 * Clients don't access this class directly. It is used by LuceneService
 */
class IndexHandle {
  private Index index;
  @Getter
  private Directory directory;
  private IndexWriterConfig iwConfig;
  @Getter(AccessLevel.MODULE)
  private Analyzer analyzer;
  private IndexWriter writer;
  private IndexReader reader;
  private IndexSearcher searcher;

  public IndexHandle(Index index, Directory directory, IndexWriterConfig iwConfig, Analyzer analyzer) {
    this.index = index;
    this.directory = directory;
    this.analyzer = analyzer;
    openWriter(iwConfig);
  }

  @SneakyThrows(IOException.class)
  void closeResources() {
    if (writer != null && writer.isOpen()) {
      writer.close();
    }
    if (reader != null) {
      reader.close();
    }
  }

  IndexWriter getWriter() {
    if (writer != null && writer.isOpen()) {
      return writer;
    } else {
      throw new IllegalStateException("Index writer not opened for " + index);
    }
  }

  @SneakyThrows(IOException.class)
  synchronized void openSearcher() {
    reader = DirectoryReader.open(directory);
    searcher = new IndexSearcher(reader);
  }

  synchronized IndexSearcher getSearcher() {
    if (searcher == null) {
      openSearcher();
    }
    return searcher;
  }

  synchronized IndexReader getReader() {
    if (reader == null) {
      openSearcher();
    }
    return reader;
  }

  @SneakyThrows(IOException.class)
  private IndexWriter openWriter(IndexWriterConfig iwConfig) {
    writer = new IndexWriter(directory, iwConfig);
    writer.commit();
    return writer;
  }

}
