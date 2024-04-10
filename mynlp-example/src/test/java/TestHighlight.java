import com.mayabot.nlp.module.QuickReplacer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TestHighlight {

    public static void main(String[] args) {
        List<String> keywords = new ArrayList<>();

        keywords.add("zezwolenie na pobyt");
        keywords.add("na żywo");

        QuickReplacer quickReplacer = new QuickReplacer(keywords);

        String result = quickReplacer.replaceForJava("Mieszkanie w Szanghaju wymaga pozwolenia na pobyt",
                (Function<String, String>) word -> "<a href='xxx'>" + word + "</a>");

        System.out.println(result);

    }
}
