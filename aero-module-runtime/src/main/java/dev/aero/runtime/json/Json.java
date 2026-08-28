package dev.aero.runtime.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal recursive-descent JSON reader, just enough to parse a module
 * manifest without pulling in an external JSON library (which could clash
 * with whatever version Minecraft/Fabric happen to bundle at runtime - this
 * module has zero runtime dependencies by design, see the module README).
 *
 * <p>Parses into plain {@link Map}/{@link List}/{@link String}/{@link Double}/
 * {@link Boolean}/{@code null} - not a general-purpose JSON library, just
 * enough of the spec for a small config-shaped document.
 */
public final class Json {

    private final String src;
    private int pos;

    private Json(String src) {
        this.src = src;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Json parser = new Json(json);
        parser.skipWhitespace();
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.pos != json.length()) {
            throw new JsonParseException("Unexpected trailing content at offset " + parser.pos);
        }
        if (!(value instanceof Map)) {
            throw new JsonParseException("Expected a JSON object at the top level");
        }
        return (Map<String, Object>) value;
    }

    private Object parseValue() {
        skipWhitespace();
        if (pos >= src.length()) {
            throw new JsonParseException("Unexpected end of input");
        }
        char c = src.charAt(pos);
        return switch (c) {
            case '{' -> parseObjectValue();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't', 'f' -> parseBoolean();
            case 'n' -> parseNull();
            default -> parseNumber();
        };
    }

    private Map<String, Object> parseObjectValue() {
        expect('{');
        Map<String, Object> result = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            Object value = parseValue();
            result.put(key, value);
            skipWhitespace();
            char next = peek();
            if (next == ',') {
                pos++;
            } else if (next == '}') {
                pos++;
                break;
            } else {
                throw new JsonParseException("Expected ',' or '}' at offset " + pos);
            }
        }
        return result;
    }

    private List<Object> parseArray() {
        expect('[');
        List<Object> result = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return result;
        }
        while (true) {
            result.add(parseValue());
            skipWhitespace();
            char next = peek();
            if (next == ',') {
                pos++;
            } else if (next == ']') {
                pos++;
                break;
            } else {
                throw new JsonParseException("Expected ',' or ']' at offset " + pos);
            }
        }
        return result;
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= src.length()) {
                throw new JsonParseException("Unterminated string");
            }
            char c = src.charAt(pos++);
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                if (pos >= src.length()) {
                    throw new JsonParseException("Unterminated escape sequence");
                }
                char esc = src.charAt(pos++);
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > src.length()) {
                            throw new JsonParseException("Invalid unicode escape");
                        }
                        String hex = src.substring(pos, pos + 4);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                    }
                    default -> throw new JsonParseException("Invalid escape '\\" + esc + "'");
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Boolean parseBoolean() {
        if (src.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (src.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw new JsonParseException("Invalid literal at offset " + pos);
    }

    private Object parseNull() {
        if (src.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw new JsonParseException("Invalid literal at offset " + pos);
    }

    private Double parseNumber() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || "+-.eE".indexOf(src.charAt(pos)) >= 0)) {
            pos++;
        }
        String number = src.substring(start, pos);
        if (number.isEmpty()) {
            throw new JsonParseException("Expected a value at offset " + start);
        }
        try {
            return Double.parseDouble(number);
        } catch (NumberFormatException e) {
            throw new JsonParseException("Invalid number '" + number + "' at offset " + start);
        }
    }

    private char peek() {
        if (pos >= src.length()) {
            throw new JsonParseException("Unexpected end of input");
        }
        return src.charAt(pos);
    }

    private void expect(char c) {
        if (pos >= src.length() || src.charAt(pos) != c) {
            throw new JsonParseException("Expected '" + c + "' at offset " + pos);
        }
        pos++;
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }
}
