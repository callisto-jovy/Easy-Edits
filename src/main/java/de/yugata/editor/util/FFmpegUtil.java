package de.yugata.editor.util;

import de.yugata.editor.editor.EditingFlag;
import de.yugata.editor.model.InputVideo;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import java.io.File;
import java.util.EnumSet;

/**
 * TODO: This needs some documentation, not only for other, but also for myself.
 */
public class FFmpegUtil {


    //TODO: Multiple fonts??
    public static String getFontFile() {
        // Our font file is in the resources, but ffmpeg needs an absolute path
        return ClassLoader.getSystemClassLoader().getResource("Dalton.otf").getFile();
    }

    public static void pushToFilters(final Frame frame, final FFmpegFrameRecorder recorder, final FFmpegFrameFilter... filters) {
        try {
            if (filters.length == 0) {
                recorder.record(frame);
                return;
            }


            // Feed the frame to the first filter
            filters[0].push(frame);
            //
            for (int i = 1; i < filters.length; i++) {
                // Pull frames from predecessor
                final FFmpegFrameFilter predecessor = filters[i - 1];
                Frame processedFrame;

                while ((processedFrame = predecessor.pull()) != null) {
                    filters[i].push(processedFrame, predecessor.getPixelFormat());
                }
            }
            // Grab the frames from the last filter...

            final FFmpegFrameFilter finalFilter = filters[filters.length - 1];
            Frame processedFrame;
            while ((processedFrame = finalFilter.pull()) != null) {
                recorder.record(processedFrame, finalFilter.getPixelFormat());
            }
        } catch (FFmpegFrameRecorder.Exception | FrameFilter.Exception e) {
            e.printStackTrace();
        }
    }


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
            recorder.setVideoOption("profile", "main10");
            recorder.setVideoOption("crf", String.valueOf(EditingFlag.BEST_QUALITY.getSetting()));
            recorder.setVideoOption("qmin", "0");
            recorder.setVideoOption("qmax", "0");
            recorder.setOption("tune", "hq");
            recorder.setOption("bf", "2");
            recorder.setOption("lookahead", "8");
            recorder.setOption("rc", "constqp");

            //  recorder.setOption("b:v", "0");
        }


        recorder.setVideoCodec(avcodec.AV_CODEC_ID_HEVC);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        // One of the pixel formats supported by h264 nvenc
     //   recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setFrameRate(inputVideo.frameRate());
        recorder.setSampleRate(inputVideo.sampleRate());
        // Select the "highest" bitrate.
        recorder.setVideoBitrate(0); // max bitrate
        //   recorder.setVideoCodecName("h264_nvenc"); // Hardware-accelerated encoding.

        return recorder;
    }
}
