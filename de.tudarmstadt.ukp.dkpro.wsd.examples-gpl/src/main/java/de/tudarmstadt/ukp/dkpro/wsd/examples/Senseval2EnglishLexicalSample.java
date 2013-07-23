/*******************************************************************************
 * Copyright 2013
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.tudarmstadt.ukp.dkpro.wsd.examples;

import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.uimafit.factory.CollectionReaderFactory.createCollectionReader;
import static org.uimafit.factory.ExternalResourceFactory.createExternalResourceDescription;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.resource.ExternalResourceDescription;
import org.uimafit.pipeline.SimplePipeline;

import de.tudarmstadt.ukp.dkpro.wsd.algorithms.MostFrequentSenseBaseline;
import de.tudarmstadt.ukp.dkpro.wsd.algorithms.lesk.SimplifiedLesk;
import de.tudarmstadt.ukp.dkpro.wsd.algorithms.lesk.util.normalization.SecondObjects;
import de.tudarmstadt.ukp.dkpro.wsd.algorithms.lesk.util.overlap.PairedOverlap;
import de.tudarmstadt.ukp.dkpro.wsd.candidates.SenseMapper;
import de.tudarmstadt.ukp.dkpro.wsd.candidates.WordNetSenseKeyToSynset;
import de.tudarmstadt.ukp.dkpro.wsd.evaluation.EvaluationTableHTML;
import de.tudarmstadt.ukp.dkpro.wsd.evaluation.SingleExactMatchEvaluatorText;
import de.tudarmstadt.ukp.dkpro.wsd.io.reader.Senseval2LSReader;
import de.tudarmstadt.ukp.dkpro.wsd.io.reader.SensevalAnswerKeyReader;
import de.tudarmstadt.ukp.dkpro.wsd.resource.WSDResourceIndividualPOS;
import de.tudarmstadt.ukp.dkpro.wsd.resource.WSDResourceSimplifiedLesk;
import de.tudarmstadt.ukp.dkpro.wsd.si.lsr.LsrToWordNetSynsetOffset;
import de.tudarmstadt.ukp.dkpro.wsd.si.resource.LsrSenseInventoryResource;
import de.tudarmstadt.ukp.dkpro.wsd.wsdannotators.WSDAnnotatorContextPOS;
import de.tudarmstadt.ukp.dkpro.wsd.wsdannotators.WSDAnnotatorIndividualPOS;

/**
 * This class illustrates a pipeline which runs various WSD baselines on
 * the Senseval-2 English Lexical Sample training data.
 *
 * @author Tristan Miller <miller@ukp.informatik.tu-darmstadt.de>
 *
 */
public class Senseval2EnglishLexicalSample
{

