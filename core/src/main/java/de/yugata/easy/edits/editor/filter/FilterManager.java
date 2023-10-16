package de.yugata.easy.edits.editor.filter;


import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.yugata.easy.edits.editor.EditInfo;
import de.yugata.easy.edits.util.FilterParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FilterManager {


    private static final File FILTER_DIR = new File("filters");

    public static final FilterManager FILTER_MANAGER = new FilterManager();


    private final List<Filter> filters = new ArrayList<>();

    private final List<FilterWrapper> availableFilters = new ArrayList<>();


    public FilterManager() {
        this.populateAvailableFilters();
    }

    private void populateAvailableFilters() {
        if (!checkFilterAvailability())
            return;

        final File[] files = FILTER_DIR.listFiles();

        for (final File file : files) {
            try {
                final JsonElement root = JsonParser.parseReader(new FileReader(file));
                final FilterWrapper wrapper = FilterParser.getFilterWrapper(root);

                availableFilters.add(wrapper);

            } catch (IOException e) {
                System.err.println("Could not read filter. IO-Exception thrown.");
            } catch (Exception e) {
                System.err.println("Error while parsing filter: " + e.getMessage());
            }
        }

    }


    public void populateFilters(final List<FilterWrapper> filterWrappers, final EditInfo editInfo) {
        if (!checkFilterAvailability())
            return;

        final File[] files = FILTER_DIR.listFiles();

        final FilterParser filterParser = new FilterParser(editInfo);

        for (final FilterWrapper filterWrapper : filterWrappers) {
            // get the filter that matches the name (id)

            for (final File file : files) {
                try {
                    final JsonElement root = JsonParser.parseReader(new FileReader(file));

                    if (filterParser.doesIdMatch(filterWrapper.getName(), root)) {
                        final Filter filter = filterParser.parseFilter(filterWrapper, root);
                        filters.add(filter);
                    }


                } catch (IOException e) {
                    System.err.println("Could not read filter. IO-Exception thrown.");
                } catch (Exception e) {
                    System.err.println("Error while parsing filter: " + e.getMessage());
                }
            }
        }
    }

    private boolean checkFilterAvailability() {
        // load the filters from the resources

        if (!FILTER_DIR.exists()) {
            System.err.println("No filter directory exists at the required position. No filters were loaded.");
            return false;
        }

        final File[] files = FILTER_DIR.listFiles();
        if (files == null) {
            System.err.println("No files found in the filter directory. No filters were loaded.");
            return false;
        }
        return true;
    }

    public List<Filter> getTransitions() {
        return this.filters.stream().filter(filter -> filter.getFilterType() == FilterType.TRANSITION).collect(Collectors.toList());
    }


    public List<Filter> getVideoFilters() {
        return this.filters.stream().filter(filter -> filter.getFilterType() == FilterType.VIDEO).collect(Collectors.toList());

    }

    public List<Filter> getAudioFilters() {
        return this.filters.stream().filter(filter -> filter.getFilterType() == FilterType.AUDIO).collect(Collectors.toList());
    }

    public List<FilterWrapper> getAvailableFilters() {
        return availableFilters;
    }
}
