package de.yugata.editor.editor;

import de.yugata.editor.model.InputVideo;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import java.util.List;
import java.util.Queue;

public class Editor {


    private final Queue<Double> timeBetweenBeats;
    private final List<Double> videoTimeStamps;


    /**
     * The Framegrabber which grabs the input video
     */
    private FFmpegFrameGrabber frameGrabber;

    /**
     * The video's input path
     */
    private final String videoPath;

    private final String audioPath;

    /**
     * Holds the most important meta-data about the input video. This is necessary for the frame recorder & reduces memory usage.
     */
    private InputVideo inputVideo;


    public Editor(final String videoInput, final String audioInput, Queue<Double> timeBetweenBeats, final List<Double> videoTimeStamps) {
        this.videoPath = videoInput;
        this.audioPath = audioInput;
        this.timeBetweenBeats = timeBetweenBeats;
        this.videoTimeStamps = videoTimeStamps;
    }

    private void initFrameGrabber() {
        if (frameGrabber == null) {
            this.frameGrabber = new FFmpegFrameGrabber(videoPath);
            //frameGrabber.setPixelFormat(AV_PIX_FMT_NV12);
            frameGrabber.setOption("allowed_extensions", "ALL");
            frameGrabber.setOption("hwaccel", "cuda");
            frameGrabber.setVideoCodecName("hevc_cuvid");

            try {
                frameGrabber.start();
            } catch (FFmpegFrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }

            this.inputVideo = new InputVideo(
                    frameGrabber.getImageWidth(),
                    frameGrabber.getImageHeight(),
                    frameGrabber.getFrameRate(),
                    frameGrabber.getVideoCodec(),
                    frameGrabber.getVideoBitrate()
            );

        } else {
            try {
                frameGrabber.start();
            } catch (FFmpegFrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    private void releaseFrameGrabber() {
        if (frameGrabber != null) {
            try {
                frameGrabber.close();
            } catch (FrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
            this.frameGrabber = null;
        }
    }

    public void edit() {
        this.initFrameGrabber();
        // The videos framerate
        final double frameRate = inputVideo.frameRate();
        // The time one frame takes in ms.
        final double frameTime = 1000 / frameRate;


        try (final FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(audioPath);
             final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(videoPath + "_edit.mp4", inputVideo.width(), inputVideo.height(), 2)) {
            // Start grabbing the audio, we need this for the sample rate.
            audioGrabber.start();

            /* Frame Recorder settings */
            recorder.setFormat("mp4");
            recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
            recorder.setFrameRate(inputVideo.frameRate());
            recorder.setSampleRate(audioGrabber.getSampleRate());
            final int bitrate = 80 * inputVideo.width() * inputVideo.height();
            recorder.setVideoBitrate(Math.max(bitrate, inputVideo.bitrate())); // max bitrate
            recorder.setVideoQuality(0);
            recorder.setVideoCodecName("h264_nvenc"); // Hardware-accelerated encoding.
            recorder.start();

            int nextStamp = 0;

            while (timeBetweenBeats.peek() != null) {
                final double timeBetween = timeBetweenBeats.poll();
                // Pick frames till the interim is filled...
                if (nextStamp < videoTimeStamps.size()) {
                    final double timeStamp = videoTimeStamps.get(nextStamp);
                    frameGrabber.setTimestamp((long) timeStamp);
                }

                double localMs = 0;

                Frame frame;
                while ((frame = frameGrabber.grabImage()) != null && localMs < timeBetween) {
                    recorder.record(frame);
                    localMs += frameTime;
                }
                nextStamp++;
            }


            // Overlay the audio
            Frame audioFrame;
            while ((audioFrame = audioGrabber.grab()) != null) {
                recorder.setTimestamp(audioFrame.timestamp);
                recorder.record(audioFrame);
            }
            // Close the frame filters, to avoid endless try-catch hell
            //    videoFrameFilter.close();
        } catch (FrameRecorder.Exception | FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }

        this.releaseFrameGrabber();
    }


    private void pushToFilters(final Frame frame, final FFmpegFrameRecorder recorder, final FFmpegFrameFilter... filters) {
        try {

            // Feed the frame to the first filter
            filters[0].push(frame);
            //
            for (int i = 1; i < filters.length; i++) {
                // Pull frames from predecessor
                final FFmpegFrameFilter predecessor = filters[i - 1];
                Frame processedFrame;
                while ((processedFrame = predecessor.pull()) != null) {
                    filters[i].push(processedFrame);
                }
            }
            // Grab the frames from the last filter...

            final FFmpegFrameFilter finalFilter = filters[filters.length - 1];
            Frame processedFrame;
            while ((processedFrame = finalFilter.pull()) != null) {
                recorder.setTimestamp(processedFrame.timestamp);
                recorder.record(processedFrame);
            }
        } catch (FFmpegFrameFilter.Exception | FFmpegFrameRecorder.Exception e) {
            e.printStackTrace();
        }
    }
}
