package de.yugata.easy.edits.filter;

import com.google.gson.JsonObject;
import de.yugata.easy.edits.filter.FilterValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for the filter class for easy transmission between front- and backend.
 */
public class FilterWrapper {

    /**
     * The name of the filter that should be wrapped.
     */
    private final String name;
    /**
     * List of filter values that will be used when loading the actual filter.
     */
    private final List<FilterValue> values;

    public FilterWrapper(String name, List<FilterValue> values) {
        this.name = name;
        this.values = values;
    }

    /**
     * Constructor: reconstruct from json
     *
     * @param jsonObject the json object configured in a way that is acceptable.
     */
    public FilterWrapper(final JsonObject jsonObject) {
        this.name = jsonObject.get("name").getAsString();
        this.values = new ArrayList<>();
        // Add the values.
        final JsonObject valueObject = jsonObject.get("values").getAsJsonObject();
        valueObject.asMap().forEach((name, value) -> values.add(new FilterValue(name, value.getAsString())));
    }


    public String getName() {
        return name;
    }

    public List<FilterValue> getValues() {
        return values;
    }
}
