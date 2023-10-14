package de.yugata.easy.edits.editor;


import java.util.Map;

import static de.yugata.easy.edits.editor.filter.FilterManager.FILTER_MANAGER;

public class FlutterWrapper {


    public static Map<String, String> getFilterValueMap() {
        return FILTER_MANAGER.getFilterValueMap();
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
