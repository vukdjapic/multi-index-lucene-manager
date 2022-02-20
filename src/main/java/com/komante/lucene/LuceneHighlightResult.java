package com.komante.lucene;

import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @param <T> either Document or String document ID
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString
public class LuceneHighlightResult<T> {
  /**
   * static method, but has to be on instance, to know the type. turns list to map by first field
   */
  public static <T1> Map<T1, List<String>> listToMap(List<LuceneHighlightResult<T1>> highlightResultList, Class<T1> documentType) {
    Map<T1, List<String>> highlightsMap = new HashMap<>();
    for (LuceneHighlightResult<T1> highlight : highlightResultList) {
      highlightsMap.put(highlight.document, highlight.fragments);
    }
    return highlightsMap;
  }

  private T document;
  private List<String> fragments;
}
