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

package com.mayabot.nlp.segment.pipeline;

import com.mayabot.nlp.Mynlp;
import com.mayabot.nlp.common.Lists;
import com.mayabot.nlp.segment.*;
import com.mayabot.nlp.segment.common.DefaultCharNormalize;
import com.mayabot.nlp.segment.lexer.bigram.ViterbiBestPathAlgorithm;
import com.mayabot.nlp.segment.plugins.collector.SentenceCollector;
import com.mayabot.nlp.segment.plugins.collector.WordTermCollector;
import com.mayabot.nlp.segment.wordnet.BestPathAlgorithm;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pipeline based Lexer Builder
 *
 * @author jimichan
 */
public class PipelineLexerBuilder implements LexerBuilder {

    @NotNull
    protected final Mynlp mynlp;

    /**
     * 保持输出char不被规则化
     */
    private boolean keepOriCharOutput = false;

    /**
     * 词图最优路径选择器
     */
    @NotNull
    private BestPathAlgorithm bestPathAlgorithm;

    /**
     * 字符处理器,默认就有最小化和全角半角化
     */
    private List<CharNormalize> charNormalizes = Lists.newArrayList(
            DefaultCharNormalize.instance
    );

    /**
     * 切词器管线
     */
    private LinkedList<WordSplitAlgorithm> wordSplitAlgorithmList = new LinkedList<>();

    /**
     * 逻辑Pipeline
     */
    private LinkedList<WordpathProcessor> pipeLine = new LinkedList<>();

    /**
     * 保存后置监听器逻辑
     */
    private List<ConsumerPair> configListener = Lists.newArrayList();

    /**
     * 最终结构收集器
     */
    private WordTermCollector termCollector;

    @NotNull
    public static PipelineLexerBuilder builder() {
        return new PipelineLexerBuilder();
    }

    @NotNull
    public static PipelineLexerBuilder builder(Mynlp mynlp) {
        return new PipelineLexerBuilder(mynlp);
    }

    public PipelineLexerBuilder() {
        this(Mynlp.instance());
    }

    public PipelineLexerBuilder(Mynlp mynlp) {
        this.mynlp = mynlp;
        this.bestPathAlgorithm = mynlp.getInstance(ViterbiBestPathAlgorithm.class);
        this.termCollector = new SentenceCollector(mynlp,
                Collections.emptyList(), Collections.emptyList());
    }

    public void install(PipelineLexerPlugin plugin) {
        plugin.init(this);
    }

    @Override
    public Lexer build() {

        // 默认core的分词算法
        if (wordSplitAlgorithmList.isEmpty()) {
            throw new IllegalStateException("Miss wordSplitAlgorithm");
        }

        callListener();

        final ArrayList<WordSplitAlgorithm> splitAlgorithms = Lists.newArrayList(wordSplitAlgorithmList);
        final ArrayList<WordpathProcessor> wordpathProcessors = Lists.newArrayList(pipeLine);

        Collections.sort(splitAlgorithms);
        Collections.sort(wordpathProcessors);

        return new PipelineLexer(
                Lists.newArrayList(splitAlgorithms),
                wordpathProcessors.toArray(new WordpathProcessor[0]),
                bestPathAlgorithm,
                termCollector,
                Lists.newArrayList(this.charNormalizes),
                keepOriCharOutput);
    }

    private boolean instanceOf(Object subObj, Class<?> parent) {
        Class sub = subObj.getClass();
        return parent.equals(sub) ||
                parent.isAssignableFrom(sub);
    }

    /**
     * 调用后置监听器
     */
    @SuppressWarnings(value = {"unchecked"})
    private void callListener() {
        for (ConsumerPair pair : configListener) {
            //WordTermCollector
            if (instanceOf(termCollector, pair.clazz)) {
                pair.consumer.accept(termCollector);
            }

            //wordSplitAlgorithmList
            wordSplitAlgorithmList.forEach(it -> {
                if (instanceOf(it, pair.clazz)) {
                    pair.consumer.accept(it);
                }
            });

            //Pipeline WordProcessor
            pipeLine.forEach(it -> {
                if (instanceOf(it, pair.clazz)) {
                    pair.consumer.accept(it);
                }
            });

        }

    }

    /**
     * 设定针对WordpathProcessor，WordSplitAlgorithm，WordTermCollector等组件后置逻辑。
     * 通过这个方法可以已经创建的组件进行配置
     *
     * @param clazz
     * @param listener
     * @param <T>
     * @return PipelineLexerBuilder
     */
    public <T> PipelineLexerBuilder config(Class<T> clazz, Consumer<T> listener) {
        configListener.add(new ConsumerPair(clazz, listener));
        return this;
    }

    /**
     * 关闭组件
     *
     * @param clazz
     * @return PipelineLexerBuilder
     */
    public PipelineLexerBuilder disabledComponent(Class<? extends SegmentComponent> clazz) {
        config(clazz, SegmentComponent::disable);
        return this;
    }

    /**
     * 启用组件
     *
     * @param clazz
     * @return PipelineLexerBuilder
     */
    public PipelineLexerBuilder enableComponent(Class<? extends SegmentComponent> clazz) {
        config(clazz, SegmentComponent::enable);
        return this;
    }


