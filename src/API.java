import org.languagetool.JLanguageTool;
import org.languagetool.language.PortugalPortuguese;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.List;

public class API {

    public static void main( String[] args ) throws IOException {
        JLanguageTool langTool = new JLanguageTool(new PortugalPortuguese());
        List<RuleMatch> matches = langTool.check("Umaa frrase que não é gramática");
        for (RuleMatch match : matches) {
            System.out.println("Potential error at characters " +
                    match.getFromPos() + "-" + match.getToPos() + ": " +
                    match.getMessage());
            System.out.println("Suggested correction(s): " +
                    match.getSuggestedReplacements());
        }
    }
}
