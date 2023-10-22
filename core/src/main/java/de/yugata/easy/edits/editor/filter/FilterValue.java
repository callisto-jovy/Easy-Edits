package de.yugata.easy.edits.editor.filter;

import com.google.gson.JsonObject;

public class FilterValue {

    //TODO: Create a proper value system

    private final String name, value;

    public FilterValue(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public FilterValue(final JsonObject jsonObject) {
        this.name = jsonObject.get("name").getAsString();
        this.value = jsonObject.get("value").getAsString();
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
