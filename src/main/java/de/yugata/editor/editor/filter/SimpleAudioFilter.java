package de.yugata.editor.editor.filter;

import de.yugata.editor.editor.EditInfo;
import de.yugata.editor.editor.filter.Filter;
import de.yugata.editor.editor.filter.FilterInfo;
import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.Frame;

import java.util.function.Consumer;

public abstract class SimpleAudioFilter implements Filter {

    private String filter;
    private final String name;
    private final String description;
    private final String value;

    /* Data for the filter */

    public SimpleAudioFilter(final EditInfo editInfo) {
        final FilterInfo annotation = getClass().getAnnotation(FilterInfo.class);
        this.name = annotation.name();
        this.description = annotation.description();
        this.value = annotation.value();
    }

    @Override
    public void startFilter() throws FFmpegFrameFilter.Exception {
        throw new RuntimeException("Simple audio filter cannot be started.");
    }

    @Override
    public void stopFilter() throws FFmpegFrameFilter.Exception {
        throw new RuntimeException("Simple audio filter cannot be stopped.");
    }

    @Override
    public void push(Frame frame) throws FFmpegFrameFilter.Exception {
        throw new RuntimeException("Frame cannot be pushed to simple audio filter.");
    }

    @Override
    public Frame pull() throws FFmpegFrameFilter.Exception {
        throw new RuntimeException("Frame cannot be pulled from simple audio filter.");
    }

    @Override
    public void pushToFilter(Frame frame, Consumer<Frame> frameConsumer) throws FFmpegFrameFilter.Exception {
        throw new RuntimeException("Frame cannot be pushed to filter.");
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getFilter() {
        return filter;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getValue() {
        return value;
    }
}
