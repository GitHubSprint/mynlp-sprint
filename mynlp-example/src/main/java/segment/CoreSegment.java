package segment;

import com.mayabot.nlp.segment.*;

import java.io.Reader;
import java.io.StringReader;

public class CoreSegment {

    public static void main(String[] args) {
        long t1 = System.currentTimeMillis();

        Lexer tokenizer = Lexers.core();


        Sentence sentence = tokenizer.scan("mynlp to chiński zestaw narzędzi NLP typu open source firmy Mayabot.");

        System.out.println(sentence.toWordList());


        LexerReader analyzer = tokenizer.reader();

        Reader reader = new StringReader("Udawaj, że to jeden duży tekst");
        WordTermSequence result = analyzer.scan(reader);
        long t2 = System.currentTimeMillis();
        System.out.println(t2 - t1);
        System.out.printf("result" + result.toSentence());
    }
}
