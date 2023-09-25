package de.yugata.editor.editor;

import java.io.File;
import java.util.*;

public class VideoEditorBuilder {
    private String videoPath;
    private String audioPath;
    private File outputFile;
    private Queue<Double> timeBetweenBeats = new ArrayDeque<>();
    private List<Long> videoTimeStamps = new ArrayList<>();
    private EnumSet<EditingFlag> flags = EnumSet.noneOf(EditingFlag.class);

    private long introStart = -1, introEnd = -1;

    public VideoEditorBuilder setIntroStart(long introStart) {
        this.introStart = introStart;
        return this;
    }

    public VideoEditorBuilder setIntroEnd(long introEnd) {
        this.introEnd = introEnd;
        return this;
    }

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

    public VideoEditor createVideoEditor() {
        return new VideoEditor(videoPath, audioPath, outputFile, timeBetweenBeats, videoTimeStamps, flags, introStart, introEnd);
    }
}