package de.yugata.editor.util;

import de.yugata.editor.editor.EditingFlag;
import de.yugata.editor.model.InputVideo;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FrameFilter;

import java.io.File;
import java.util.EnumSet;

public class FFmpegUtil {


    public static void configureGrabber(final FFmpegFrameGrabber grabber) {
        grabber.setOption("allowed_extensions", "ALL");
        grabber.setOption("hwaccel", "cuda");
        grabber.setAudioStream(1);
        grabber.setVideoBitrate(0);
    }

    public static FFmpegFrameGrabber createGrabber(final String filePath) throws FFmpegFrameGrabber.Exception {
        final FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(filePath);
        frameGrabber.setOption("allowed_extensions", "ALL");
        frameGrabber.setOption("hwaccel", "cuda");
        frameGrabber.setVideoCodecName("hevc_cuvid");
        frameGrabber.setAudioStream(1);
        frameGrabber.start();
        return frameGrabber;
    }

    public static FFmpegFrameFilter createAudioFilter(final String audioFilter) {
        try (final FFmpegFrameFilter audioFrameFilter = new FFmpegFrameFilter(audioFilter, 2)) {
            audioFrameFilter.start();
            return audioFrameFilter;
        } catch (FrameFilter.Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static FFmpegFrameFilter createVideoFilter(final String videoFilter, final InputVideo inputVideo, final int pixelFormat) {
        final FFmpegFrameFilter videoFrameFilter = new FFmpegFrameFilter(videoFilter, inputVideo.width(), inputVideo.height());
        videoFrameFilter.setFrameRate(inputVideo.frameRate());
        videoFrameFilter.setPixelFormat(pixelFormat);
        return videoFrameFilter;
    }


    public static FFmpegFrameRecorder createRecorder(final File outputFile, final EnumSet<EditingFlag> editingFlags, final InputVideo inputVideo) throws FFmpegFrameRecorder.Exception {
        final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, inputVideo.width(), inputVideo.height(), 2);

        recorder.setFormat("mp4");

        // Preserve the color range for the HDR video files.
        // This lets us tone-map the hdr content later on if we want to.
        //TODO: get this from the grabber
        if (editingFlags.contains(EditingFlag.WRITE_HDR_OPTIONS)) {
            recorder.setVideoOption("color_range", "tv");
            recorder.setVideoOption("colorspace", "bt2020nc");
            recorder.setVideoOption("color_primaries", "bt2020");
            recorder.setVideoOption("color_trc", "smpte2084");
        }

        if (editingFlags.contains(EditingFlag.BEST_QUALITY)) {
            recorder.setVideoQuality(EditingFlag.BEST_QUALITY.getSetting()); // best quality --> Produces big files
            recorder.setVideoOption("cq", String.valueOf(EditingFlag.BEST_QUALITY.getSetting()));
            recorder.setOption("preset", "slow");
            recorder.setVideoOption("profile", "high444");
            recorder.setVideoOption("crf", String.valueOf(EditingFlag.BEST_QUALITY.getSetting()));
            recorder.setVideoOption("qmin", "0");
            recorder.setVideoOption("qmax", "0");
            recorder.setOption("tune", "hq");
            recorder.setOption("bf", "2");
            recorder.setOption("lookahead", "8");
            recorder.setOption("rc", "constqp");

            //  recorder.setOption("b:v", "0");
        }


        // One of the pixel formats supported by h264 nvenc
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setFrameRate(inputVideo.frameRate());
        recorder.setSampleRate(inputVideo.sampleRate());
        // Select the "highest" bitrate.
        recorder.setVideoBitrate(0); // max bitrate
        //   recorder.setVideoCodecName("h264_nvenc"); // Hardware-accelerated encoding.

        recorder.start();

        return recorder;
    }
}
