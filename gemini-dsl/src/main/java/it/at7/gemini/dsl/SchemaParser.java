package it.at7.gemini.dsl;

import it.at7.gemini.dsl.Lexer.TokenType;
import it.at7.gemini.dsl.entities.RawEntity;
import it.at7.gemini.dsl.entities.RawEntityBuilder;
import it.at7.gemini.dsl.entities.RawSchema;
import it.at7.gemini.dsl.entities.RawSchemaBuilder;

import java.io.Reader;

import static it.at7.gemini.dsl.Lexer.TokenType.*;

public class SchemaParser {
    private Lexer lexer;
    private TokenType currentToken;

    private SchemaParser(Reader r) {
        lexer = new Lexer(r);
    }

    private RawSchema parse() throws SyntaxError {
        RawSchemaBuilder rawSchemaBuilder = new RawSchemaBuilder();
        nextToken(); // first token
        while (currentToken != TokenType.EOF) {
            switch (currentToken) {
                case INTERFACE:
                    rawSchemaBuilder.addInterface(parseEntity());
                    break;
                case ENTITY:
                    rawSchemaBuilder.addEntity(parseEntity());
                    break;
            }
        }
        return rawSchemaBuilder.build();
    }

    private RawEntity parseEntity() throws SyntaxError {
        RawEntityBuilder builder = new RawEntityBuilder();
        nextToken();
        parseEntityModifiers(builder);
        expect(TokenType.WORD);
        builder.addName(lexer.getVal());
        nextToken();
        if (currentToken.equals(TokenType.IMPLEMENTS)) {
            do {
                nextToken();
                expect(TokenType.WORD);
                String implementsName = lexer.getVal();
                builder.addImplementsInterface(implementsName);
                nextToken();
            } while (currentToken.equals(TokenType.COMMA));
            if (currentToken != L_BRACE) {
                return builder.build();
            }
        }
        expect(TokenType.L_BRACE);
        nextToken();
        while (currentToken != TokenType.R_BRACE) {
            parseModelEntry(builder);
        }
        nextToken();
        return builder.build();
    }

    private void parseEntityModifiers(RawEntityBuilder builder) {
        boolean foundAny;
        do {
            foundAny = false;
            if (currentToken.equals(EMBEDABLE)) {
                builder.isEmbedable();
                nextToken();
                foundAny = true;
            }
            if (currentToken.equals(ONEREC)) {
                builder.isOneRec();
                nextToken();
                foundAny = true;
            }
        } while (foundAny);
    }

    private void parseModelEntry(RawEntityBuilder entityBuilder) throws SyntaxError {
        expect(TokenType.WORD);
        String type = lexer.getVal();
        nextToken();
        expect(TokenType.WORD);
        String name = lexer.getVal();
        RawEntityBuilder.EntryBuilder entryBuilder = new RawEntityBuilder.EntryBuilder(entityBuilder, type, name);
        parseEntryQualifier(entryBuilder);
        entityBuilder.addEntry(entryBuilder.build());
    }

    private void parseEntryQualifier(RawEntityBuilder.EntryBuilder entryBuilder) throws SyntaxError {
        nextToken();
        if (currentToken.equals(TokenType.ASTERISK)) {
            if (entryBuilder.getEntityBuilder().getIsOneRec()) {
                throw new SyntaxError(String.format("Entity %s has only one record - cannot have a logical key entry", entryBuilder.getEntityBuilder().getName()));
            }
            String lkToken = lexer.getVal();
            int lkOrder = 1;
            if (!lkToken.equals("*")) {
                String order = lkToken.substring(1);
                lkOrder = Integer.parseInt(order);
            }
            entryBuilder.isLogicalKey(lkOrder);
            parseEntryQualifier(entryBuilder);
        }
    }

    private void expect(TokenType tokenType) throws SyntaxError {
        if (currentToken != tokenType) {
            throw new SyntaxError(String.format("Expected %s while found %s", tokenType, currentToken));
        }
    }

    private TokenType nextToken() {
        currentToken = lexer.nextToken();
        return currentToken;
    }

    public static RawSchema parse(Reader r) throws SyntaxError {
        return new SchemaParser(r).parse();
    }
}
