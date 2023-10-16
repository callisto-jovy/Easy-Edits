package de.yugata.easy.edits.editor.filter;

public class FilterValue {

    //TODO: Create a proper value system

    private final String name, value;


    public FilterValue(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
