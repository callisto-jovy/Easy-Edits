package de.yugata.editor.editor.filter;

import de.yugata.editor.editor.EditInfo;
import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.Frame;

import java.util.function.Consumer;

public abstract class TransitionVideoFilter implements Filter {

    private String filter;
    private final String name;
    private final String description;
    private final String value;

    public TransitionVideoFilter(final EditInfo editInfo) {
        /* Data for the filter */

        final FilterInfo annotation = getClass().getAnnotation(FilterInfo.class);
        this.name = annotation.name();
        this.description = annotation.description();
        this.value = annotation.value();
    }

    @Override
    public void startFilter() throws FFmpegFrameFilter.Exception {
        throw new RuntimeException("Simple video filter cannot be started.");
    }

    @Override
    public void stopFilter() throws FFmpegFrameFilter.Exception {
        throw new RuntimeException("Simple video filter cannot be stopped.");
    }

    @Override
    public void push(Frame frame) throws FFmpegFrameFilter.Exception {
        throw new RuntimeException("Frame cannot be pushed to simple video filter.");
    }

    @Override
    public Frame pull() throws FFmpegFrameFilter.Exception {
        throw new RuntimeException("Frame cannot be pulled from simple video filter.");
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
