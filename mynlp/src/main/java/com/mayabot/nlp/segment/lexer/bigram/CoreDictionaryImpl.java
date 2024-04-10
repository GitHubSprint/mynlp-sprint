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
import com.mayabot.nlp.algorithm.collection.dat.DoubleArrayTrieStringIntMap;
import com.mayabot.nlp.common.EncryptionUtil;
import com.mayabot.nlp.common.injector.Singleton;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.TreeMap;

/**
 * 内置的核心词典，词数大约20+万
 * key 存储 word
 * value 存储 词频
 * 词典的文件格式：
 * word1 freq
 * word2 freq
 * 缓存为DAT格式
 *
 * @author jimichan
 */
@Singleton
public class CoreDictionaryImpl extends BaseExternalizable implements CoreDictionary {

    private final MynlpEnv env;

    private Logger logger = LoggerFactory.getLogger(CoreDictionaryImpl.class);

    public static final String path = "core-dict/CoreDict.txt";

    /**
     * 词频总和
     */
    private int totalFreq;

    private DoubleArrayTrieStringIntMap trie;

    private @Nullable
    CoreDictPatch coreDictPatch;

    private boolean loadFromBinCache = true;

    public CoreDictionaryImpl(MynlpEnv env, CoreDictPathWrap coreDictPathWrap) throws Exception {
        super(env);
        this.env = env;
        this.coreDictPatch = coreDictPathWrap.getCoreDictPatch();
        // coreDictPathWrap 注定是个可配置且变化的数据，那么就不要指望cache加速了
        if (this.coreDictPatch != null) {
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
    public int totalFreq() {
        return totalFreq;
    }

    @Override
    @SuppressWarnings(value = "rawtypes")
    public void loadFromSource() throws Exception {

        //词和词频
        TreeMap<String, Integer> map = new TreeMap<>();

        CoreDictionaryReader reader = new CoreDictionaryReader(env);

        reader.read(new Function2<String, Integer, Unit>() {
            @Override
            public Unit invoke(String word, Integer count) {
                map.put(word, count);
                return Unit.INSTANCE;
            }
        });

        int maxFreq = reader.getTotalFreq();

        // apply dict patch
        if (coreDictPatch != null) {
            List<Pair<String, Integer>> list = coreDictPatch.appendDict();

            if (list != null) {
                for (Pair<String, Integer> pair : list) {
                    map.put(pair.getFirst(), pair.getSecond());
                }
            }

            List<String> deleted = coreDictPatch.deleteDict();
            if (deleted != null) {
                for (String word : deleted) {
                    map.remove(word);
                }
            }
        }

        this.totalFreq = maxFreq;

        //补齐，确保ID顺序正确
        for (String label : DictionaryAbsWords.allLabel()) {
            if (!map.containsKey(label)) {
                map.put(label, 1);
            }
        }

        if (map.isEmpty()) {
            throw new RuntimeException("not found core dict file ");
        }

        this.trie = new DoubleArrayTrieStringIntMap(map);
    }

    @Override
    public String sourceVersion() {
        String version = env.hashResource(path);
        if (version == null) {
            version = "";
        }
//        Hasher hasher = Hashing.murmur3_32().newHasher().
//                putString(version, Charsets.UTF_8).
//                putString("v2", Charsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        sb.append(version).append("v2");

//        if (coreDictPatch != null) {
//            sb.append(coreDictPatch.dictVersion());
//        }

        return EncryptionUtil.md5(sb.toString());
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(totalFreq);
        trie.save(out);
        out.flush();
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        this.totalFreq = in.readInt();
        this.trie = new DoubleArrayTrieStringIntMap(in);
    }

    /**
     * 获取条目
     *
     * @param key
     * @return 词频
     */
    public int wordFreq(String key) {
        int c = trie.get(key);
        if (c == -1) {
            c = 0;
        }
        return c;
    }

    /**
     * 获取条目
     *
     * @param wordID
     * @return 词频
     */
    @Override
    public int wordFreq(int wordID) {
        return trie.get(wordID);
    }


    @Override
    public int wordId(CharSequence key) {
        return trie.indexOf(key);
    }

    public int wordId(CharSequence key, int pos, int len, int nodePos) {
        return trie.indexOf(key, pos, len, nodePos);
    }

    @Override
    public int wordId(char[] chars, int pos, int len) {
        return trie.indexOf(chars, pos, len);
    }

    public int wordId(char[] keyChars, int pos, int len, int nodePos) {
        return trie.indexOf(keyChars, pos, len, nodePos);
    }


    /**
     * 是否包含词语
     *
     * @param key
     * @return 是否包含
     */
    @Override
    public boolean contains(String key) {
        return trie.indexOf(key) >= 0;
    }

    /**
     * 获取词语的ID
     *
     * @param word
     * @return 下标Id
     */
    @Override
    public int getWordID(String word) {
        return trie.indexOf(word);
    }

    @Override
    public DoubleArrayTrieStringIntMap.DATMapMatcherInt match(char[] text, int offset) {
        return trie.match(text, offset);
    }

    @Override
    public int size() {
        return trie.size();
    }

    @Nullable
    public CoreDictPatch getCoreDictPatch() {
        return coreDictPatch;
    }

    public void setCoreDictPatch(@Nullable CoreDictPatch coreDictPatch) {
        this.coreDictPatch = coreDictPatch;
    }
}
