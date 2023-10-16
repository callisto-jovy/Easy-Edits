package de.yugata.easy.edits.editor.filter;

import java.util.List;

public class FilterWrapper {

    private final String name;

    private final String description;

    private final List<FilterValue> values;


    public FilterWrapper(String name, String description, List<FilterValue> values) {
        this.name = name;
        this.description = description;
        this.values = values;
    }

    public String getName() {
        return name;
    }


    public String getDescription() {
        return description;
    }

    public List<FilterValue> getValues() {
        return values;
    }
}
