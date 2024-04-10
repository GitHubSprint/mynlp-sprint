package com.mayabot.nlp.segment.lexer.perceptron;

import com.mayabot.nlp.MynlpConfigs;
import com.mayabot.nlp.MynlpEnv;
import com.mayabot.nlp.common.injector.Singleton;
import com.mayabot.nlp.common.resources.NlpResource;
import com.mayabot.nlp.segment.plugins.ner.PerceptronNerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 感知机分词服务
 */
@Singleton
public class PerceptronsSegmentService {

    private PerceptronSegment ps;


    static Logger logger = LoggerFactory.getLogger(PerceptronNerService.class);

    public PerceptronsSegmentService(MynlpEnv mynlp,
                                     PerceptronSegmentPatch perceptronSegmentPatch) throws Exception {

        //cws-model or cws-hanlp-model
        String modelName = mynlp.get(MynlpConfigs.cwsModelItem);

        long t1 = System.currentTimeMillis();
        NlpResource parameterResource = mynlp.loadResource(modelName + "/parameter.bin");
        NlpResource featureResource = mynlp.loadResource(modelName + "/feature.dat");

        ps = PerceptronSegment.load(
                parameterResource.inputStream(),
                featureResource.inputStream());

        for (String example : perceptronSegmentPatch.getExamples()) {
            ps.learn(example);
        }

        long t2 = System.currentTimeMillis();

        logger.info("PerceptronCwsService init use " + (t2 - t1) + " ms");
    }

    public List<String> splitWord(String sentence) {
        return ps.decode(sentence);
    }

    /**
     * 词使用空格分开。
     * @param example
     */
    public void learn(String example){
        ps.learn(example);
    }

    public PerceptronSegment getPerceptron() {
        return ps;
    }
}
