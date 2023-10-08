package de.yugata.editor.editor;

import de.yugata.editor.model.InputVideo;
import de.yugata.editor.util.FFmpegUtil;
import org.bytedeco.javacv.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_HEVC;
import static org.bytedeco.ffmpeg.global.avutil.*;

/**
 * TODO: This needs documentation & a cleanup
 */
public class VideoEditor {

    private final Queue<Double> timeBetweenBeats;
    private final List<Long> videoTimeStamps;


    /**
     * The Framegrabber which grabs the input video
     */
    private FFmpegFrameGrabber frameGrabber;

    /**
     * The video's input path
     */
    private final String videoPath;

    /**
     * The audio file's path
     */
    private final String audioPath;

    /**
     * Holds the most important meta-data about the input video. This is necessary for the frame recorder & reduces memory usage.
     */
    private InputVideo inputVideo;


    private long introStart, introEnd;

    private EnumSet<EditingFlag> editingFlags;

    private final File outputFile;

    public VideoEditor(final String videoPath,
                       final String audioPath,
                       final File outputFile,
                       final Queue<Double> timeBetweenBeats,
                       final List<Long> videoTimeStamps,
                       final EnumSet<EditingFlag> flags,
                       final long introStart,
                       final long introEnd) {

        this.videoPath = videoPath;
        this.audioPath = audioPath;
        this.timeBetweenBeats = timeBetweenBeats;
        this.videoTimeStamps = videoTimeStamps;
        this.editingFlags = flags;
        this.introStart = introStart;
        this.introEnd = introEnd;

        if (outputFile.exists()) {
            this.outputFile = new File(outputFile.getParent(), UUID.randomUUID() + outputFile.getName());
        } else {
            this.outputFile = outputFile;
        }

        if (flags.contains(EditingFlag.PRINT_DEBUG)) {
            FFmpegLogCallback.set();
            av_log_set_level(AV_LOG_PRINT_LEVEL);
        } else {
            av_log_set_level(AV_LOG_PANIC);
        }
    }


