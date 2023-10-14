package de.yugata.easy.edits.util;


import de.yugata.easy.edits.editor.EditingFlag;
import org.apache.commons.io.FileUtils;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;

/**
 * TODO: This needs some documentation, not only for other, but also for myself.
 */
public class FFmpegUtil {

    public static final File RESOURCE_DIRECTORY = new File("editor_resources");

    static {
        if (!RESOURCE_DIRECTORY.exists()) {
            RESOURCE_DIRECTORY.mkdir();
        }
    }

    //TODO: Multiple fonts??
    public static String getFontFile() {
        // Download the font file to the temp.
        File dalton = new File(RESOURCE_DIRECTORY, "dalton.otf");

        try {
            if (!dalton.exists())
                FileUtils.copyURLToFile(new URL("https://github.com/callisto-jovy/Fast-Edits/releases/download/external/Dalton.otf"), dalton);
        } catch (IOException e) {
            e.printStackTrace();
            return cleanPath("C:/Windows/Fonts/Arial.ttf");
        }

        // Our font file is in the resources, but ffmpeg needs an absolute path
        return cleanPath(dalton.getAbsolutePath());
    }

    public static String cleanPath(final String filePath) {
        return filePath
                .replace('\\', '/')
                .replace(":", "\\:");
    }

    public static void pushToFilters(final Frame frame, final FFmpegFrameRecorder recorder, final FFmpegFrameFilter... filters) {
        try {
            // just record if no filters are in the chain.
            if (filters.length == 0) {
                recorder.record(frame);
                return;
            }

            // Feed the frame to the first filter
            filters[0].push(frame);

            // Loop through the following filters.
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
            // Push the frames that moved through the entire filter chain to the recorder
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
        frameGrabber.setVideoCodecName("h264_cuvid");

        frameGrabber.setAudioStream(1);
        frameGrabber.start();
        return frameGrabber;
    }


    public static FFmpegFrameRecorder createRecorder(final File outputFile, final EnumSet<EditingFlag> editingFlags, final FFmpegFrameGrabber inputGrabber) throws FFmpegFrameRecorder.Exception {
        final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, inputGrabber.getImageWidth(), inputGrabber.getImageHeight(), 2);

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
        recorder.setFrameRate(inputGrabber.getFrameRate());
        recorder.setSampleRate(inputGrabber.getSampleRate()); //TODO: Audio sample rate
        // Select the "highest" bitrate.
        recorder.setVideoBitrate(0); // max bitrate
        //   recorder.setVideoCodecName("h264_nvenc"); // Hardware-accelerated encoding.

        return recorder;
    }
}
