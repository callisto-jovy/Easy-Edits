package de.yugata.editor.util;

import org.bytedeco.ffmpeg.ffmpeg;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.opencv.opencv_java;

public class FFmpegUtil {



    public static FFmpegFrameGrabber createGrabber(final String filePath) throws FFmpegFrameGrabber.Exception {
        final FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(filePath);
        frameGrabber.setOption("allowed_extensions", "ALL");
        frameGrabber.setOption("hwaccel", "cuda");
        frameGrabber.setVideoCodecName("hevc_cuvid");
        frameGrabber.start();
        return frameGrabber;
    }


}
