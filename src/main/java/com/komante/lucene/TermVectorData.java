package com.komante.lucene;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/** Term data returned from term vector call */
@Getter
@AllArgsConstructor
@ToString
public class TermVectorData {

    private String term;
    private long termFreq;
    private long docFreq;
    private long ttf;

}
