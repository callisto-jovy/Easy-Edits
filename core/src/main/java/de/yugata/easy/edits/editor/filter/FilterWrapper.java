package de.yugata.easy.edits.editor.filter;

public class FilterWrapper {

    private final String name;
    private final String value;


    public FilterWrapper(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FilterWrapper && ((FilterWrapper) obj).getName().equals(name);
    }

    public boolean nameEquals(final String name) {
        return this.name.equals(name);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
