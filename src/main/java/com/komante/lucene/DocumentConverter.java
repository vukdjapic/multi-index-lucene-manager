package com.komante.lucene;

import org.apache.lucene.document.Document;

/** Converts entity object to matching Lucene document */
public interface DocumentConverter<T> {
  Document getDocument(T entity);
}
