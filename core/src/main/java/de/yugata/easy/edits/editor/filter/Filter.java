package de.yugata.easy.edits.editor.filter;

public class Filter {


    private final String name, description, filter;

    private final FilterType filterType;


    public Filter(final String name, final String description, final String filter, final FilterType filterType) {
        this.name = name;
        this.description = description;
        this.filter = filter;
        this.filterType = filterType;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getFilter() {
        return filter;
    }
}
