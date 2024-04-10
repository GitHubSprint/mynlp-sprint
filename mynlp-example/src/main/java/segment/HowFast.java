package segment;

import com.mayabot.nlp.common.Guava;
import com.mayabot.nlp.segment.Lexer;
import com.mayabot.nlp.segment.LexerReader;
import com.mayabot.nlp.segment.Lexers;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;


public class HowFast {
    public static void main(String[] args) throws Exception {
        File file = new File("data.work/data.txt");


        List<String> lines = Files.readAllLines(file.toPath()).stream().filter(it -> !it.isEmpty()).collect(Collectors.toList());

        String text = Guava.join(lines, "\n");

        Lexer lexer = Lexers.builder()
                .core()
                .build();

        LexerReader analyzer = lexer.reader();

//        MynlpTokenizer lexer = new SimpleDictTokenizerBuilder().build();

        final int charNum = lines.stream().mapToInt(String::length).sum();


        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            analyzer.scan(reader).forEach(x -> {
            });
        }

        lines.forEach(lexer::scan);

        System.currentTimeMillis();

        {
            long t1 = System.currentTimeMillis();

            lines.forEach(lexer::scan);

            long t2 = System.currentTimeMillis();

            double time = (t2 - t1);
            System.out.println("Mynlp użycie imiesłowu " + (int) time + " ms");

            System.out.println("prędkość " + (int) ((charNum / time) * 1000) + "słowa/sekundę");
        }

//        System.out.println("--------Ansj----");
//        lines.forEach(line -> {
//            ToAnalysis.scan(line);
//
//        });
//
//        {
//            long t1 = System.currentTimeMillis();
//
//            lines.forEach(line -> {
//                ToAnalysis.scan(line);
//
//            });
//            long t2 = System.currentTimeMillis();
//
//            double time = (t2 - t1);
//            System.out.println("Ans " + (int) time + " ms");
//
//            System.out.println("predkosc " + (int) ((charNum / time) * 1000) + "word/sec");
//        }
//
//        System.out.println("xxxx");

    }
}
