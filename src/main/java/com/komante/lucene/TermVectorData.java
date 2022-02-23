package com.komante.lucene;

import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/** Term data returned from term vector call */
@Getter
@ToString
public class TermVectorData {

    private String term;
    private long termFreq;
    private long docFreq;
    private long ttf;
    private List<TermOffset> offsets;

    public TermVectorData(String term, long termFreq, long docFreq, long ttf) {
        this.term = term;
        this.termFreq = termFreq;
        this.docFreq = docFreq;
        this.ttf = ttf;
        offsets = new ArrayList<>();
    }

    public void addOffset(int startOffset, int endOffset) {
        offsets.add(new TermOffset(startOffset, endOffset));
    }

    public List<TermOffset> getOffsets() {
        return new ArrayList<>(offsets);
    }
}
