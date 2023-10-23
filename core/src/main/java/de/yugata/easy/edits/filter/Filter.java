package de.yugata.easy.edits.filter;

public class Filter {

    private final String name, filter;
    private final FilterType filterType;
    private final FilterRange filterRange;

    public Filter(final String name, final String filter, final FilterType filterType, FilterRange filterRange) {
        this.name = name;
        this.filter = filter;
        this.filterType = filterType;
        this.filterRange = filterRange;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public String getName() {
        return name;
    }

    public FilterRange getFilterRange() {
        return filterRange;
    }

    public String getFilter() {
        return filter;
    }
}
