package io.fand.api.persistence;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;

/**
 * Immutable plugin persistent data stored under namespaced keys.
 */
public record PersistentDataContainer(JsonObject values) {

    public static final PersistentDataContainer EMPTY = new PersistentDataContainer(new JsonObject());

    public PersistentDataContainer {
        values = values == null ? new JsonObject() : values.deepCopy();
    }

    public boolean empty() {
        return values.size() == 0;
    }

    public Set<Key> keys() {
        var keys = new LinkedHashSet<Key>();
        for (var key : values.keySet()) {
            keys.add(Key.key(key));
        }
        return Set.copyOf(keys);
    }

    public boolean has(Key key) {
        return values.has(requireKey(key));
    }

    public Optional<JsonElement> get(Key key) {
        var value = values.get(requireKey(key));
        return value == null ? Optional.empty() : Optional.of(value.deepCopy());
    }

    public Optional<String> getString(Key key) {
        return primitive(key).filter(JsonPrimitive::isString).map(JsonPrimitive::getAsString);
    }

    public Optional<Integer> getInt(Key key) {
        return primitive(key).filter(JsonPrimitive::isNumber).map(JsonPrimitive::getAsInt);
    }

    public Optional<Long> getLong(Key key) {
        return primitive(key).filter(JsonPrimitive::isNumber).map(JsonPrimitive::getAsLong);
    }

    public Optional<Double> getDouble(Key key) {
        return primitive(key).filter(JsonPrimitive::isNumber).map(JsonPrimitive::getAsDouble);
    }

    public Optional<Boolean> getBoolean(Key key) {
        return primitive(key).filter(JsonPrimitive::isBoolean).map(JsonPrimitive::getAsBoolean);
    }

    public PersistentDataContainer with(Key key, JsonElement value) {
        Objects.requireNonNull(value, "value");
        var copy = values.deepCopy();
        copy.add(requireKey(key), value.deepCopy());
        return new PersistentDataContainer(copy);
    }

    public PersistentDataContainer withString(Key key, String value) {
        return with(key, new JsonPrimitive(Objects.requireNonNull(value, "value")));
    }

    public PersistentDataContainer withInt(Key key, int value) {
        return with(key, new JsonPrimitive(value));
    }

    public PersistentDataContainer withLong(Key key, long value) {
        return with(key, new JsonPrimitive(value));
    }

    public PersistentDataContainer withDouble(Key key, double value) {
        return with(key, new JsonPrimitive(value));
    }

    public PersistentDataContainer withBoolean(Key key, boolean value) {
        return with(key, new JsonPrimitive(value));
    }

    public PersistentDataContainer without(Key key) {
        var copy = values.deepCopy();
        copy.remove(requireKey(key));
        return new PersistentDataContainer(copy);
    }

    public JsonObject toJson() {
        return values.deepCopy();
    }

    private Optional<JsonPrimitive> primitive(Key key) {
        return get(key)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsJsonPrimitive);
    }

    private static String requireKey(Key key) {
        return Objects.requireNonNull(key, "key").asString();
    }
}
