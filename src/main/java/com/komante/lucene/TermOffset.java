package com.komante.lucene;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class TermOffset {
    private int startOffset;
    private int endOffset;
}
