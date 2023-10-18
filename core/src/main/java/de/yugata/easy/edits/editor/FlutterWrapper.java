package de.yugata.easy.edits.editor;


import de.yugata.easy.edits.editor.filter.FilterType;
import de.yugata.easy.edits.editor.filter.FilterValue;

import java.util.List;

import static de.yugata.easy.edits.editor.filter.FilterManager.FILTER_MANAGER;

public class FlutterWrapper {


    public static List<FlutterFilterWrapper> getFilters() {
        return FILTER_MANAGER.getAvailableFilters();
    }

    public static void exportSegments(final String json) {
        final VideoEditor videoEditor = new VideoEditorBuilder()
                .fromJson(json);

        videoEditor.writeSegments();
    }

    public static void edit(final String json) {
        final VideoEditor videoEditor = new VideoEditorBuilder()
                .fromJson(json);

        videoEditor.edit(false);
    }


    /**
     * Data class to pass to the frontend containing the data the frontend needs to display the filter,
     * as well as the needed data to convey the filter to the backend.
     */
    public static class FlutterFilterWrapper {

        private final String name;

        private final String displayName;

        private final String description;

        private final String helpText;

        private final FilterType filterType;

        private final List<FilterValue> values;


        public FlutterFilterWrapper(String name, String displayName, String description, String helpText, FilterType filterType, List<FilterValue> values) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.helpText = helpText;
            this.filterType = filterType;
            this.values = values;
        }


        public String getHelpText() {
            return helpText;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public FilterType getFilterType() {
            return filterType;
        }

        public List<FilterValue> getValues() {
            return values;
        }
    }

}
