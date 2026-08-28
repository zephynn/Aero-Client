package dev.aero.runtime.json;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonTest {

    @Test
    void parsesNestedStructures() {
        Map<String, Object> obj = Json.parseObject("""
                {"a": "hi \\"there\\"", "b": 42, "c": true, "d": null, "e": [1, "two", false], "f": {"g": "h"}}
                """);

        assertEquals("hi \"there\"", obj.get("a"));
        assertEquals(42.0, obj.get("b"));
        assertEquals(true, obj.get("c"));
        assertEquals(null, obj.get("d"));
        assertEquals(List.of(1.0, "two", false), obj.get("e"));
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) obj.get("f");
        assertEquals("h", nested.get("g"));
    }

    @Test
    void rejectsTrailingGarbage() {
        assertThrows(JsonParseException.class, () -> Json.parseObject("{} garbage"));
    }

    @Test
    void rejectsNonObjectTopLevel() {
        assertThrows(JsonParseException.class, () -> Json.parseObject("[1,2,3]"));
    }
}
