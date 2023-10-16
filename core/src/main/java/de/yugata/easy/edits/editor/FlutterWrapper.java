package de.yugata.easy.edits.editor;


import de.yugata.easy.edits.editor.filter.FilterWrapper;

import java.util.List;
import java.util.Map;

import static de.yugata.easy.edits.editor.filter.FilterManager.FILTER_MANAGER;

public class FlutterWrapper {


    public static List<FilterWrapper> getFilters() {
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


}
