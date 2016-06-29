package org.getalp.lexsema.similarity.signatures.enrichment;

import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.getalp.lexsema.similarity.signatures.SemanticSignature;
import org.getalp.lexsema.similarity.signatures.SemanticSignatureImpl;
import org.getalp.lexsema.similarity.signatures.symbols.SemanticSymbol;
import org.getalp.lexsema.similarity.signatures.symbols.SemanticSymbolImpl;
import org.getalp.lexsema.util.Language;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Word2VecLocalSignatureEnrichment extends SignatureEnrichment {

    public static final int DEFAULT_TOP_N = 10;
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("\\p{Punct}");

    private final WordVectors word2Vec;
    private final int topN;
    
    private static final HashMap<String, List<SemanticSymbol>> symbolsCache = new HashMap<>();


    public Word2VecLocalSignatureEnrichment(WordVectors word2Vec) {
        this(word2Vec, DEFAULT_TOP_N);
    }

    public Word2VecLocalSignatureEnrichment(WordVectors word2Vec, int topN) {
        this.word2Vec = word2Vec;
        this.topN = topN;
    }

    private List<SemanticSymbol> enrichSemanticSymbol(SemanticSymbol semanticSymbol) {
        String word = semanticSymbol.getSymbol();
        if (symbolsCache.containsKey(word) && symbolsCache.get(word).size() >= topN) {
            return symbolsCache.get(word).subList(0, topN);
        }
        final Matcher matcher = PUNCTUATION_PATTERN.matcher(word);
        Collection<String> related = word2Vec.wordsNearest(matcher.replaceAll(""), topN);
        List<String> relatedSorted = sortRelatedList(word, related);
        List<SemanticSymbol> symbols = new ArrayList<>();
        symbols.add(semanticSymbol);
        for (String sword : relatedSorted) {
            symbols.add(new SemanticSymbolImpl(sword, 1.0));
        }
        symbolsCache.put(word, symbols);
        return symbols;
    }

    @Override
    public SemanticSignature enrichSemanticSignature(SemanticSignature semanticSignature) {
        SemanticSignature newSignature = new SemanticSignatureImpl();
        for (SemanticSymbol semanticSymbol : semanticSignature) {
            newSignature.addSymbols(enrichSemanticSymbol(semanticSymbol));
        }
        return newSignature;
    }

    public List<String> sortRelatedList(String word, Collection<String> related) {
        List<String> relatedSorted = new ArrayList<>(related);
        relatedSorted.sort(new Comparator<String>(){
            public int compare(String arg0, String arg1) {
                if (word2Vec.similarity(word, arg0) > word2Vec.similarity(word, arg1)) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        return relatedSorted;
    }
}
