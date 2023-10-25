package de.yugata.easy.edits.editor.video;


import de.yugata.easy.edits.editor.Editor;
import de.yugata.easy.edits.editor.edit.EditInfo;
import de.yugata.easy.edits.editor.edit.EditInfoBuilder;
import de.yugata.easy.edits.editor.edit.EditingFlag;
import de.yugata.easy.edits.filter.*;
import de.yugata.easy.edits.util.FFmpegUtil;
import org.bytedeco.javacv.*;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_VERBOSE;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;

/**
 * TODO: This needs documentation & a cleanup
 */
public class VideoEditor implements Editor {

    private final List<VideoClip> videoClips;


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
    private final String overlayAudioPath;

    private final long introStart;
    private final long introEnd;

    private final EnumSet<EditingFlag> editingFlags;

    private final List<FilterWrapper> filters;

    private final File outputFile, workingDirectory;

    private final File segmentAudioFile;

    public VideoEditor(final String videoPath,
                       final String overlayAudioPath,
                       final File outputFile,
                       final List<VideoClip> videoClips,
                       final EnumSet<EditingFlag> flags,
                       final List<FilterWrapper> filters,
                       final long introStart,
                       final long introEnd,
                       final File workingDirectory) {

        this.videoPath = videoPath;
        this.overlayAudioPath = overlayAudioPath;
        this.videoClips = videoClips;
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

        this.segmentAudioFile = new File(workingDirectory, UUID.randomUUID() + ".wav");

        if (flags.contains(EditingFlag.PRINT_DEBUG)) {
            FFmpegLogCallback.set();
            av_log_set_level(AV_LOG_VERBOSE);
        }
    }


