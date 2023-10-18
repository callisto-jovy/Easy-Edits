package de.yugata.easy.edits.editor;


import de.yugata.easy.edits.editor.filter.FilterManager;
import de.yugata.easy.edits.editor.filter.FilterWrapper;
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
    private FFmpegFrameGrabber videoGrabber;

    /**
     * The video's input path
     */
    private final String videoPath;

    /**
     * The audio file's path
     */
    private final String audioPath;

    private final long introStart;
    private final long introEnd;

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
                       final File workingDirectory) {

        this.videoPath = videoPath;
        this.audioPath = audioPath;
        this.timeBetweenBeats = timeBetweenBeats;
        this.videoTimeStamps = videoTimeStamps;
        this.editingFlags = flags;
        this.introStart = introStart;
        this.introEnd = introEnd;
        this.filters = filters;
        this.workingDirectory = workingDirectory;


        if (!workingDirectory.exists())
            workingDirectory.mkdir();

        if (outputFile.exists()) {
            this.outputFile = new File(workingDirectory.getParent(), UUID.randomUUID() + outputFile.getName());
        } else {
            this.outputFile = outputFile;
        }


        if (flags.contains(EditingFlag.PRINT_DEBUG)) {
            FFmpegLogCallback.set();
            av_log_set_level(AV_LOG_VERBOSE);
        }
    }


    private void initFrameGrabber() {
        if (videoGrabber == null) {
            try {
                this.videoGrabber = new FFmpegFrameGrabber(videoPath);
                FFmpegUtil.configureGrabber(videoGrabber);

                videoGrabber.setVideoCodecName("hevc_cuvid");
                videoGrabber.start();

            } catch (FFmpegFrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                videoGrabber.start();
            } catch (FFmpegFrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void releaseFrameGrabber() {
        if (videoGrabber != null) {
            try {
                videoGrabber.close();
                this.videoGrabber = null;
            } catch (FrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<File> collectSegments(final boolean useSegments) {
        if (useSegments) {
            return Arrays.stream(workingDirectory.listFiles())
                    .sorted(Comparator.comparingInt(value -> Integer.parseInt(value.getName().substring("segment ".length(), value.getName().lastIndexOf(".")))))
                    .collect(Collectors.toList());

        } else {
            return writeSegments();
        }
    }


    public void edit(final boolean useSegments) {
        // Write the segment files that will be stitched together.
        final List<File> segments = collectSegments(useSegments);

        this.initFrameGrabber();

        // Recorder for the final product & audio grabber to overlay the audio
        try (final FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(audioPath);
             final FFmpegFrameRecorder recorder = FFmpegUtil.createRecorder(outputFile, editingFlags, videoGrabber)) {

            // Start grabbing the audio, we need this for the sample rate.
            audioGrabber.start();
            recorder.start();


            /* Add the intro in front */

            if (introStart != -1 && introEnd != -1) {
                this.videoGrabber.setTimestamp(introStart);

                Frame introFrame;
                while ((introFrame = videoGrabber.grab()) != null && videoGrabber.getTimestamp() < introEnd) {
                    recorder.record(introFrame);
                }
            }

            /* End intro */


            // Edit: I fucking hate this, we just pass the frame grabber in the fucking future...
            final EditInfo editInfo = new EditInfoBuilder()
                    .setEditTime(audioGrabber.getLengthInTime())
                    .setAudioCodec(audioGrabber.getAudioCodec())
                    .setAspectRatio(videoGrabber.getAspectRatio())
                    .setAudioChannels(audioGrabber.getAudioChannels())
                    .setAudioBitrate(audioGrabber.getAudioBitrate())
                    .setAudioMetadata(audioGrabber.getAudioMetadata())
                    .setAudioOptions(audioGrabber.getAudioOptions())
                    .setBpp(videoGrabber.getBitsPerPixel())
                    .setDeinterlace(videoGrabber.isDeinterlace())
                    .setAudioSideData(audioGrabber.getAudioSideData())
                    .setFrameRate(videoGrabber.getFrameRate())
                    .setGamma(videoGrabber.getGamma())
                    .setImageHeight(videoGrabber.getImageHeight())
                    .setImageWidth(videoGrabber.getImageWidth())
                    .setAudioCodecName(audioGrabber.getAudioCodecName())
                    .setMetadata(videoGrabber.getMetadata())
                    .setOptions(videoGrabber.getOptions())
                    .setSampleFormat(audioGrabber.getSampleFormat())
                    .setVideoBitrate(videoGrabber.getVideoBitrate())
                    .setImageScalingFlags(videoGrabber.getImageScalingFlags())
                    .setSampleRate(audioGrabber.getSampleRate())
                    .setVideoCodec(videoGrabber.getVideoCodec())
                    .setVideoCodecName(videoGrabber.getVideoCodecName())
                    .setVideoMetadata(videoGrabber.getVideoMetadata())
                    .setVideoOptions(videoGrabber.getVideoOptions())
                    .setVideoSideData(videoGrabber.getVideoSideData())
                    .setPixelFormat(recorder.getPixelFormat())
                    .setIntroStart(introStart)
                    .setIntroEnd(introEnd)
                    .createEditInfo();


            // Populate the filters
            FilterManager.FILTER_MANAGER.populateFilters(filters, editInfo);


            // Configure the simple video filters.
            final FFmpegFrameFilter simpleVideoFiler = FFmpegUtil.populateVideoFilters(editInfo);

            /* Writing the segments to the main file, apply filters */

            for (final File segment : segments) {
                // TODO: Move to util maybe
                final FFmpegFrameGrabber segmentGrabber = new FFmpegFrameGrabber(segment);
                FFmpegUtil.configureGrabber(segmentGrabber);
                segmentGrabber.setVideoCodecName("hevc_cuvid"); // HW-Accelerated grabbing
                segmentGrabber.setPixelFormat(recorder.getPixelFormat()); // Ensure that the pixel format is right.
                segmentGrabber.start();

                // Populate the transition filters, we have to reconfigure them every time, as the offsets depend on it.
                final FFmpegFrameFilter transitionFilter = FFmpegUtil.populateTransitionFilters(editInfo);

                // Add the filters to a chain.
                // TODO: Add to a ffmpeg chain?? Join the filters with a semicolon
                final FFmpegFrameFilter[] filters;

                if (simpleVideoFiler == null) {
                    filters = new FFmpegFrameFilter[]{};
                } else {
                    filters = transitionFilter == null ? new FFmpegFrameFilter[]{simpleVideoFiler} : new FFmpegFrameFilter[]{transitionFilter, simpleVideoFiler};
                }

                // grab the frames & send them to the filters
                Frame videoFrame;
                while ((videoFrame = segmentGrabber.grabImage()) != null) {
                    FFmpegUtil.pushToFilters(videoFrame, recorder, filters);
                }

                // Close the transition filter, free the resources
                if (transitionFilter != null)
                    transitionFilter.close();

                // Close the grabber, release the resources
                segmentGrabber.close();
            }

            /* End recording video */

            /* Clean up resources */
            if (simpleVideoFiler != null)
                simpleVideoFiler.stop();

            // Record the audio
            this.recordAudio(audioGrabber, recorder);
            ///////////////
        } catch (FrameRecorder.Exception | FrameGrabber.Exception | FrameFilter.Exception e) {
            throw new RuntimeException(e);
        }
        this.releaseFrameGrabber();
    }

    private void recordAudio(final FFmpegFrameGrabber audioGrabber, final FFmpegFrameRecorder recorder) throws FFmpegFrameFilter.Exception, FFmpegFrameGrabber.Exception, FFmpegFrameRecorder.Exception {
        final FFmpegFrameFilter simpleAudioFiler = FFmpegUtil.populateAudioFilters();

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
        final double frameRate = videoGrabber.getFrameRate();
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
                    videoGrabber.setTimestamp((long) timeStamp);
                }

                // Write a new segment to disk
                final File segmentFile = new File(workingDirectory, String.format("segment %d.mp4", nextStamp));
                segmentFiles.add(segmentFile); // Add the file to the segments.
                final FFmpegFrameRecorder recorder = FFmpegUtil.createRecorder(segmentFile, editingFlags, videoGrabber);
                //        recorder.setPixelFormat(frameGrabber.getPixelFormat());
                //    recorder.setPixelFormat(frameGrabber.getPixelFormat());
                //   recorder.setVideoCodec(AV_CODEC_ID_H265);

                recorder.start();


                // Time passed in frame times.
                double localMs = 0;

                // Pick frames till the interim is filled...
                Frame frame;
                while ((frame = videoGrabber.grabImage()) != null && localMs < timeBetween) {

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


}