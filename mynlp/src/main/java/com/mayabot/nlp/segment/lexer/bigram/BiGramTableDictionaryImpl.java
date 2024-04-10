/*
 * Copyright 2018 mayabot.com authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mayabot.nlp.segment.lexer.bigram;

import com.mayabot.nlp.MynlpEnv;
import com.mayabot.nlp.common.EncryptionUtil;
import com.mayabot.nlp.common.Guava;
import com.mayabot.nlp.common.TreeBasedTable;
import com.mayabot.nlp.common.injector.Singleton;
import com.mayabot.nlp.common.matrix.CSRSparseMatrix;
import com.mayabot.nlp.common.resources.NlpResource;
import com.mayabot.nlp.common.resources.UseLines;
import com.mayabot.nlp.common.utils.CharSourceLineReader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

/**
 * 核心词典的二元接续词典，采用整型储存，高性能。
 * 表示一个词接着另外一个词的概率次数
 *
 * @author jimichan
 */
@Singleton
public class BiGramTableDictionaryImpl extends BaseExternalizable implements BiGramTableDictionary {

    private final MynlpEnv mynlp;

    private final CoreDictPatch coreDictPatch;
    private CSRSparseMatrix matrix;

    public static final String path = "core-dict/CoreDict.bigram.txt";

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @Nullable
    private final CoreDictionary coreDictionary;

    private boolean loadFromBinCache = true;

    public BiGramTableDictionaryImpl(CoreDictionary coreDictionary,
                                     MynlpEnv mynlp,
                                     CoreDictPathWrap coreDictPathWrap) throws Exception {
        super(mynlp);
        this.coreDictionary = coreDictionary;
        this.mynlp = mynlp;
        coreDictPatch = coreDictPathWrap.getCoreDictPatch();

        if(coreDictPathWrap != null){
            loadFromBinCache = false;
        }

        this.refresh();
    }

    /**
     * 刷新资源
     *
     * @throws Exception
     */
    @Override
    public void refresh() throws Exception {
        if(loadFromBinCache){
            this.restore();
        }else{
            loadFromSource();
        }
    }

    @Override
    public String sourceVersion() {

        StringBuilder sb = new StringBuilder();
        sb.append(mynlp.hashResource(path));
        sb.append("v2");

//        if (coreDictPatch != null) {
//            sb.append(coreDictPatch.biGramVersion());
//        }

        return EncryptionUtil.md5(sb.toString());

    }

    @Override
    public void loadFromSource() throws Exception {

        NlpResource source = mynlp.loadResource(path);

        if (source == null) {
            throw new NullPointerException();
        }

        TreeBasedTable table = new TreeBasedTable();

        String firstWord = null;
        int count = 0;

        try (CharSourceLineReader reader = UseLines.lineReader(source.inputStream())) {
            while (reader.hasNext()) {
                String line = reader.next();

                if (line.startsWith("\t")) {
                    int firstWh = line.indexOf(" ");
                    String numString = line.substring(1, firstWh);
                    int num = Integer.parseInt(numString);
                    List<String> words = Guava.split(line.substring(firstWh + 1), " ");

                    String wordA = firstWord;
                    int idA = coreDictionary.wordId(wordA);
                    if (idA == -1) {
                        continue;
                    }
                    for (String wordB : words) {
                        int idB = coreDictionary.wordId(wordB);
                        if (idB >= 0) {
                            table.put(idA, idB, num);
                            count++;
                        }
                    }

                } else {
                    firstWord = line;
                }

            }
        }


        if (coreDictPatch != null) {
            List<BiGram> list = coreDictPatch.appendBiGram();
            if (list != null) {
                for (BiGram item : list) {
                    int idA = coreDictionary.wordId(item.getWordA());
                    int idB = coreDictionary.wordId(item.getWordB());
                    if (idA >= 0 && idB >= 0) {
                        table.put(idA, idB, item.getCount());
                        count++;
                    }
                }
            }
        }

        logger.info("Core biGram pair size " + count);
        this.matrix = new CSRSparseMatrix(table, coreDictionary.size());
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        matrix.writeExternal(out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.matrix = CSRSparseMatrix.readExternal(in);
    }

    /**
     * 获取共现频次
     *
     * @param a 第一个词
     * @param b 第二个词
     * @return 第一个词@第二个词出现的频次
     */
    public int getBiFrequency(String a, String b) {
        int idA = coreDictionary.getWordID(a);
        if (idA < 0) {
            return 0;
        }
        int idB = coreDictionary.getWordID(b);
        if (idB < 0) {
            return 0;
        }
        return matrix.get(idA, idB);
    }

    /**
     * 获取共现频次
     *
     * @param idA 第一个词的id
     * @param idB 第二个词的id
     * @return 共现频次, 不存在就返回0
     */
    @Override
    public final int getBiFrequency(int idA, int idB) {
        return matrix.get(idA, idB);
    }

    public CoreDictPatch getCoreDictPatch() {
        return coreDictPatch;
    }
}
