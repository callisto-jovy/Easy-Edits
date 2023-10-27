package de.yugata.easy.edits.editor;

import de.yugata.easy.edits.util.FFmpegUtil;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FrameGrabber;

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
        final BytePointer sampleFormatName = avutil.av_get_sample_fmt_name(recorder.getSampleFormat());


        final String aformat = String.format("aformat=%s:sample_rates=%d:channel_layouts=stereo", sampleFormatName.getString(), recorder.getSampleRate());


        final FFmpegFrameFilter overlayFilter = FFmpegUtil.configureAudioFilter(String.format("[0:a]volume=2.25, %s[a1]; [1:a]volume=0.25, %s[a2]; [a1][a2]amerge,pan=stereo|c0<c0+c2|c1<c1+c3[a]", aformat, aformat), recorder.getSampleRate(), recorder.getSampleFormat());
        overlayFilter.setAudioInputs(2);
        overlayFilter.start();
        sampleFormatName.close(); // release reference
        return overlayFilter;
    }


    default FFmpegFrameFilter convertAudioFilter(final FFmpegFrameRecorder recorder) throws FFmpegFrameFilter.Exception {
        final BytePointer sampleFormatName = avutil.av_get_sample_fmt_name(recorder.getSampleFormat());
        final FFmpegFrameFilter convertAudioFilter = FFmpegUtil.configureAudioFilter(String.format("aformat=sample_fmts=%s:sample_rates=%d:channel_layouts=stereo", sampleFormatName.getString(), recorder.getSampleRate()), recorder.getSampleRate(), recorder.getSampleFormat());
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
