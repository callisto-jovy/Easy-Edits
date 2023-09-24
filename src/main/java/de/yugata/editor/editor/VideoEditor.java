package de.yugata.editor.editor;

import de.yugata.editor.model.InputVideo;
import de.yugata.editor.util.FFmpegUtil;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

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


    private final File outputFile;

    public VideoEditor(final File outputFile, final String videoInput, final String audioInput, final Queue<Double> timeBetweenBeats, final List<Long> videoTimeStamps) {
        this.videoPath = videoInput;
        this.audioPath = audioInput;
        this.timeBetweenBeats = timeBetweenBeats;
        this.videoTimeStamps = videoTimeStamps;
        if (outputFile.exists()) {
            this.outputFile = new File(outputFile.getParent(), UUID.randomUUID() + outputFile.getName());
        } else {
            this.outputFile = outputFile;
        }
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

    public void edit(final EnumSet<EditingFlag> flags, final long introStart, final long introEnd) {
        // Write the segment files that will be stitched together.
        final List<File> segments = this.writeSegments(flags);

        /*
        final List<File> segments = Arrays.stream(Editor.WORKING_DIRECTORY.listFiles()).sorted(Comparator.comparingInt(value -> Integer.parseInt(value.getName().substring("segment ".length(), value.getName().lastIndexOf("."))))).collect(Collectors.toList());

        this.initFrameGrabber();
        this.releaseFrameGrabber();

         */

        // Recorder for the final product & audio grabber to overlay the audio
        try (final FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(audioPath);
             final FFmpegFrameRecorder recorder = getRecorder(outputFile, flags)) {

            // Start grabbing the audio, we need this for the sample rate.
            audioGrabber.start();

            // Add the intro in front

            if (introStart != -1 && introEnd != -1) {
                this.initFrameGrabber();
                this.frameGrabber.setTimestamp(introStart);

                Frame introFrame;
                while ((introFrame = frameGrabber.grab()) != null && frameGrabber.getTimestamp() < introEnd) {
                    recorder.record(introFrame);
                }

                this.releaseFrameGrabber();
            }


            /* Configuring the video filters */

            final List<FFmpegFrameFilter> videoFilters = new ArrayList<>();

            if (flags.contains(EditingFlag.INTERPOLATE_FRAMES)) {
                final int fps = EditingFlag.INTERPOLATE_FRAMES.getSetting();
                final int factor = (int) (fps / recorder.getFrameRate());

                final String videoFilter = String.format("minterpolate=fps=%d,tblend=all_mode=average,setpts=%d*PTS", fps, factor);

                final FFmpegFrameFilter interpolateFilter = createVideoFilter(videoFilter, recorder.getPixelFormat());
                interpolateFilter.start();
                videoFilters.add(interpolateFilter);
            }

            if (flags.contains(EditingFlag.FADE_OUT_VIDEO)) {
                final int fadeOutLength = EditingFlag.FADE_OUT_VIDEO.getSetting();
                final int fadeOutStart = (int) ((audioGrabber.getLengthInTime() / 1000000L) - fadeOutLength);
                final FFmpegFrameFilter videoFadeFilter = createVideoFilter(String.format("fade=t=out:st=%d:d=%d", fadeOutStart - 1, fadeOutLength), recorder.getPixelFormat());
                videoFadeFilter.start();
                videoFilters.add(videoFadeFilter);
            }

            if (flags.contains(EditingFlag.ZOOM_IN)) {

                final FFmpegFrameFilter zoomAnimation = createVideoFilter("zoompan=z='min(pzoom+0.00213,2.13)':x=iw/2-(iw/zoom/2):y=ih/2-(ih/zoom/2):d=1:fps=60", recorder.getPixelFormat());
                zoomAnimation.start();
                videoFilters.add(zoomAnimation);
            }

            //    final FFmpegFrameFilter sharpen = createVideoFilter("smartblur=1.5:-0.35:-3.5:0.65:0.25:2.0", recorder.getPixelFormat());
            //   sharpen.start();
            // videoFilters.add(sharpen);


            // Array with all our video filters. This is needed for the pushToFilters method. Otherwise, the filters would be a new array every time.
            // This is just how java works.
            //   final FFmpegFrameFilter[] videoFiltersArray = videoFilters.toArray(new FFmpegFrameFilter[]{});


            //TODO: Not available in our javacv build
            //final FFmpegFrameFilter colorCorrection = createVideoFilter("setpts=N,eq=brightness=0.1:saturation=3:gamma=1, curves=m='0/0,0.5/0.8,1/1'");
            //final FFmpegFrameFilter toneMapping = createVideoFilter("zscale=t=linear:npl=100,format=gbrpf32le,zscale=p=bt709,tonemap=tonemap=hable:desat=0,zscale=t=bt709:m=bt709:r=tv,format=yuv420p");


            for (final File segment : segments) {
                final FFmpegFrameGrabber segmentGrabber = new FFmpegFrameGrabber(segment);
                segmentGrabber.setOption("allowed_extensions", "ALL");
                segmentGrabber.setOption("hwaccel", "cuda");
                segmentGrabber.setVideoBitrate(0);
                segmentGrabber.setVideoCodecName("h265_cuvid");
                segmentGrabber.setPixelFormat(recorder.getPixelFormat());
                segmentGrabber.start();

                if (flags.contains(EditingFlag.FADE_TRANSITION)) {
                    final FFmpegFrameFilter fadeIn = createVideoFilter(String.format("fade=t=in:st=%dus:d=%dms", 0, EditingFlag.FADE_TRANSITION.getSetting()), recorder.getPixelFormat());
                    fadeIn.start();
                    videoFilters.add(fadeIn);
                }

                final FFmpegFrameFilter[] videoFiltersArray = videoFilters.toArray(new FFmpegFrameFilter[]{});

                // grab the frames & send them to the filters
                Frame videoFrame;
                while ((videoFrame = segmentGrabber.grabImage()) != null) {
                    pushToFilters(videoFrame, recorder, videoFiltersArray);
                }

                if (flags.contains(EditingFlag.FADE_TRANSITION)) {
                    videoFilters.get(videoFilters.size() - 1).close();
                    videoFilters.remove(videoFilters.size() - 1);
                }

                // Close the grabber, release the resources
                segmentGrabber.close();
            }


            final List<FFmpegFrameFilter> audioFilters = new ArrayList<>();

            /*
            if (flags.contains(EditingFlag.FADE_OUT_VIDEO)) {
                final int fadeOutLength = EditingFlag.FADE_OUT_VIDEO.getSetting();
                final int fadeOutStart = (int) ((audioGrabber.getLengthInTime() / 1000000L) - fadeOutLength);
                final FFmpegFrameFilter audioFadeFilter = createAudioFilter(String.format("fade=t=out:st=%d:d=%d", fadeOutStart - 1, fadeOutLength));
                audioFilters.add(audioFadeFilter);
            }

             */

            // Overlay the audio
            Frame audioFrame;
            while ((audioFrame = audioGrabber.grab()) != null) {
                recorder.setTimestamp(introStart == -1 ? audioFrame.timestamp : introStart + audioFrame.timestamp);
                pushToFilters(audioFrame, recorder, audioFilters.toArray(new FFmpegFrameFilter[]{}));
            }

            /* Clean up resources */

            for (final FFmpegFrameFilter videoFilter : videoFilters) {
                videoFilter.close();
            }

        } catch (FrameRecorder.Exception | FrameGrabber.Exception | FrameFilter.Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<File> writeSegments(final EnumSet<EditingFlag> flags) {
        this.initFrameGrabber();

        //TODO: We could just read the files in the dir & sort them based on their name?
        final List<File> segmentFiles = new ArrayList<>();


        // The videos framerate
        final double frameRate = inputVideo.frameRate();
        // The time one frame takes in ms.
        final double frameTime = 1000 / frameRate;

        if (flags.contains(EditingFlag.SHUFFLE_SEQUENCES)) {
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
                final FFmpegFrameRecorder recorder = getRecorder(segmentFile, flags);

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


    private FFmpegFrameRecorder getRecorder(final File outputFile, final EnumSet<EditingFlag> flags) throws FFmpegFrameRecorder.Exception {
        final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, inputVideo.width(), inputVideo.height(), 2);

        recorder.setFormat("mp4");
        recorder.setVideoCodecName("h265_nvenc"); // Hardware-accelerated encoding.

        // Preserve the color range for the HDR video files.
        // This lets us tone-map the hdr content later on if we want to.
        //TODO: get this from the grabber
        if (flags.contains(EditingFlag.WRITE_HDR_OPTIONS)) {
            recorder.setVideoOption("color_range", "tv");
            recorder.setVideoOption("colorspace", "bt2020nc");
            recorder.setVideoOption("color_primaries", "bt2020");
            recorder.setVideoOption("color_trc", "smpte2084");
        }

        if (flags.contains(EditingFlag.BEST_QUALITY)) {
            recorder.setVideoQuality(EditingFlag.BEST_QUALITY.getSetting()); // best quality --> Produces big files
            recorder.setVideoOption("cq", String.valueOf(EditingFlag.BEST_QUALITY.getSetting()));
            recorder.setOption("preset", "slow");
            recorder.setVideoOption("profile", "high444");
            recorder.setVideoOption("crf", String.valueOf(EditingFlag.BEST_QUALITY.getSetting()));
            recorder.setVideoOption("qmin", "0");
            recorder.setVideoOption("qmax", "24");
            recorder.setOption("tune", "hq");
            recorder.setOption("bf", "2");
            recorder.setOption("lookahead", "8");
            recorder.setOption("rc", "vbr");

            //  recorder.setOption("b:v", "0");
        }


        // One of the pixel formats supported by h264 nvenc
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setFrameRate(inputVideo.frameRate());
        recorder.setSampleRate(inputVideo.sampleRate());
        // Select the "highest" bitrate.
        recorder.setVideoBitrate(0); // max bitrate

        recorder.start();

        return recorder;
    }


    private FFmpegFrameFilter createVideoFilter(final String videoFilter, final int pixelFormat) throws FFmpegFrameFilter.Exception {
        final FFmpegFrameFilter videoFrameFilter = new FFmpegFrameFilter(videoFilter, inputVideo.width(), inputVideo.height());
        videoFrameFilter.setFrameRate(inputVideo.frameRate());
        videoFrameFilter.setPixelFormat(pixelFormat);
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
}