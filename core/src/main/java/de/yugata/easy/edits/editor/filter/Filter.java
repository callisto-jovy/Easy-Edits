package de.yugata.easy.edits.editor.filter;

public class Filter {


    private final String name, filter;

    private final FilterType filterType;


    public Filter(final String name, final String filter, final FilterType filterType) {
        this.name = name;
        this.filter = filter;
        this.filterType = filterType;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public String getName() {
        return name;
    }


    public String getFilter() {
        return filter;
    }
}
