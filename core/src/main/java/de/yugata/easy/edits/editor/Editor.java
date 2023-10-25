package de.yugata.easy.edits.editor;

import de.yugata.easy.edits.editor.edit.EditInfo;
import de.yugata.easy.edits.editor.edit.EditInfoBuilder;
import de.yugata.easy.edits.util.FFmpegUtil;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.*;

import java.io.File;
import java.util.List;

public interface Editor {


    default FFmpegFrameGrabber baseSegmentGrabber(final File input, final FFmpegFrameRecorder recorder, final String videoCodec) throws FFmpegFrameGrabber.Exception {
        final FFmpegFrameGrabber segmentGrabber = new FFmpegFrameGrabber(input);
        FFmpegUtil.configureGrabber(segmentGrabber);
        segmentGrabber.setSampleFormat(recorder.getSampleFormat());
        segmentGrabber.setSampleRate(recorder.getSampleRate());
        segmentGrabber.setPixelFormat(recorder.getPixelFormat());
        segmentGrabber.setVideoCodecName(videoCodec);
        segmentGrabber.start();

        return segmentGrabber;
    }

    default FFmpegFrameFilter simpleAudioFilter(final FFmpegFrameRecorder recorder) throws FFmpegFrameFilter.Exception {
        final FFmpegFrameFilter simpleAudioFiler = FFmpegUtil.populateAudioFilters();
        if (simpleAudioFiler != null) {
            simpleAudioFiler.setSampleRate(recorder.getSampleRate());
            simpleAudioFiler.setSampleFormat(recorder.getSampleFormat());
            simpleAudioFiler.start();
        }
        return simpleAudioFiler;
    }

    default FFmpegFrameFilter overlayAudioFilter(final FFmpegFrameRecorder recorder) throws FFmpegFrameFilter.Exception {
        final FFmpegFrameFilter overlayFilter = FFmpegUtil.configureAudioFilter("[1:a]volume=1.35[a1]; [a1][0:a]amerge=inputs=2[a]", recorder.getSampleRate(), recorder.getSampleFormat());
        overlayFilter.setAudioInputs(2);
        overlayFilter.start();
        return overlayFilter;
    }


    default FFmpegFrameFilter convertAudioFilter(final FFmpegFrameRecorder recorder) throws FFmpegFrameFilter.Exception {
        final BytePointer sampleFormatName = avutil.av_get_sample_fmt_name(recorder.getSampleFormat());
        final FFmpegFrameFilter convertAudioFilter = FFmpegUtil.configureAudioFilter(String.format("aformat=sample_fmts=%s:sample_rates=%d", sampleFormatName.getString(), recorder.getSampleRate()), recorder.getSampleRate(), recorder.getSampleFormat());
        convertAudioFilter.setAudioInputs(1);
        convertAudioFilter.start();

        sampleFormatName.close(); // release reference
        return convertAudioFilter;
    }

    //TODO: Rework this.
    default long getEditLength(final List<String> videoPaths) throws FrameGrabber.Exception {
        long time = 0;

        for (String videoPath : videoPaths) {
            final FFmpegFrameGrabber segmentGrabber = new FFmpegFrameGrabber(videoPath);
            segmentGrabber.start();
            time += segmentGrabber.getLengthInTime();
            segmentGrabber.close();
        }

        return time;
    }



}
