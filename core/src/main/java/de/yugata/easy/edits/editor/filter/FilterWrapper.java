package de.yugata.easy.edits.editor.filter;

import java.util.List;

public class FilterWrapper {

    private final String name;

    private final String displayName;

    private final String description;

    private final List<FilterValue> values;

    public FilterWrapper(String name, String displayName, String description, List<FilterValue> values) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.values = values;
    }

    public String getDisplayName() {
        return displayName;
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
