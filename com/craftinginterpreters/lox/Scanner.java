package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

class Scanner {

    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and",     AND);
        keywords.put("class",   CLASS);
        keywords.put("else",    ELSE);
        keywords.put("false",   FALSE);
        keywords.put("for",     FOR);
        keywords.put("fun",     FUN);
        keywords.put("if",      IF);
        keywords.put("nil",     NIL);
        keywords.put("print",   PRINT);
        keywords.put("return",  RETURN);
        keywords.put("super",   SUPER);
        keywords.put("this",    THIS);
        keywords.put("true",    TRUE);
        keywords.put("var",     VAR);
        keywords.put("while",   WHILE);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int mcnest = 0;

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the begining of the next lexeme
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();

        switch(c) {
            // Ignore whitespace
            case ' ':
            case '\t':
            case '\r':
                break;
            case '\n':
              line++;
              break;

            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case '*': addToken(STAR); break;
            case ';': addToken(SEMICOLON); break;

            // Handle 1-2 character combination tokens
            case '!': addToken(match('=') ? BANG_EQUAL : BANG ); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS ); break;
            case '>': addToken(match('=') ? GREATER_EQUAL: GREATER ); break;
            // Single line comments need to handle more its own 2 chars
            case '/':
                // Handle single line comments
                if (match('/')) {
                    while(peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else if (match('*')) {
                    mcnest=1;
                    multilinecomment();
                } else {
                    addToken(SLASH);
                }
                break;

            // String literals
            case '"': string(); break;


            // Rather than case match for every potential starting number
            // we do number handling under the default case.
            // The same goes for identifiers and keywords
            default:
                // Numeric literals
                if (isDigit(c)) {
                    number();
                } else if(isAlpha(c)) {
                    identifier();
                } else {
                    // No scanner match must be a syntax error
                    Lox.error(line, "Unexpected character: " + c);
                    break;
                }
        }
    }

    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    private char peek() {
        if (isAtEnd()) { return '\0'; }
        return source.charAt(current);
    }

    private char peekNext() {
        // Don't peek past the end of source
        if (current + 1 >= source.length()) { return '\0'; }

        return source.charAt(current + 1);
    }

    private boolean match(char expected) {
        if (isAtEnd()) { return false; }
        if (source.charAt(current) != expected) { return false; }

        current++;
        return true;
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z' ||
                c >= 'A' && c <= 'Z' ||
                c == '_'
        );
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        String text = source.substring(start,current);
        TokenType type = keywords.get(text);
        // Not a reserved word
        if (type ==  null) {
            type = IDENTIFIER;
        }
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) {
            advance();
        }

        // Fractions are supported in numbers
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the .
            advance();
 
            while (isDigit(peek())) {
                advance();
            }
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
   }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') { line++; }
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // Skip the closing "
        advance();

        // Trim the surrounding quotes
        String value = source.substring(start+1, current -1);
        // This is where we would typically unescape things like \n
        addToken(STRING, value); 
    }

    private void multilinecomment() {
        while(mcnest> 0 && !isAtEnd()) {
        // Room to optimize in future...
            if (peek() == '/' && peekNext() == '*' && !isAtEnd()) { mcnest++; }
            if (peek() == '*' && peekNext() == '/') { mcnest--; }
            if (peek() == '\n') { line++; }
            advance();
        }

        // Did we exit due to hitting the end?
        if (isAtEnd()) {
            Lox.error(line, "Unterminated multiline comment.");
            return;
        }

        // Consume the asterisk and the slash
        advance();
        advance();
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start,current);
        tokens.add(new Token(type, text, literal, line));
    }

    private Boolean isAtEnd() {
        return current >= source.length();
    }
}