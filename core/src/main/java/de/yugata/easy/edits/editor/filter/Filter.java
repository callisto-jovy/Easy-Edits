package de.yugata.easy.edits.editor.filter;

import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.Frame;

import java.util.function.Consumer;

public interface Filter {


    void startFilter() throws FFmpegFrameFilter.Exception;

    void stopFilter() throws FFmpegFrameFilter.Exception;

    void pushToFilter(final Frame frame, final Consumer<Frame> frameConsumer) throws FFmpegFrameFilter.Exception;

    void push(final Frame frame) throws FFmpegFrameFilter.Exception;

    Frame pull() throws FFmpegFrameFilter.Exception;

}
