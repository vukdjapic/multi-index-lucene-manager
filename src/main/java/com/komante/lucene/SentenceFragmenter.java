package com.komante.lucene;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to extract natural (sentence bound) fragment from a text fragment and to highlight selected terms in it
 */
class SentenceFragmenter {
    /** finds first sentence beginning (after broken sentence at start */
    public static final Pattern SENTENCE_BEGINNING_PATTERN = Pattern.compile("^[^.!?]*[.!?][.\\s]*");
    public static final Pattern SENTENCE_END_PATTERN = Pattern.compile("[.!?][^.!?]*$");

    public static final String HIGHLIGHT_START_TAG = "<em>";
    public static final String HIGHLIGHT_END_TAG = "</em>";
    private static final int HIGHLIGHT_START_TAG_LENGTH = 4;
    private static final int HIGHLIGHT_END_TAG_LENGTH = 5;

    private int highlightsFragmentHalfSize;

    public SentenceFragmenter(LuceneConfig.HighlightsConfig highlightsConfig) {
        this.highlightsFragmentHalfSize = highlightsConfig.getFragmentSizeChars()/2;
    }

    public String getSentenceFragment(String text, String term, int termStartOffset, int termEndOffset) {
        int textLength = text.length();

        //if term is near beginning or end, ensure it doesn't go out of string bounds
        int realStartOffset = termStartOffset > highlightsFragmentHalfSize ? termStartOffset - highlightsFragmentHalfSize : 0;
        int realEndOffset = termEndOffset > textLength - highlightsFragmentHalfSize ? textLength :
            termEndOffset + highlightsFragmentHalfSize;
        //raw fragment with highlighted term
        String fragment = text.substring(realStartOffset, realEndOffset);
        int fragmentTermStartOffset = termStartOffset - realStartOffset;
        int fragmentTermEndOffset = termEndOffset - realStartOffset;

        fragment = new StringBuilder().append(fragment.substring(0, fragmentTermStartOffset)).append(HIGHLIGHT_START_TAG)
            .append(fragment.substring(fragmentTermStartOffset, fragmentTermEndOffset)).append(HIGHLIGHT_END_TAG)
            .append(fragment.substring(fragmentTermEndOffset)).toString();

        fragmentTermEndOffset += HIGHLIGHT_START_TAG_LENGTH + HIGHLIGHT_END_TAG_LENGTH;

        //first try to trim at sentence boundaries
        boolean trimmedLeft = false;
        boolean trimmedRight = false;
        Matcher matcher = SENTENCE_BEGINNING_PATTERN.matcher(fragment);
        if (matcher.find()) {
            int sentenceBeginning = matcher.end();
            if (sentenceBeginning < fragmentTermStartOffset) {
                fragment = fragment.substring(sentenceBeginning);
                trimmedLeft = true;
            }
        }
        matcher = SENTENCE_END_PATTERN.matcher(fragment);
        if (matcher.find()) {
            int sentenceEnd = matcher.start() + 1;
            if (sentenceEnd > termEndOffset - realStartOffset) {
                fragment = fragment.substring(0, sentenceEnd);
                trimmedRight = true;
            }
        }

        //second try to trim at word boundaries
        if (!trimmedLeft) {
            int indexOfSpace = fragment.indexOf(' ');
            int trimLeftIndex = indexOfSpace != -1 && indexOfSpace < fragmentTermStartOffset ? indexOfSpace : -1;
            if (trimLeftIndex > 0) {
                fragment = fragment.substring(trimLeftIndex);
            }
        }
        if (!trimmedRight) {
            int indexOfSpace = fragment.lastIndexOf(' ');
            int trimRightIndex = indexOfSpace != -1 && indexOfSpace > fragmentTermEndOffset ? indexOfSpace : -1;
            if (trimRightIndex > 0) {
                fragment = fragment.substring(0, trimRightIndex);
            }
        }
        return fragment;
    }

}
