package org.getalp.lexsema.supervised.experiments;


import org.getalp.lexsema.io.annotresult.ConfigurationWriter;
import org.getalp.lexsema.io.annotresult.SemevalWriter;
import org.getalp.lexsema.io.document.loader.CorpusLoader;
import org.getalp.lexsema.io.document.loader.SemCorCorpusLoader;
import org.getalp.lexsema.io.document.loader.Semeval2013Task12CorpusLoader;
import org.getalp.lexsema.io.resource.LRLoader;
import org.getalp.lexsema.io.resource.babelnet.BabelNetAPILoader;
import org.getalp.lexsema.ml.supervised.weka.NaiveBayesSetUp;
import org.getalp.lexsema.similarity.Document;
import org.getalp.lexsema.supervised.WekaDisambiguator;
import org.getalp.lexsema.supervised.features.*;
import org.getalp.lexsema.supervised.features.extractors.AggregateLocalTextFeatureExtractor;
import org.getalp.lexsema.supervised.features.extractors.LemmaFeatureExtractor;
import org.getalp.lexsema.supervised.features.extractors.LocalCollocationFeatureExtractor;
import org.getalp.lexsema.supervised.features.extractors.PosFeatureExtractor;
import org.getalp.lexsema.util.Language;
import org.getalp.lexsema.wsd.configuration.Configuration;
import org.getalp.lexsema.wsd.method.Disambiguator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Semeval2013BabelNetWekaDisambiguationTranslation {
    private static Logger logger = LoggerFactory.getLogger(Semeval2013BabelNetWekaDisambiguationTranslation.class);

    public static void main(String[] args) throws IOException {
        CorpusLoader dl = new Semeval2013Task12CorpusLoader(args[0])
                .loadNonInstances(false);
        CorpusLoader semCor = new SemCorCorpusLoader(args[1]);
        LRLoader lrloader = new BabelNetAPILoader(Language.FRENCH).extendedSignature(false).shuffle(false).loadDefinitions(false).loadRelated(false);

        semCor.load();
        WindowLoader wloader = new DocumentCollectionWindowLoader(semCor);
        wloader.load();

        logger.info("Loading texts");
        dl.load();

        logger.info("Extracting features");
        List<ContextWindow> contextWindows = new ArrayList<>();
        contextWindows.add(new ContextWindowImpl(-1, -1));
        contextWindows.add(new ContextWindowImpl(1, 1));
        contextWindows.add(new ContextWindowImpl(-2, -2));
        contextWindows.add(new ContextWindowImpl(2, 2));
        contextWindows.add(new ContextWindowImpl(-2, -1));
        contextWindows.add(new ContextWindowImpl(-1, 1));
        contextWindows.add(new ContextWindowImpl(1, 2));
        contextWindows.add(new ContextWindowImpl(-3, -1));
        contextWindows.add(new ContextWindowImpl(-2, 1));
        contextWindows.add(new ContextWindowImpl(-1, 2));
        contextWindows.add(new ContextWindowImpl(1, 3));
        LocalCollocationFeatureExtractor lcfe = new LocalCollocationFeatureExtractor(contextWindows);
        PosFeatureExtractor pfe = new PosFeatureExtractor(5, 5);
        LemmaFeatureExtractor acfe = new LemmaFeatureExtractor(5, 5);


        AggregateLocalTextFeatureExtractor altfe = new AggregateLocalTextFeatureExtractor();
        altfe.addExtractor(lcfe);
        altfe.addExtractor(pfe);
        altfe.addExtractor(acfe);

        TrainingDataExtractor trainingDataExtractor = new BabelLemmaTranslateSemCorTrainingDataExtractor(altfe, new File(args[2]), Language.FRENCH);
        //TrainingDataExtractor trainingDataExtractor = new SemCorTrainingDataExtractor(altfe);
        trainingDataExtractor.extract(semCor);

        //Le dernier argument est la taille de la poole de threads
        // pour changer echo ou echo 2 changer dans EchoLexicalEntryDisambiguator
        Disambiguator disambiguator = new WekaDisambiguator("../data/supervised", new NaiveBayesSetUp(true, true), altfe, 2, trainingDataExtractor);
        int i = 0;
        if (args.length == 1) {
            i = Integer.valueOf(args[0]) - 1;
        }
        for (Document d : dl) {
            System.err.println("Starting document " + d.getId());
            System.err.println("\tLoading senses...");
            lrloader.loadSenses(d);
            Configuration c = disambiguator.disambiguate(d);
            //c = firstSense.disambiguate(d,c);
            ConfigurationWriter sw = new SemevalWriter(d.getId() + ".ans", "\t");
            System.err.println("\n\tWriting results...");
            sw.write(d, c.getAssignments());
            System.err.println("done!");
        }
        disambiguator.release();
        //disambiguator.release();
    }
}
