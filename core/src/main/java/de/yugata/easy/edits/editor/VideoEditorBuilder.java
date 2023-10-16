package de.yugata.easy.edits.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.yugata.easy.edits.editor.filter.FilterValue;
import de.yugata.easy.edits.editor.filter.FilterWrapper;

import java.io.File;
import java.util.*;

public class VideoEditorBuilder {

    private List<FilterWrapper> filters = new ArrayList<>();
    private String videoPath;
    private String audioPath;
    private File outputFile;
    private Queue<Double> timeBetweenBeats = new ArrayDeque<>();
    private List<Long> videoTimeStamps = new ArrayList<>();
    private EnumSet<EditingFlag> flags = EnumSet.noneOf(EditingFlag.class);

    private long introStart = -1, introEnd = -1;

    private File workingDirectory;


    public VideoEditorBuilder setVideoPath(String videoPath) {
        this.videoPath = videoPath;
        return this;
    }

    public VideoEditorBuilder setAudioPath(String audioPath) {
        this.audioPath = audioPath;
        return this;
    }

    public VideoEditorBuilder setOutputFile(File outputFile) {
        this.outputFile = outputFile;
        return this;
    }

    public VideoEditorBuilder setTimeBetweenBeats(Queue<Double> timeBetweenBeats) {
        this.timeBetweenBeats = timeBetweenBeats;
        return this;
    }

    public VideoEditorBuilder setVideoTimeStamps(List<Long> videoTimeStamps) {
        this.videoTimeStamps = videoTimeStamps;
        return this;
    }

    public VideoEditorBuilder setFlags(EnumSet<EditingFlag> flags) {
        this.flags = flags;
        return this;
    }

    public VideoEditorBuilder setFilters(List<FilterWrapper> filters) {
        this.filters = filters;
        return this;
    }

    public VideoEditorBuilder setIntroStart(long introStart) {
        this.introStart = introStart;
        return this;
    }

    public VideoEditorBuilder setIntroEnd(long introEnd) {
        this.introEnd = introEnd;
        return this;
    }

    public VideoEditorBuilder setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }


    //TODO: Maybe move to filter wrapper
    public VideoEditor fromJson(final String json) {
        final JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        final JsonObject editorState = root.getAsJsonObject("editor_state");
        final JsonArray filters = editorState.getAsJsonArray("filters");
        final JsonArray beatTimes = editorState.getAsJsonArray("beat_times");
        final JsonArray timeStamps = editorState.getAsJsonArray("time_stamps");
        final JsonObject editingFlags = editorState.getAsJsonObject("editing_flags");


        final List<FilterWrapper> mappedFilters = new ArrayList<>();
        filters.forEach(jsonElement -> {
            final JsonObject object = jsonElement.getAsJsonObject();
            final String name = object.get("name").getAsString();
            final JsonArray value = object.get("values").getAsJsonArray();

            final List<FilterValue> values = new ArrayList<>();
            // Map the json values to filter values
            //TODO: Clean up this mess..
            value.forEach(jsonElement1 -> values.add(new FilterValue(jsonElement1.getAsJsonObject().get("name").getAsString(), jsonElement1.getAsJsonObject().get("value").getAsString())));

            final FilterWrapper wrapper = new FilterWrapper(name, "", values);
            mappedFilters.add(wrapper);
        });

        final List<Long> mappedTimeStamps = new ArrayList<>();
        timeStamps.forEach(jsonElement -> mappedTimeStamps.add(jsonElement.getAsLong()));

        final List<Double> mappedBeatTimes = new ArrayList<>();
        beatTimes.forEach(jsonElement -> mappedBeatTimes.add(jsonElement.getAsDouble()));

        final EnumSet<EditingFlag> mappedEditingFlags = EnumSet.noneOf(EditingFlag.class);
        editingFlags.asMap().forEach((key, value) -> {
            if (value.getAsBoolean())
                mappedEditingFlags.add(EditingFlag.valueOf(key));
        });

        final String sourceVideo = root.get("source_video").getAsString();
        final String sourceAudio = root.get("source_audio").getAsString();
        final String outputPath = root.get("output_path").getAsString();
        final String workingPath = root.get("working_path").getAsString();

        final long introStart = editorState.get("intro_start").getAsLong(), introEnd = editorState.get("intro_end").getAsLong();

        return setVideoPath(sourceVideo)
                .setWorkingDirectory(new File(workingPath))
                .setOutputFile(new File(outputPath))
                .setAudioPath(sourceAudio)
                .setFilters(mappedFilters)
                .setFlags(mappedEditingFlags)
                .setTimeBetweenBeats(new ArrayDeque<>(mappedBeatTimes))
                .setIntroStart(introStart)
                .setIntroEnd(introEnd)
                .setVideoTimeStamps(mappedTimeStamps)
                .createVideoEditor();
    }


    public VideoEditor createVideoEditor() {
        return new VideoEditor(videoPath, audioPath, outputFile, timeBetweenBeats, videoTimeStamps, flags, filters, introStart, introEnd, workingDirectory);
    }
}