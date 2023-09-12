package de.yugata.editor.editor;

import de.yugata.editor.model.InputVideo;
import de.yugata.editor.util.FFmpegUtil;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;

public class VideoEditor {

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

    /**
     * The audio file's path
     */
    private final String audioPath;

    /**
     * Holds the most important meta-data about the input video. This is necessary for the frame recorder & reduces memory usage.
     */
    private InputVideo inputVideo;


    private final File outputFile;

    public VideoEditor(final File outputFile, final String videoInput, final String audioInput, final Queue<Double> timeBetweenBeats, final List<Double> videoTimeStamps) {
        this.videoPath = videoInput;
        this.audioPath = audioInput;
        this.timeBetweenBeats = timeBetweenBeats;
        this.videoTimeStamps = videoTimeStamps;
        this.outputFile = outputFile;
    }

    private void initFrameGrabber() {
        if (frameGrabber == null) {
            try {
                this.frameGrabber = FFmpegUtil.createGrabber(videoPath);
                this.inputVideo = new InputVideo(
                        frameGrabber.getImageWidth(),
                        frameGrabber.getImageHeight(),
                        frameGrabber.getFrameRate(),
                        frameGrabber.getVideoCodec(),
                        frameGrabber.getVideoBitrate()
                );

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

    public void edit(final EnumSet<EditingFlag> flags) {
        this.initFrameGrabber();
        // The videos framerate
        final double frameRate = inputVideo.frameRate();
        // The time one frame takes in ms.
        final double frameTime = 1000 / frameRate;

        try (final FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(audioPath);
             final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, inputVideo.width(), inputVideo.height(), 2)) {

            // Start grabbing the audio, we need this for the sample rate.
            audioGrabber.start();


            /* Configure the recorder */
            recorder.setFormat("mp4");

            // Preserve the color range for the HDR video files.
            // This lets us tone-map the hdr content later on if we want to.
            //TODO: get this from the grabber
            if (flags.contains(EditingFlag.WRITE_HDR_OPTIONS)) {
                recorder.setVideoOption("color_range", "tv");
                recorder.setVideoOption("colorspace", "bt2020nc");
                recorder.setVideoOption("color_primaries", "bt2020");
                recorder.setVideoOption("color_trc", "smpte2084");
            }

            if (flags.contains(EditingFlag.BEST_QUALITY))
                recorder.setVideoQuality(0); // best quality --> Produces big files

            // One of the pixel formats supported by h264 nvenc
            recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
            recorder.setFrameRate(inputVideo.frameRate());
            recorder.setSampleRate(frameGrabber.getSampleRate());
            // Select the "highest" bitrate. If the video has a lower bitrate than HD, we just set it to hd.
            final int bitrate = 80 * inputVideo.width() * inputVideo.height();
            recorder.setVideoBitrate(Math.max(bitrate, inputVideo.bitrate())); // max bitrate
            recorder.setVideoCodecName("h264_nvenc"); // Hardware-accelerated encoding.

            recorder.start();
            /* End configuring the recorder */

            /* Configuring the video filters */

            //TODO: Find out why javacv is retarded

            final List<FFmpegFrameFilter> videoFilters = new ArrayList<>();

            if (flags.contains(EditingFlag.INTERPOLATE_FRAMES)) {
                final FFmpegFrameFilter interpolateFilter = createVideoFilter("setpts=N,minterpolate=fps=60,tblend=all_mode=average");
                videoFilters.add(interpolateFilter);
            }

            if (flags.contains(EditingFlag.FADE_OUT_VIDEO)) {
                //TODO: THIS SHOULD BE A SETTING
                // for now, we just fade out for the last four seconds.
                final int fadeOutLength = 4;
                final int fadeOutStart = (int) ((audioGrabber.getLengthInTime() / 1000000L) - fadeOutLength);
                final FFmpegFrameFilter videoFadeFilter = createVideoFilter(String.format("fade=t=out:st=%d:d=%d", fadeOutStart - 1, fadeOutLength));
                videoFilters.add(videoFadeFilter);
            }

            //TODO: Not available in our javacv build
            //final FFmpegFrameFilter colorCorrection = createVideoFilter("setpts=N,eq=brightness=0.1:saturation=3:gamma=1, curves=m='0/0,0.5/0.8,1/1'");
            //final FFmpegFrameFilter toneMapping = createVideoFilter("zscale=t=linear:npl=100,format=gbrpf32le,zscale=p=bt709,tonemap=tonemap=hable:desat=0,zscale=t=bt709:m=bt709:r=tv,format=yuv420p");


            // Array with all our video filters. This is needed for the pushToFilters method. Otherwise, the filters would be a new array every time.
            // This is just how java works.
            final FFmpegFrameFilter[] videoFiltersArray = videoFilters.toArray(new FFmpegFrameFilter[]{});

            /* End filters */

            int nextStamp = 0;


            /* Needed for transitions, I have to fix this Clusterfuck someday */
            Frame lastFrame = null;
            /* Beat loop */
            while (timeBetweenBeats.peek() != null) {
                final double timeBetween = timeBetweenBeats.poll();

                // If the next stamp (index in the list) is valid, we move to the timestamp.
                // If not, we just keep recording, until no beat times are left
                // This is nice to have for ending sequences, where a last sequence is displayed for x seconds.

                long filterStamp = -1;
                if (nextStamp < videoTimeStamps.size()) {
                    final double timeStamp = videoTimeStamps.get(nextStamp);
                    frameGrabber.setTimestamp((long) timeStamp);

                    filterStamp = frameGrabber.getTimestamp();
                }

                // This filter is an anomaly. We have to have it here, as we need the offset
                // Maybe we need to add the offset
                //      final FFmpegFrameFilter hBlurTransition = createVideoFilter(String.format("[0:v][1:v]xfade=transition=hblur:duration=%sms:offset=%sms[v],[v]setpts=N[v]", 15, filterStamp));
                //      hBlurTransition.setVideoInputs(2);
                //      hBlurTransition.start();

                // Time passed in frame times.xxx
                double localMs = 0;

                // Pick frames till the interim is filled...
                Frame frame;
                while ((frame = frameGrabber.grabImage()) != null && localMs < timeBetween) {

                    pushToFilters(frame, recorder, videoFiltersArray);

                    /*
                    // This is fucking stupid
                    if (filterStamp != -1 && lastFrame != null) {
                        hBlurTransition.push(0, lastFrame);
                        hBlurTransition.push(1, frame);

                        Frame processedFrame;
                        while ((processedFrame = hBlurTransition.pull()) != null) {
                            recorder.record(processedFrame);
                        }

                        hBlurTransition.close();
                        lastFrame.close();
                        lastFrame = null;
                    } else {
                        pushToFilters(frame, recorder, videoFilters);
                    }

                     */

                    localMs += frameTime;
                }

                /*
                // We copy the last frame after a sequence is finished, in order to transition between the last frame and the first frame
                if (frame != null) // Just for safety reasons
                    lastFrame = frame.clone();

                 */
                // Advance to the next timestamp.
                nextStamp++;
            }
            /* End beat loop */

            this.releaseFrameGrabber();


            //final FFmpegFrameFilter fadeOutAudioFilter = createAudioFilter(String.format("afade=t=out:st=%d:d=%d", fadeOutStart, fadeOutLength));


            // Overlay the audio
            Frame audioFrame;
            while ((audioFrame = audioGrabber.grab()) != null) {
                recorder.setTimestamp(audioFrame.timestamp);
                recorder.record(audioFrame);
            }

            // Close the frame filters, to avoid endless try-catch hell

            //        videoFade.close();
            //      fadeOutAudioFilter.close();
        } catch (FrameRecorder.Exception | FrameGrabber.Exception | FrameFilter.Exception e) {
            e.printStackTrace();
        }
    }


    private FFmpegFrameFilter createVideoFilter(final String videoFilter) throws FFmpegFrameFilter.Exception {
        if (frameGrabber == null)
            return null;

        final FFmpegFrameFilter videoFrameFilter = new FFmpegFrameFilter(videoFilter, frameGrabber.getImageWidth(), frameGrabber.getImageHeight());
        videoFrameFilter.setFrameRate(frameGrabber.getFrameRate());
        videoFrameFilter.setPixelFormat(frameGrabber.getPixelFormat());
        return videoFrameFilter;
    }

    private FFmpegFrameFilter createAudioFilter(final String audioFilter) {
        try (final FFmpegFrameFilter audioFrameFilter = new FFmpegFrameFilter(audioFilter, 2)) {
            audioFrameFilter.start();
            return audioFrameFilter;
        } catch (FrameFilter.Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void pushToFilters(final Frame frame, final FFmpegFrameRecorder recorder, final FFmpegFrameFilter... filters) {
        try {
            for (FFmpegFrameFilter filter : filters) {
                filter.restart();
            }

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
                recorder.record(processedFrame);
            }

            for (FFmpegFrameFilter filter : filters) {
                filter.close();
            }
        } catch (FFmpegFrameRecorder.Exception | FrameFilter.Exception e) {
            e.printStackTrace();
        }
    }
}