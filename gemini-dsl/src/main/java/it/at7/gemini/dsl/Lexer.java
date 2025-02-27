package it.at7.gemini.dsl;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;

public class Lexer {
    private StreamTokenizer input;

    public enum TokenType {
        INTERFACE("INTERFACE"),
        ENTITY("ENTITY"),
        EMBEDABLE("EMBEDABLE"),
        IMPLEMENTS("IMPLEMENTS"),
        ONEREC("ONEREC"),
        L_BRACE("\\{"),
        R_BRACE("\\}"),
        ASTERISK("\\*[0-9]*"),
        COMMA("\\,"),
        WORD(""),
        EOF(""),
        EOL("");

        private String keyword;

        TokenType(String keyword) {
            this.keyword = keyword;
        }

        public static TokenType getTokenFromKeyword(int type, String keyword) {
            for (TokenType tokenType : values()) {
                if (!tokenType.keyword.isEmpty())
                    if (keyword != null && keyword.matches(tokenType.keyword)) {
                        return tokenType;
                    }
            }
            if (type == StreamTokenizer.TT_WORD) {
                return WORD;
            }
            return EOF;
        }
    }

    public Lexer(Reader r) {
        input = new StreamTokenizer(r);
        input.resetSyntax();
        input.wordChars('!', '~');
        input.ordinaryChar('/');
        input.whitespaceChars('\u0000', ' ');
        input.slashSlashComments(true);
        input.slashStarComments(true);
        input.eolIsSignificant(false);
        input.commentChar('#');
    }

    public TokenType nextToken() {
        try {
            return TokenType.getTokenFromKeyword(input.nextToken(), input.sval);
        } catch (IOException e) {
            e.printStackTrace();
            return TokenType.EOF;
        }
    }

    public String getVal() throws SyntaxError {
        if (input.sval == null) throw new SyntaxError("Syntax Error");
        return input.sval;
    }

}
