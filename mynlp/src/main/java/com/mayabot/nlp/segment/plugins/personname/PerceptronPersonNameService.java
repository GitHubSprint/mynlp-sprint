package com.mayabot.nlp.segment.plugins.personname;

import com.mayabot.nlp.MynlpEnv;
import com.mayabot.nlp.common.injector.Singleton;
import com.mayabot.nlp.common.resources.NlpResource;
import com.mayabot.nlp.common.utils.CharNormUtils;
import com.mayabot.nlp.perceptron.FeatureSet;
import com.mayabot.nlp.segment.plugins.ner.PerceptronNerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * 感知机分词服务
 */
@Singleton
public class PerceptronPersonNameService {

    private PersonNamePerceptron perceptron;

    static Logger logger = LoggerFactory.getLogger(PerceptronNerService.class);

    public PerceptronPersonNameService(MynlpEnv mynlp) throws Exception {

        long t1 = System.currentTimeMillis();
        NlpResource parameterResource = mynlp.loadResource("person-name-model/parameter.bin");
        NlpResource featureResource = mynlp.loadResource("person-name-model/feature.txt");

        File temp = new File(mynlp.getCacheDir(), "ner");

        File featureDatFile = new File(temp, featureResource.hash() + ".personName.dat");
        if (!featureDatFile.getParentFile().exists()) {
            featureDatFile.getParentFile().mkdirs();
        }

        if (!featureDatFile.exists()) {
            FeatureSet featureSet = FeatureSet.readFromText(new BufferedInputStream(featureResource.inputStream()));
            featureSet.save(featureDatFile, null);
        }
        this.perceptron = PersonNamePerceptron.load(
                parameterResource.inputStream(),
                new BufferedInputStream(new FileInputStream(featureDatFile), 8 * 1024));

        long t2 = System.currentTimeMillis();

        logger.info("PerceptronPersonNameService load use " + (t2 - t1) + " ms");

    }

    public List<PersonName> findName(String sentence) {
        char[] chars = sentence.toCharArray();
        CharNormUtils.convert(chars);
        return perceptron.findPersonName(chars);
    }

    public List<PersonName> findName(char[] sentence) {
        return perceptron.findPersonName(sentence);
    }

}