    /**
     * Initializes & configure the frame grabber for the input video.
     */
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
                videoGrabber.restart();
            } catch (FrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * Frees the resource allocated by the frame grabber
     */
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

    /**
     * Collects the segments creates by the video editor.
     *
     * @param useSegments whether to rewrite the segments or just use the old ones.
     * @return a list with all the files pointing to the segments in order.
     */
    private List<File> collectSegments(final boolean useSegments) {
        if (useSegments) {
            return Arrays.stream(workingDirectory.listFiles())
                    .filter(file -> file.getName().startsWith("segment"))
                    .sorted(Comparator.comparingInt(value -> Integer.parseInt(value.getName().substring("segment ".length(), value.getName().lastIndexOf(".")))))
                    .collect(Collectors.toList());

        } else {
            return writeSegments();
        }
    }

    public void edit(final boolean useSegments) {
        // Write the segment files that will be stitched together.
        final List<File> segments = collectSegments(useSegments);
        final List<String> segmentPaths = segments.stream().map(File::getAbsolutePath).collect(Collectors.toList());

        this.initFrameGrabber();

        try {
            // Grabs the audio frames from the supplied audio
            final FFmpegFrameGrabber overlayAudioGrabber = new FFmpegFrameGrabber(overlayAudioPath);
            overlayAudioGrabber.start();

            final FFmpegFrameGrabber segmentAudioGrabber = new FFmpegFrameGrabber(segmentAudioFile);
            segmentAudioGrabber.start();

            // Configure the recorder
            final FFmpegFrameRecorder recorder = FFmpegUtil.createRecorder(outputFile, editingFlags, videoGrabber);
            recorder.setAudioCodec(segmentAudioGrabber.getAudioCodec());
            recorder.setSampleRate(segmentAudioGrabber.getSampleRate());
            recorder.setSampleFormat(segmentAudioGrabber.getSampleFormat());
            recorder.start();


            // Edit: I fucking hate this, we just pass the frame grabber in the fucking future...
            final EditInfo editInfo = new EditInfoBuilder()
                    .setEditTime(getEditLength(segmentPaths))
                    .setAudioCodec(overlayAudioGrabber.getAudioCodec())
                    .setAspectRatio(videoGrabber.getAspectRatio())
                    .setAudioChannels(overlayAudioGrabber.getAudioChannels())
                    .setAudioBitrate(overlayAudioGrabber.getAudioBitrate())
                    .setFrameRate(videoGrabber.getFrameRate())
                    .setImageHeight(videoGrabber.getImageHeight())
                    .setImageWidth(videoGrabber.getImageWidth())
                    .setAudioCodecName(overlayAudioGrabber.getAudioCodecName())
                    .setSampleFormat(overlayAudioGrabber.getSampleFormat())
                    .setVideoBitrate(videoGrabber.getVideoBitrate())
                    .setImageScalingFlags(videoGrabber.getImageScalingFlags())
                    .setSampleRate(overlayAudioGrabber.getSampleRate())
                    .setVideoCodec(videoGrabber.getVideoCodec())
                    .setVideoCodecName(videoGrabber.getVideoCodecName())
                    .setPixelFormat(recorder.getPixelFormat())
                    .createEditInfo();


            // Populate the filters
            FilterManager.FILTER_MANAGER.populateFilters(filters, editInfo);


            // optional: audio filter with chained  audio filters.
            final FFmpegFrameFilter simpleAudioFiler = simpleAudioFilter(recorder);

            // Audio filter which overlays audio and input audio if there is input audio given.
            final FFmpegFrameFilter overlayFilter = overlayAudioFilter(recorder);

            // Filter to convert the supplied audio into the same format & sample rate as the recorder.
            final FFmpegFrameFilter convertAudioFilter = convertAudioFilter(recorder);

            // optional: Frame filter with chained together video filters
            final FFmpegFrameFilter simpleVideoFiler = FFmpegUtil.populateVideoFilters(editInfo);


            for (final String inputPath : segmentPaths) {
                // grabber for the individual clip segment
                final FFmpegFrameGrabber segmentGrabber = baseSegmentGrabber(new File(inputPath), recorder, "h265_cuvid");

                // Optional: Populate the transition filters, we have to reconfigure them every time, as the offsets depend on it.
                final FFmpegFrameFilter transitionFilter = FFmpegUtil.populateTransitionFilters(editInfo);

                // Add the filters to a chain.
                final FFmpegFrameFilter[] filters = simpleVideoFiler == null ? new FFmpegFrameFilter[0] : transitionFilter == null ? new FFmpegFrameFilter[]{simpleVideoFiler} : new FFmpegFrameFilter[]{transitionFilter, simpleVideoFiler};

                /* Workflow */
                //  1. Record the segments video frames.
                //  2. Record the audio frames in a separate loop

                // grab the video frames & send them to the filters
                Frame frame;
                while ((frame = segmentGrabber.grabImage()) != null) {
                    FFmpegUtil.pushToFilters(frame, recorder, filters);
                }

                // Close the transition filter, free the resources
                if (transitionFilter != null)
                    transitionFilter.close();

                segmentGrabber.close();
            }

            /* End samples loop */


            Frame segmentAudio;
            while ((segmentAudio = segmentAudioGrabber.grabSamples()) != null) {
                // Push the background audio to [0], no volume decrease
                overlayFilter.push(0, segmentAudio);
            }

            Frame audioFrame;
            while ((audioFrame = overlayAudioGrabber.grabSamples()) != null) {
                // process the audio frame, convert the audio frame to our sample-rate, sample-format
                convertAudioFilter.push(audioFrame);

                Frame convertAudioFrame;
                if ((convertAudioFrame = convertAudioFilter.pull()) != null) {
                    FFmpegUtil.pushToFilterOrElse(convertAudioFrame, simpleAudioFiler, f -> {
                        try {
                            overlayFilter.push(1, f);
                        } catch (FFmpegFrameFilter.Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }

            // Pull from the overlay filter & record.
            Frame overlayFrame;
            while ((overlayFrame = overlayFilter.pull()) != null) {
                // Set the timestamp in the recorder.
                recorder.record(overlayFrame);
            }


            /* End audio grabbing */

            if (simpleVideoFiler != null)
                simpleVideoFiler.close();

            if (simpleAudioFiler != null)
                simpleAudioFiler.close();


            overlayFilter.close();
            overlayAudioGrabber.close();
            segmentAudioGrabber.close();

            recorder.close();
        } catch (FrameRecorder.Exception | FrameGrabber.Exception | FrameFilter.Exception e) {
            e.printStackTrace();
        } finally {
            this.releaseFrameGrabber();
        }
    }


    public List<File> writeSegments() {
        this.initFrameGrabber();

        // List of all the segment files in order.
        final List<File> segmentFiles = new ArrayList<>();

        // Shuffle the sequences if the flag is toggled.
        if (editingFlags.contains(EditingFlag.SHUFFLE_SEQUENCES)) {
            Collections.shuffle(videoClips);
        }
        try {

            final FFmpegFrameRecorder audioRecorder = new FFmpegFrameRecorder(segmentAudioFile, 2);
            audioRecorder.setAudioOption("ac", "2"); // Downsample the 5.1 to stereo
            audioRecorder.setSampleRate(videoGrabber.getSampleRate()); // Sample rate from the audio source
            audioRecorder.setSampleFormat(videoGrabber.getSampleFormat());
            audioRecorder.setAudioBitrate(videoGrabber.getAudioBitrate());
            audioRecorder.setFrameRate(videoGrabber.getAudioFrameRate());
            audioRecorder.start();

            /* Record the intro */

            if (introStart != -1 && introEnd != -1) {
                final File introFile = new File(workingDirectory, "segment 0.mp4");
                final FFmpegFrameRecorder introRecorder = FFmpegUtil.createRecorder(introFile, editingFlags, videoGrabber);
                introRecorder.start();

                this.videoGrabber.setTimestamp(introStart);
                // grab from the intro
                Frame introFrame;
                while (videoGrabber.getTimestamp() < introEnd && (introFrame = videoGrabber.grab()) != null) {
                    introRecorder.record(introFrame);
                }

                introRecorder.close();
                segmentFiles.add(introFile);
            }

            /* Record the video clips */

            for (int i = 0; i < videoClips.size(); i++) {
                final VideoClip videoClip = videoClips.get(i);

                // Navigate to the clip.
                videoGrabber.setVideoTimestamp(videoClip.getTimeStamp());

                // Write a new segment to disk
                final File segmentFile = new File(workingDirectory, String.format("segment %d.mp4", introStart == -1 ? i : i + 1));
                segmentFiles.add(segmentFile); // Add the file to the segments.

                final FFmpegFrameRecorder recorder = FFmpegUtil.createRecorder(segmentFile, editingFlags, videoGrabber);
                recorder.start();

                final FFmpegFrameFilter[] filters;
                // Filters that might be applied if the flag is enabled.
                if (editingFlags.contains(EditingFlag.PROCESS_SEGMENTS)) {
                    // TODO: Include audio filters
                    final List<Filter> exportVideo = FilterManager.FILTER_MANAGER.getFilters(filter -> filter.getFilterRange() == FilterRange.EXPORT && filter.getFilterType() == FilterType.VIDEO);

                    // I hate this.
                    final EditInfo editInfo = new EditInfoBuilder()
                            .setAspectRatio(videoGrabber.getAspectRatio())
                            .setFrameRate(videoGrabber.getFrameRate())
                            .setImageHeight(videoGrabber.getImageHeight())
                            .setImageWidth(videoGrabber.getImageWidth())
                            .setVideoBitrate(videoGrabber.getVideoBitrate())
                            .setImageScalingFlags(videoGrabber.getImageScalingFlags())
                            .setVideoCodec(videoGrabber.getVideoCodec())
                            .setVideoCodecName(videoGrabber.getVideoCodecName())
                            .setPixelFormat(recorder.getPixelFormat())
                            .setIntroStart(introStart)
                            .setIntroEnd(introEnd)
                            .createEditInfo();

                    filters = new FFmpegFrameFilter[]{FFmpegUtil.populateVideoFilters(exportVideo, editInfo)};
                } else {
                    filters = new FFmpegFrameFilter[0];
                }

                Frame frame;
                final long frameLenience = 4;
                while (videoGrabber.getTimestamp() - videoClip.getTimeStamp() <= videoClip.getLength() + frameLenience && (frame = videoGrabber.grab()) != null) {
                    if (frame.getTypes().contains(Frame.Type.VIDEO)) {
                        FFmpegUtil.pushToFilters(frame, recorder, filters);
                    } else if (frame.getTypes().contains(Frame.Type.AUDIO)) {
                        // Record null samples.
                        if (videoClip.isMuteAudio()) {
                            final FloatBuffer silence = FloatBuffer.allocate((int) ((audioRecorder.getAudioChannels() * audioRecorder.getSampleRate()) / audioRecorder.getFrameRate()));
                            audioRecorder.recordSamples(silence);
                            silence.flip();
                            silence.clear();
                        } else {
                            audioRecorder.record(frame);
                        }
                    }
                }

                // Close the filter(s) if there are any.
                for (final FFmpegFrameFilter filter : filters) {
                    filter.close();
                }

                // Close our local recorder.
                recorder.close();
            }
            /* End video clip loop */

            audioRecorder.close();
            this.releaseFrameGrabber();

        } catch (IOException e) {
            e.printStackTrace();

        }
        return segmentFiles;
    }
}