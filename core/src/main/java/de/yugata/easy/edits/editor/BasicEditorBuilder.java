package de.yugata.easy.edits.editor;

import de.yugata.easy.edits.editor.edit.EditingFlag;
import de.yugata.easy.edits.filter.FilterWrapper;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class BasicEditorBuilder {
    private FFmpegFrameGrabber videoGrabber;
    private String audioPath;
    private FFmpegFrameRecorder recorder;
    private List<FilterWrapper> filters = new ArrayList<>();
    private EnumSet<EditingFlag> editingFlags = EnumSet.noneOf(EditingFlag.class);

    public BasicEditorBuilder setVideoGrabber(FFmpegFrameGrabber videoGrabber) {
        this.videoGrabber = videoGrabber;
        return this;
    }

    public BasicEditorBuilder setAudioPath(String audioPath) {
        this.audioPath = audioPath;
        return this;
    }

    public BasicEditorBuilder setRecorder(FFmpegFrameRecorder recorder) {
        this.recorder = recorder;
        return this;
    }

    public BasicEditorBuilder setFilters(List<FilterWrapper> filters) {
        this.filters = filters;
        return this;
    }

    public BasicEditorBuilder setEditingFlags(EnumSet<EditingFlag> editingFlags) {
        this.editingFlags = editingFlags;
        return this;
    }

    public BasicEditor createBasicEditor() {
        return new BasicEditor(videoGrabber, audioPath, recorder, filters, editingFlags);
    }
}