    /**
     * 添加CharNormalize
     *
     * @param charNormalizeClass 通过Guice来初始化该对象
     * @return PipelineLexerBuilder self
     */
    public PipelineLexerBuilder addCharNormalize(Class<? extends CharNormalize> charNormalizeClass) {
        addCharNormalize(mynlp.getInstance(charNormalizeClass));
        return this;
    }

    /**
     * 添加CharNormalize
     *
     * @param charNormalize
     * @return PipelineLexerBuilder self
     */
    public PipelineLexerBuilder addCharNormalize(CharNormalize charNormalize) {

        if (this.charNormalizes.contains(charNormalize)) {
            return this;
        }

        this.charNormalizes.add(charNormalize);

        return this;
    }

    /**
     * 移除CharNormalize
     *
     * @param clazz
     * @return PipelineLexerBuilder
     */
    public PipelineLexerBuilder removeCharNormalize(Class<? extends CharNormalize> clazz) {
        this.charNormalizes.removeIf(obj -> clazz.isAssignableFrom(obj.getClass()) || obj.getClass().equals(clazz));
        return this;
    }

    /**
     * 设置BestPathComputer的实现对象
     *
     * @param bestPathAlgorithm BestPathAlgorithm
     * @return PipelineLexerBuilder
     */
    public PipelineLexerBuilder setBestPathAlgorithm(BestPathAlgorithm bestPathAlgorithm) {
        if (bestPathAlgorithm == null) {
            throw new NullPointerException();
        }
        this.bestPathAlgorithm = bestPathAlgorithm;
        return this;
    }

    /**
     * 设置BestPathComputer的实现类，有Guice创建对象
     *
     * @param clazz BestPathAlgorithm
     * @return PipelineLexerBuilder
     */
    public PipelineLexerBuilder setBestPathComputer(Class<? extends BestPathAlgorithm> clazz) {
        setBestPathAlgorithm(mynlp.getInstance(clazz));
        return this;
    }

    /**
     * 增加一个WordpathProcessor实现对象
     *
     * @param processor
     * @return PipelineLexerBuilder
     */
    public PipelineLexerBuilder addProcessor(WordpathProcessor processor) {
        if (pipeLine.contains(processor)) {
            return this;
        }

        pipeLine.add(processor);
        Collections.sort(pipeLine);
        return this;
    }


    /**
     * 增加一个WordpathProcessor实现类
     *
     * @param clazz
     * @return PipelineLexerBuilder
     */
    public PipelineLexerBuilder addProcessor(Class<? extends WordpathProcessor> clazz) {
        addProcessor(mynlp.getInstance(clazz));
        return this;
    }

    /**
     * 是没有指定class的实例存在
     *
     * @return PipelineLexerBuilder
     */
    public boolean existWordPathProcessor(Class clazz) {
        return pipeLine.stream().anyMatch(x -> instanceOf(x.getClass(), clazz));
    }


    /**
     * 增加WordnetInitializer对象
     *
     * @param algorithm
     * @return PipelineLexerBuilder
     */
    public PipelineLexerBuilder addWordSplitAlgorithm(WordSplitAlgorithm algorithm) {
        if (wordSplitAlgorithmList.contains(algorithm)) {
            return this;
        }

        this.wordSplitAlgorithmList.add(algorithm);

        Collections.sort(wordSplitAlgorithmList);
        return this;
    }

    /**
     * 增加WordnetInitializer
     *
     * @param algorithm Class
     * @return PipelineLexerBuilder
     */
    public PipelineLexerBuilder addWordSplitAlgorithm(Class<? extends WordSplitAlgorithm> algorithm) {
        addWordSplitAlgorithm(mynlp.getInstance(algorithm));
        return this;
    }


    /**
     * 设置分词结果收集器
     *
     * @param termCollector
     * @return PipelineLexerBuilder
     */
    public PipelineLexerBuilder setTermCollector(
            @NotNull
                    WordTermCollector termCollector) {
        if (termCollector == null) {
            throw new NullPointerException("WordTermCollector is null");
        }
        this.termCollector = termCollector;
        return this;
    }

    /**
     * 设置分词结果收集器
     *
     * @param termCollectorClass
     * @return PipelineLexerBuilder PipelineLexerBuilder
     */
    public PipelineLexerBuilder setTermCollector(Class<? extends WordTermCollector> termCollectorClass) {
        this.termCollector = mynlp.getInstance(termCollectorClass);
        return this;
    }

    @NotNull
    public WordTermCollector getTermCollector() {
        return termCollector;
    }

    private static class ConsumerPair {
        Class clazz;
        Consumer consumer;

        public ConsumerPair(Class clazz, Consumer consumer) {
            this.clazz = clazz;
            this.consumer = consumer;
        }
    }

    public boolean isKeepOriCharOutput() {
        return keepOriCharOutput;
    }

    public PipelineLexerBuilder setKeepOriCharOutput(boolean keepOriCharOutput) {
        this.keepOriCharOutput = keepOriCharOutput;
        return this;
    }

    @NotNull
    public Mynlp getMynlp() {
        return mynlp;
    }
}