    private void initFrameGrabber() {
        if (frameGrabber == null) {
            try {
                this.frameGrabber = new FFmpegFrameGrabber(videoPath);
                FFmpegUtil.configureGrabber(frameGrabber);

                frameGrabber.setVideoCodecName("hevc_cuvid");
                frameGrabber.start();

                this.inputVideo = new InputVideo(
                        frameGrabber.getImageWidth(),
                        frameGrabber.getImageHeight(),
                        frameGrabber.getFrameRate(),
                        frameGrabber.getVideoCodec(),
                        frameGrabber.getVideoBitrate(),
                        frameGrabber.getSampleRate());

            } catch (FFmpegFrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
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
                this.frameGrabber = null;
            } catch (FrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void edit(final boolean useSegments) {
        // Write the segment files that will be stitched together.
        final List<File> segments;

        if (useSegments) {
            initFrameGrabber();
            releaseFrameGrabber();

            segments = Arrays.stream(Editor.WORKING_DIRECTORY.listFiles())
                    .sorted(Comparator.comparingInt(value -> Integer.parseInt(value.getName().substring("segment ".length(), value.getName().lastIndexOf(".")))))
                    .collect(Collectors.toList());

        } else {
            segments = writeSegments();
        }


        this.initFrameGrabber();

        // Recorder for the final product & audio grabber to overlay the audio
        try (final FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(audioPath);
             final FFmpegFrameRecorder recorder = FFmpegUtil.createRecorder(outputFile, editingFlags, inputVideo)) {
            //   recorder.setPixelFormat(inputVideo.);
           // recorder.setPixelFormat(frameGrabber.getPixelFormat());
            recorder.start();

            // Start grabbing the audio, we need this for the sample rate.
            audioGrabber.start();

            // Add the intro in front

            if (introStart != -1 && introEnd != -1) {
                this.frameGrabber.setTimestamp(introStart);

                Frame introFrame;
                while ((introFrame = frameGrabber.grab()) != null && frameGrabber.getTimestamp() < introEnd) {
                    recorder.record(introFrame);
                }
            }

            this.releaseFrameGrabber();


            /* Configuring the video filters */

            final List<FFmpegFrameFilter> videoFilters = new ArrayList<>();

            populateVideoFilters(videoFilters, audioGrabber.getLengthInTime(), recorder.getPixelFormat());


            //    final FFmpegFrameFilter sharpen = createVideoFilter("smartblur=1.5:-0.35:-3.5:0.65:0.25:2.0", recorder.getPixelFormat());
            //   sharpen.start();
            // videoFilters.add(sharpen);


            //TODO: Not available in our javacv build
            //final FFmpegFrameFilter colorCorrection = createVideoFilter("setpts=N,eq=brightness=0.1:saturation=3:gamma=1, curves=m='0/0,0.5/0.8,1/1'");


            for (final File segment : segments) {
                final FFmpegFrameGrabber segmentGrabber = new FFmpegFrameGrabber(segment);
                FFmpegUtil.configureGrabber(segmentGrabber);
                //       segmentGrabber.setVideoCodecName("h265_cuvid");
                segmentGrabber.setVideoCodecName("hevc_cuvid");
                segmentGrabber.setPixelFormat(recorder.getPixelFormat());
                segmentGrabber.start();

                if (editingFlags.contains(EditingFlag.FADE_TRANSITION)) {
                    final FFmpegFrameFilter fadeIn = FFmpegUtil.createVideoFilter(String.format("fade=t=in:st=%dus:d=%dms", 0, EditingFlag.FADE_TRANSITION.getSetting()), inputVideo, recorder.getPixelFormat());
                    fadeIn.start();
                    videoFilters.add(fadeIn);
                }

                final FFmpegFrameFilter[] videoFiltersArray = videoFilters.toArray(new FFmpegFrameFilter[]{});

                // grab the frames & send them to the filters
                Frame videoFrame;
                while ((videoFrame = segmentGrabber.grabImage()) != null) {
                    FFmpegUtil.pushToFilters(videoFrame, recorder, videoFiltersArray);
                }

                if (editingFlags.contains(EditingFlag.FADE_TRANSITION)) {
                    videoFilters.get(videoFilters.size() - 1).close();
                    videoFilters.remove(videoFilters.size() - 1);
                }

                // Close the grabber, release the resources
                segmentGrabber.close();
            }


            final List<FFmpegFrameFilter> audioFilters = new ArrayList<>();


            if (editingFlags.contains(EditingFlag.FADE_OUT_VIDEO)) {
                final int fadeOutLength = EditingFlag.FADE_OUT_VIDEO.getSetting();
                final int fadeOutStart = (int) ((audioGrabber.getLengthInTime() / 1000000L) - fadeOutLength);
                final FFmpegFrameFilter audioFadeFilter = FFmpegUtil.createAudioFilter(String.format("afade=t=out:st=%d:d=%d", fadeOutStart - 1, fadeOutLength));
                audioFadeFilter.start();
                audioFilters.add(audioFadeFilter);
            }

            // Overlay the audio
            Frame audioFrame;
            while ((audioFrame = audioGrabber.grab()) != null) {
                recorder.setTimestamp(introStart == -1 ? audioFrame.timestamp : introStart + audioFrame.timestamp);
                FFmpegUtil.pushToFilters(audioFrame, recorder, audioFilters.toArray(new FFmpegFrameFilter[]{}));
            }

            /* Clean up resources */

            for (final FFmpegFrameFilter videoFilter : videoFilters) {
                videoFilter.close();
            }

        } catch (FrameRecorder.Exception | FrameGrabber.Exception | FrameFilter.Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<File> writeSegments() {
        this.initFrameGrabber();

        final List<File> segmentFiles = new ArrayList<>();


        // The videos framerate
        final double frameRate = inputVideo.frameRate();
        // The time one frame takes in ms.
        final double frameTime = 1000 / frameRate;

        // Shuffle the sequences if the flag is toggled.
        if (editingFlags.contains(EditingFlag.SHUFFLE_SEQUENCES)) {
            Collections.shuffle(videoTimeStamps);
        }

        try {
            int nextStamp = 0;

            /* Beat loop */
            while (timeBetweenBeats.peek() != null) {
                double timeBetween = timeBetweenBeats.poll();

                // If the next stamp (index in the list) is valid, we move to the timestamp.
                // If not, we just keep recording, until no beat times are left
                // This is nice to have for ending sequences, where a last sequence is displayed for x seconds.

                if (nextStamp < videoTimeStamps.size()) {
                    final double timeStamp = videoTimeStamps.get(nextStamp);
                    frameGrabber.setTimestamp((long) timeStamp);
                }

                // Write a new segment to disk
                final File segmentFile = new File(Editor.WORKING_DIRECTORY, String.format("segment %d.mp4", nextStamp));
                segmentFiles.add(segmentFile); // Add the file to the segments.
                final FFmpegFrameRecorder recorder = FFmpegUtil.createRecorder(segmentFile, editingFlags, inputVideo);
        //        recorder.setPixelFormat(frameGrabber.getPixelFormat());
                //    recorder.setPixelFormat(frameGrabber.getPixelFormat());
                //   recorder.setVideoCodec(AV_CODEC_ID_H265);

                recorder.start();


                // Time passed in frame times.
                double localMs = 0;

                // Pick frames till the interim is filled...
                Frame frame;
                while ((frame = frameGrabber.grabImage()) != null && localMs < timeBetween) {

                    recorder.record(frame);
                    localMs += frameTime;
                }

                // Close our local recorder.
                recorder.close();
                // Advance to the next timestamp.
                nextStamp++;
            }
            /* End beat loop */

            this.releaseFrameGrabber();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return segmentFiles;
    }


    private void populateVideoFilters(final List<FFmpegFrameFilter> videoFilters, final long audioTime, final int pixelFormat) throws FFmpegFrameFilter.Exception {
        //TODO: move the filters to their own enum with a functional interface

        if (editingFlags.contains(EditingFlag.FADE_OUT_VIDEO)) {
            final int fadeOutLength = EditingFlag.FADE_OUT_VIDEO.getSetting();
            final int fadeOutStart = (int) ((audioTime / 1000000L) - fadeOutLength);


            final FFmpegFrameFilter videoFadeFilter = FFmpegUtil.createVideoFilter(String.format("fade=t=out:st=%d:d=%d", fadeOutStart - 1, fadeOutLength), inputVideo, pixelFormat);
            videoFadeFilter.start();
            videoFilters.add(videoFadeFilter);

            final String textFilter = String.format("drawtext=fontcolor=ffffff:enable='between(t,%d,%d):fontsize=128:fontfile=%s:text=yugata:x=(w-text_w)/2:y=(h-text_h)/2", fadeOutStart, fadeOutStart + fadeOutLength, FFmpegUtil.getFontFile());
            final FFmpegFrameFilter watermarkFilter = FFmpegUtil.createVideoFilter(textFilter, inputVideo, pixelFormat);
            watermarkFilter.start();
            videoFilters.add(watermarkFilter);

        }


        if (editingFlags.contains(EditingFlag.INTERPOLATE_FRAMES)) {
            final int fps = EditingFlag.INTERPOLATE_FRAMES.getSetting();
            final int factor = (int) (fps / inputVideo.frameRate());

            final String videoFilter = String.format("minterpolate=fps=%d,tblend=all_mode=average,setpts=%d*PTS", fps, factor);

            final FFmpegFrameFilter interpolateFilter = FFmpegUtil.createVideoFilter(videoFilter, inputVideo, pixelFormat);
            interpolateFilter.start();
            videoFilters.add(interpolateFilter);
        }

        if (editingFlags.contains(EditingFlag.ZOOM_IN)) {
            final FFmpegFrameFilter zoomAnimation = FFmpegUtil.createVideoFilter("zoompan=z='min(pzoom+0.00213,2.13)':x=iw/2-(iw/zoom/2):y=ih/2-(ih/zoom/2):d=1:fps=60", inputVideo, pixelFormat);
            zoomAnimation.start();
            videoFilters.add(zoomAnimation);
        }
    }

}