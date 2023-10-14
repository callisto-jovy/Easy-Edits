package de.yugata.easy.edits.editor;


import de.yugata.easy.edits.editor.filter.*;
import de.yugata.easy.edits.util.FFmpegUtil;
import org.bytedeco.javacv.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_VERBOSE;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;

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

    private long introStart, introEnd;

    private final EnumSet<EditingFlag> editingFlags;

    private final List<FilterWrapper> filters;

    private final File outputFile, workingDirectory;


    public VideoEditor(final String videoPath,
                       final String audioPath,
                       final File outputFile,
                       final Queue<Double> timeBetweenBeats,
                       final List<Long> videoTimeStamps,
                       final EnumSet<EditingFlag> flags,
                       final List<FilterWrapper> filters,
                       final long introStart,
                       final long introEnd,
                       File workingDirectory) {

        this.videoPath = videoPath;
        this.audioPath = audioPath;
        this.timeBetweenBeats = timeBetweenBeats;
        this.videoTimeStamps = videoTimeStamps;
        this.editingFlags = flags;
        this.introStart = introStart;
        this.introEnd = introEnd;
        this.filters = filters;
        this.workingDirectory = workingDirectory;

        if (outputFile.exists()) {
            this.outputFile = new File(outputFile.getParent(), UUID.randomUUID() + outputFile.getName());
        } else {
            this.outputFile = outputFile;
        }

        if (flags.contains(EditingFlag.PRINT_DEBUG)) {
            FFmpegLogCallback.set();
            av_log_set_level(AV_LOG_VERBOSE);
        }
    }


    private void initFrameGrabber() {
        if (frameGrabber == null) {
            try {
                this.frameGrabber = new FFmpegFrameGrabber(videoPath);
                FFmpegUtil.configureGrabber(frameGrabber);

                frameGrabber.setVideoCodecName("hevc_cuvid");
                frameGrabber.start();

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

            segments = Arrays.stream(workingDirectory.listFiles())
                    .sorted(Comparator.comparingInt(value -> Integer.parseInt(value.getName().substring("segment ".length(), value.getName().lastIndexOf(".")))))
                    .collect(Collectors.toList());

        } else {
            segments = writeSegments();
        }


        this.initFrameGrabber();

        // Recorder for the final product & audio grabber to overlay the audio
        try (final FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(audioPath);
             final FFmpegFrameRecorder recorder = FFmpegUtil.createRecorder(outputFile, editingFlags, frameGrabber)) {
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


            // Edit: I fucking hate this, we just pass the frame grabber in the fucking future...
            final EditInfo editInfo = new EditInfoBuilder()
                    .setEditTime(audioGrabber.getLengthInTime())
                    .setAudioCodec(audioGrabber.getAudioCodec())
                    .setAspectRatio(frameGrabber.getAspectRatio())
                    .setAudioChannels(audioGrabber.getAudioChannels())
                    .setAudioBitrate(audioGrabber.getAudioBitrate())
                    .setAudioMetadata(audioGrabber.getAudioMetadata())
                    .setAudioOptions(audioGrabber.getAudioOptions())
                    .setBpp(frameGrabber.getBitsPerPixel())
                    .setDeinterlace(frameGrabber.isDeinterlace())
                    .setAudioSideData(audioGrabber.getAudioSideData())
                    .setFrameRate(frameGrabber.getFrameRate())
                    .setGamma(frameGrabber.getGamma())
                    .setImageHeight(frameGrabber.getImageHeight())
                    .setImageWidth(frameGrabber.getImageWidth())
                    .setAudioCodecName(audioGrabber.getAudioCodecName())
                    .setMetadata(frameGrabber.getMetadata())
                    .setOptions(frameGrabber.getOptions())
                    .setSampleFormat(audioGrabber.getSampleFormat())
                    .setVideoBitrate(frameGrabber.getVideoBitrate())
                    .setImageScalingFlags(frameGrabber.getImageScalingFlags())
                    .setSampleRate(audioGrabber.getSampleRate())
                    .setVideoCodec(frameGrabber.getVideoCodec())
                    .setVideoCodecName(frameGrabber.getVideoCodecName())
                    .setVideoMetadata(frameGrabber.getVideoMetadata())
                    .setVideoOptions(frameGrabber.getVideoOptions())
                    .setVideoSideData(frameGrabber.getVideoSideData())
                    .setPixelFormat(recorder.getPixelFormat())
                    .setIntroStart(introStart)
                    .setIntroEnd(introEnd)
                    .createEditInfo();



            /* Configuring the video filters */

            final FFmpegFrameFilter simpleVideoFiler = this.populateVideoFilters(editInfo);

            for (final File segment : segments) {
                final FFmpegFrameGrabber segmentGrabber = new FFmpegFrameGrabber(segment);
                FFmpegUtil.configureGrabber(segmentGrabber);
                segmentGrabber.setVideoCodecName("hevc_cuvid");
                segmentGrabber.setPixelFormat(recorder.getPixelFormat());
                segmentGrabber.start();


                final FFmpegFrameFilter transitionFilter = populateTransitionFilters(editInfo);

                final FFmpegFrameFilter[] filters;

                if (simpleVideoFiler == null) {
                    filters = new FFmpegFrameFilter[]{};
                } else {
                    if (transitionFilter == null) {
                        filters = new FFmpegFrameFilter[]{simpleVideoFiler};
                    } else {
                        filters = new FFmpegFrameFilter[]{transitionFilter, simpleVideoFiler};
                    }
                }

                // grab the frames & send them to the filters
                Frame videoFrame;
                while ((videoFrame = segmentGrabber.grabImage()) != null) {
                    FFmpegUtil.pushToFilters(videoFrame, recorder, filters);
                }

                if (transitionFilter != null)
                    transitionFilter.close();

                // Close the grabber, release the resources
                segmentGrabber.close();
            }

            /* Clean up resources */
            if (simpleVideoFiler != null)
                simpleVideoFiler.stop();


            this.recordAudio(audioGrabber, recorder, editInfo);
        } catch (FrameRecorder.Exception | FrameGrabber.Exception | FrameFilter.Exception e) {
            throw new RuntimeException(e);
        }

        this.releaseFrameGrabber();
    }

    private void recordAudio(final FFmpegFrameGrabber audioGrabber, final FFmpegFrameRecorder recorder, final EditInfo editInfo) throws FFmpegFrameFilter.Exception, FFmpegFrameGrabber.Exception, FFmpegFrameRecorder.Exception {
        final FFmpegFrameFilter simpleAudioFiler = this.populateAudioFilters(editInfo);

        /* Audio frame grabbing */
        Frame audioFrame;
        while ((audioFrame = audioGrabber.grab()) != null) {

            // offset the audio timestamp by the intro start, so that the intro keeps its original audio if that's requested.
            // this then can be paired with a slow fade in of x seconds for the intro
            if (editingFlags.contains(EditingFlag.OFFSET_AUDIO_FOR_INTRO)) {
                recorder.setTimestamp(introStart == -1 ? audioFrame.timestamp : introStart + audioFrame.timestamp);
            } else {
                recorder.setTimestamp(audioFrame.timestamp);
            }


            if (simpleAudioFiler == null) {
                recorder.record(audioFrame);
            } else {
                simpleAudioFiler.push(audioFrame);

                while ((audioFrame = simpleAudioFiler.pull()) != null) {
                    recorder.record(audioFrame);
                }
            }
        }

        /* End audio grabbing */

        /* Clean up resources */
        if (simpleAudioFiler != null)
            simpleAudioFiler.stop();
    }


    public List<File> writeSegments() {
        this.initFrameGrabber();

        final List<File> segmentFiles = new ArrayList<>();


        // The videos framerate
        final double frameRate = frameGrabber.getFrameRate();
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
                final File segmentFile = new File(workingDirectory, String.format("segment %d.mp4", nextStamp));
                segmentFiles.add(segmentFile); // Add the file to the segments.
                final FFmpegFrameRecorder recorder = FFmpegUtil.createRecorder(segmentFile, editingFlags, frameGrabber);
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

    private FFmpegFrameFilter populateTransitionFilters(EditInfo editInfo) throws FFmpegFrameFilter.Exception {
        final StringBuilder combinedFilters = new StringBuilder();

        final List<TransitionVideoFilter> transitions = FilterManager.FILTER_MANAGER.getTransitions(filters, editInfo);

        for (int i = 0; i < transitions.size(); i++) {
            final TransitionVideoFilter transition = transitions.get(i);

            combinedFilters
                    .append(transition.getFilter());

            if (i < transitions.size() - 1) {
                combinedFilters.append(",");
            }
        }


        if (combinedFilters.length() == 0)
            return null;

        final FFmpegFrameFilter transitionFilter = new FFmpegFrameFilter(combinedFilters.toString(), editInfo.getImageWidth(), editInfo.getImageHeight());
        transitionFilter.setPixelFormat(editInfo.getPixelFormat());
        transitionFilter.setFrameRate(editInfo.getFrameRate());
        transitionFilter.start();

        return transitionFilter;
    }


    private FFmpegFrameFilter populateAudioFilters(EditInfo editInfo) throws FFmpegFrameFilter.Exception {
        final StringBuilder combinedFilters = new StringBuilder();

        final List<SimpleAudioFilter> audioFilters = FilterManager.FILTER_MANAGER.getAudioFilters(filters, editInfo);
        for (int i = 0; i < audioFilters.size(); i++) {
            final SimpleAudioFilter audioFilter = audioFilters.get(i);

            combinedFilters
                    .append(audioFilter.getFilter());

            if (i < audioFilters.size() - 1) {
                combinedFilters.append(",");
            }
        }

        if (combinedFilters.length() == 0)
            return null;

        System.out.println(combinedFilters);
        final FFmpegFrameFilter audioFilter = new FFmpegFrameFilter(combinedFilters.toString(), 2);
        audioFilter.start();

        return audioFilter;
    }




    /*
    TODO: find a new way, maybe just a manager with all the filters, that shouldnt be bad..
     */

    private FFmpegFrameFilter populateVideoFilters(final EditInfo editInfo) throws FFmpegFrameFilter.Exception {
        final StringBuilder combinedFilters = new StringBuilder();

        final List<SimpleVideoFilter> videoFilters = FilterManager.FILTER_MANAGER.getVideoFilters(filters, editInfo);

        for (int i = 0; i < videoFilters.size(); i++) {
            final SimpleVideoFilter videoFilter = videoFilters.get(i);

            combinedFilters
                    .append(videoFilter.getFilter());

            if (i < videoFilters.size() - 1) {
                combinedFilters.append(",");
            }
        }

        if (combinedFilters.length() == 0)
            return null;

        System.out.println(combinedFilters);
        final FFmpegFrameFilter videoFilter = new FFmpegFrameFilter(combinedFilters.toString(), editInfo.getImageWidth(), editInfo.getImageHeight());
        videoFilter.setPixelFormat(editInfo.getPixelFormat());
        videoFilter.setFrameRate(editInfo.getFrameRate());
        videoFilter.setVideoInputs(1);
        videoFilter.start();

        return videoFilter;
    }
}