    public static void main(String[] args)
        throws UIMAException, IOException
    {

        // For our corpus and answer key we will use the Senseval-2 English
        // Lexical Sample training data.
        final String directory = "classpath:/senseval-2/english-lex-sample/train/";
        final String corpus = directory + "eng-lex-sample.train.xml";
        final String answerkey = directory + "eng-lex-sample.train.fixed.key";

        // A collection reader for the documents to be disambiguated.
        CollectionReader reader = createCollectionReader(
                Senseval2LSReader.class, Senseval2LSReader.PARAM_FILE, corpus);

        // This AE reads the Senseval-2 answer key. Because the Senseval
        // answer key format doesn't itself indicate what sense inventory is
        // used for the keys, we need to pass this as a configuration parameter.
        // In this case, the keys use sense identifiers which are specific
        // to the Senseval task, so we shall arbitrarily name this sense
        // inventory "Senseval2_sensekey".
        AnalysisEngineDescription answerReader = createPrimitiveDescription(
                SensevalAnswerKeyReader.class,
                SensevalAnswerKeyReader.PARAM_FILE, answerkey,
                SensevalAnswerKeyReader.PARAM_SENSE_INVENTORY,
                "Senseval2_sensekey");

        // The Senseval2 sense identifiers are actually based on sense keys from
        // the WordNet 1.7-prerelease, so for ease of interoperability we use
        // this AE to convert them to WordNet 1.7-prerelease sense keys. We
        // have a delimited text file providing a mapping between the two
        // sense identifiers, which the SenseMapper annotator reads in and
        // uses to perform the conversion.
        AnalysisEngineDescription convertSensevalToSensekey = createPrimitiveDescription(
                SenseMapper.class, SenseMapper.PARAM_FILE,
                "classpath:/WordNet/wordnet_senseval.tsv",
                SenseMapper.PARAM_SOURCE_SENSE_INVENTORY_NAME, "Senseval2_sensekey",
                SenseMapper.PARAM_TARGET_SENSE_INVENTORY_NAME,
                "WordNet_1.7pre_sensekey", SenseMapper.PARAM_KEY_COLUMN, 2,
                SenseMapper.PARAM_VALUE_COLUMN, 1,
                SenseMapper.PARAM_IGNORE_UNKNOWN_SENSES, true);

        // WordNet 1.7-prerelease sense keys are not unique identifiers for
        // WordNet synsets (that is, multiple sense keys map to the same synset)
        // we use another annotator to convert them to strings comprised of the
        // WordNet synset offset plus part of speech. These strings uniquely
        // identify WordNet senses.
        AnalysisEngineDescription convertSensekeyToSynset = createPrimitiveDescription(
                WordNetSenseKeyToSynset.class,
                WordNetSenseKeyToSynset.PARAM_INDEX_SENSE_FILE,
                "classpath:/WordNet/WordNet_1.7pre/dict/index.sense",
                SenseMapper.PARAM_SOURCE_SENSE_INVENTORY_NAME,
                "WordNet_1.7pre_sensekey",
                SenseMapper.PARAM_TARGET_SENSE_INVENTORY_NAME, "WordNet_1.7pre_synset",
                SenseMapper.PARAM_IGNORE_UNKNOWN_SENSES, true);

        // The WSD baseline algorithms we will be using need to select senses
        // from a sense inventory. We will use JLSR as an interface to
        // the WordNet 1.7 prerelease. For this to work you will need
        // to have the WordNet 1.7 prerelease installed on your local system,
        // and to have an appropriately configured WordNet properties file and
        // DKPro resources.xml file.
        ExternalResourceDescription wordnet1_7 = createExternalResourceDescription(
                LsrSenseInventoryResource.class,
                LsrSenseInventoryResource.PARAM_RESOURCE_NAME, "wordnet17",
                LsrSenseInventoryResource.PARAM_RESOURCE_LANGUAGE, "en");

        // The sense identifiers returned by JLSR are also proprietary, so we
        // use this AE to convert them to strings comprised of the
        // WordNet 1.7-prerelease synset offset plus part of speech.
        AnalysisEngineDescription convertLSRtoSynset = createPrimitiveDescription(
                LsrToWordNetSynsetOffset.class,
                LsrToWordNetSynsetOffset.PARAM_SOURCE_SENSE_INVENTORY_NAME,
                "WordNet_3.0_LSR",
                LsrToWordNetSynsetOffset.PARAM_TARGET_SENSE_INVENTORY_NAME,
                "WordNet_1.7pre_synset");

        // Here's a resource encapsulating the most frequent sense baseline
        // algorithm, which we bind to the JLSR sense inventory.
        ExternalResourceDescription mfsBaselineResource = createExternalResourceDescription(
                WSDResourceIndividualPOS.class,
                WSDResourceIndividualPOS.SENSE_INVENTORY_RESOURCE, wordnet1_7,
                WSDResourceIndividualPOS.DISAMBIGUATION_METHOD,
                MostFrequentSenseBaseline.class.getName());

        // And here we create an analysis engine, and bind to it the
        // most frequent sense baseline resource.
        AnalysisEngineDescription mfsBaseline = createPrimitiveDescription(
                WSDAnnotatorIndividualPOS.class,
                WSDAnnotatorIndividualPOS.WSD_ALGORITHM_RESOURCE,
                mfsBaselineResource);

        // Here's a resource encapsulating the simplified Lesk algorithm (that
        // is, a Lesk-like algorithm where the word's definitions are compared
        // with the context in which the word appears.) The algorithm takes
        // three parameters: a tokenization strategy (that is, how to split a
        // string representing a context or a definition into tokens), an
        // overlap strategy (that is, how to compute the overlap between two
        // collections of tokens), and a a normalization strategy (that is, how
        // to normalize the value returned by the overlap measure).
        ExternalResourceDescription simplifiedLeskResource = createExternalResourceDescription(
                WSDResourceSimplifiedLesk.class,
                WSDResourceSimplifiedLesk.SENSE_INVENTORY_RESOURCE, wordnet1_7,
                WSDResourceSimplifiedLesk.PARAM_NORMALIZATION_STRATEGY,
                SecondObjects.class.getName(),
                WSDResourceSimplifiedLesk.PARAM_OVERLAP_STRATEGY,
                PairedOverlap.class.getName(),
                WSDResourceSimplifiedLesk.PARAM_TOKENIZATION_STRATEGY,
                EnglishStopLemmatizer.class.getName());

        // Next we create the analysis engine for the Lesk algorithm
        AnalysisEngineDescription simplifiedLesk = createPrimitiveDescription(
                WSDAnnotatorContextPOS.class,
                WSDAnnotatorContextPOS.WSD_METHOD_CONTEXT,
                simplifiedLeskResource);

        // This AE prints out detailed information on the AEs' sense
        // assignments.
        AnalysisEngineDescription writer = createPrimitiveDescription(
                EvaluationTableHTML.class,
                EvaluationTableHTML.PARAM_GOLD_STANDARD_ALGORITHM, answerkey,
                EvaluationTableHTML.PARAM_OUTPUT_FILE,
                "/tmp/Senseval2LS.html");

        // This AE compares the sense assignments of the SimplifiedLesk
        // algorithm against the given gold standard (in this case, the answer
        // key we read in) and computes and prints out useful statistics, such
        // as precision, recall, and coverage.
        AnalysisEngineDescription evaluator = createPrimitiveDescription(
                SingleExactMatchEvaluatorText.class,
                SingleExactMatchEvaluatorText.PARAM_GOLD_STANDARD_ALGORITHM,
                answerkey,
                SingleExactMatchEvaluatorText.PARAM_TEST_ALGORITHM, SimplifiedLesk.class.getName(),
                SingleExactMatchEvaluatorText.PARAM_BACKOFF_ALGORITHM, MostFrequentSenseBaseline.class.getName()
                //, SingleExactMatchEvaluator.PARAM_IGNORE_ALL_GOLD, "^[PU]$"
                );

        // Here we run the pipeline
        SimplePipeline.runPipeline(reader,
                answerReader,
                convertSensevalToSensekey,
                convertSensekeyToSynset,
                mfsBaseline,
                simplifiedLesk,
                convertLSRtoSynset,
                writer,
                evaluator);
    }

}
