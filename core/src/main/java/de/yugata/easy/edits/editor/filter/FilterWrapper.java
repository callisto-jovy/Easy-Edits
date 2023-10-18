package de.yugata.easy.edits.editor.filter;

import java.util.List;

public class FilterWrapper {

    private final String name;


    private final List<FilterValue> values;

    public FilterWrapper(String name, List<FilterValue> values) {
        this.name = name;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public List<FilterValue> getValues() {
        return values;
    }
}
