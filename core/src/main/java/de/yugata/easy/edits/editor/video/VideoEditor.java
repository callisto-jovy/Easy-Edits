package de.yugata.easy.edits.editor.video;


import de.yugata.easy.edits.editor.edit.EditingFlag;
import de.yugata.easy.edits.editor.edit.EditInfo;
import de.yugata.easy.edits.editor.edit.EditInfoBuilder;
import de.yugata.easy.edits.filter.*;
import de.yugata.easy.edits.filter.FilterWrapper;
import de.yugata.easy.edits.util.FFmpegUtil;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_VERBOSE;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;

/**
 * TODO: This needs documentation & a cleanup
 */
public class VideoEditor {

    private final Queue<Double> timeBetweenBeats;
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
                       final List<VideoClip> videoClips,
                       final EnumSet<EditingFlag> flags,
                       final List<FilterWrapper> filters,
                       final long introStart,
                       final long introEnd,
                       final File workingDirectory) {

        this.videoPath = videoPath;
        this.audioPath = audioPath;
        this.timeBetweenBeats = timeBetweenBeats;
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
                videoGrabber.start();
            } catch (FFmpegFrameGrabber.Exception e) {
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
        try (final FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(audioPath)) {

            final FFmpegFrameRecorder recorder = FFmpegUtil.createRecorder(outputFile, editingFlags, videoGrabber);
            recorder.start();

            audioGrabber.setSampleRate(recorder.getSampleRate());
            audioGrabber.setSampleFormat(recorder.getSampleFormat());
            audioGrabber.start();

            // Edit: I fucking hate this, we just pass the frame grabber in the fucking future...
            final EditInfo editInfo = new EditInfoBuilder()
                    .setEditTime(audioGrabber.getLengthInTime())
                    .setAudioCodec(audioGrabber.getAudioCodec())
                    .setAspectRatio(videoGrabber.getAspectRatio())
                    .setAudioChannels(audioGrabber.getAudioChannels())
                    .setAudioBitrate(audioGrabber.getAudioBitrate())
                    .setFrameRate(videoGrabber.getFrameRate())
                    .setImageHeight(videoGrabber.getImageHeight())
                    .setImageWidth(videoGrabber.getImageWidth())
                    .setAudioCodecName(audioGrabber.getAudioCodecName())
                    .setSampleFormat(audioGrabber.getSampleFormat())
                    .setVideoBitrate(videoGrabber.getVideoBitrate())
                    .setImageScalingFlags(videoGrabber.getImageScalingFlags())
                    .setSampleRate(audioGrabber.getSampleRate())
                    .setVideoCodec(videoGrabber.getVideoCodec())
                    .setVideoCodecName(videoGrabber.getVideoCodecName())
                    .setPixelFormat(recorder.getPixelFormat())
                    .setIntroStart(introStart)
                    .setIntroEnd(introEnd)
                    .createEditInfo();


            // Populate the filters
            FilterManager.FILTER_MANAGER.populateFilters(filters, editInfo);


            final FFmpegFrameFilter simpleAudioFiler = FFmpegUtil.populateAudioFilters();
            if (simpleAudioFiler != null) {
                simpleAudioFiler.setSampleRate(recorder.getSampleRate());
                simpleAudioFiler.setSampleFormat(recorder.getSampleFormat());
                simpleAudioFiler.start();
            }

            final FFmpegFrameFilter overlayFilter = FFmpegUtil.configureAudioFilter("[0:a]volume=0.25[a1]; [a1][1:a]amerge=inputs=2[a]", recorder.getSampleRate(), recorder.getSampleFormat());
            overlayFilter.start();


            final BytePointer sampleFormatName = avutil.av_get_sample_fmt_name(recorder.getSampleFormat());
            final FFmpegFrameFilter convertAudioFilter = FFmpegUtil.configureAudioFilter(String.format("aformat=sample_fmts=%s:sample_rates=%d", sampleFormatName.getString(), recorder.getSampleRate()), recorder.getSampleRate(), recorder.getSampleFormat());
            convertAudioFilter.start();

            sampleFormatName.close(); // release reference


            // Configure the simple video filters.
            final FFmpegFrameFilter simpleVideoFiler = FFmpegUtil.populateVideoFilters(editInfo);

            /* Writing the segments to the main file, apply filters */

            for (final File segment : segments) {
                // TODO: Move to util maybe

                final FFmpegFrameGrabber segmentGrabber = new FFmpegFrameGrabber(segment);
                FFmpegUtil.configureGrabber(segmentGrabber);
                segmentGrabber.setSampleFormat(recorder.getSampleFormat());
                segmentGrabber.setSampleRate(recorder.getSampleRate());
                segmentGrabber.setVideoCodecName("h265_cuvid"); // HW-Accelerated grabbing
                segmentGrabber.setPixelFormat(recorder.getPixelFormat());
                segmentGrabber.start();


                // Populate the transition filters, we have to reconfigure them every time, as the offsets depend on it.
                final FFmpegFrameFilter transitionFilter = FFmpegUtil.populateTransitionFilters(editInfo);

                // Add the filters to a chain.
                final FFmpegFrameFilter[] filters;

                if (simpleVideoFiler == null) {
                    filters = new FFmpegFrameFilter[]{};
                } else {
                    filters = transitionFilter == null ? new FFmpegFrameFilter[]{simpleVideoFiler} : new FFmpegFrameFilter[]{transitionFilter, simpleVideoFiler};
                }

                // grab the frames & send them to the filters
                Frame frame;
                while ((frame = segmentGrabber.grab()) != null) {


                    Frame audioFrame;
                    // if there's no audio, we just record.
                    if (!segmentGrabber.hasAudio() && (audioFrame = audioGrabber.grab()) != null) {
                        recorder.record(audioFrame);
                    }

                    // just record the video, skip the audio part, there is none.
                    if (frame.getTypes().contains(Frame.Type.VIDEO)) {
                        FFmpegUtil.pushToFilters(frame, recorder, filters);
                        continue;
                    }

                    // if there's no audio, no audio will be in the edit.
                    if ((audioFrame = audioGrabber.grab()) != null) {

                        // overlay background audio.
                        if (segmentGrabber.hasAudio()) {

                            // Do audio processing
                            try {
                                overlayFilter.push(1, frame);
                            } catch (FFmpegFrameFilter.Exception e) {
                                throw new RuntimeException(e);
                            }


                            // process the audio frame
                            convertAudioFilter.push(audioFrame);

                            Frame convertAudioFrame;
                            if ((convertAudioFrame = convertAudioFilter.pull()) != null) {

                                FFmpegUtil.pushToFilterOrElse(convertAudioFrame, simpleAudioFiler, f -> {
                                    try {
                                        overlayFilter.push(0, convertAudioFrame);
                                    } catch (FFmpegFrameFilter.Exception e) {
                                        throw new RuntimeException(e);
                                    }

                                });

                            }


                            // Pull from the overlay filter & record.
                            Frame overlayFrame;
                            while ((overlayFrame = overlayFilter.pull()) != null) {
                                // Set the timestamp in the recorder.
                                recorder.record(overlayFrame);
                            }

                        } else {
                            // Just record the audio
                            recorder.record(audioFrame);
                        }
                    }
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
                simpleVideoFiler.close();

            if (simpleAudioFiler != null)
                simpleAudioFiler.close();

            overlayFilter.close();
            recorder.close();
            ///////////////
        } catch (FrameRecorder.Exception | FrameGrabber.Exception | FrameFilter.Exception e) {
            e.printStackTrace();
            // delete file.
            outputFile.delete();
        } finally {
            this.releaseFrameGrabber();
        }
    }


    public List<File> writeSegments() {
        this.initFrameGrabber();

        // List of all the segment files in order.
        final List<File> segmentFiles = new ArrayList<>();


        // The videos framerate
        final double frameRate = videoGrabber.getFrameRate();
        // The time one frame takes in ms.
        final double frameTime = 1000 / frameRate;

        // Shuffle the sequences if the flag is toggled.
        if (editingFlags.contains(EditingFlag.SHUFFLE_SEQUENCES)) {
            Collections.shuffle(videoClips);
        }

        try {

            /* Record the intro */

            if (introStart != -1 && introEnd != -1) {
                final File introFile = new File(workingDirectory, "segment 0.mp4");
                final FFmpegFrameRecorder introRecorder = FFmpegUtil.createRecorder(introFile, editingFlags, videoGrabber);
                introRecorder.start();

                this.videoGrabber.setTimestamp(introStart);
                // grab from the intro
                Frame introFrame;
                while ((introFrame = videoGrabber.grab()) != null && videoGrabber.getTimestamp() < introEnd) {
                    introRecorder.record(introFrame);
                }

                introRecorder.close();
                segmentFiles.add(introFile);
            }


            int nextStamp = 0;

            boolean muteAudio = true;

            /* Beat loop */
            while (timeBetweenBeats.peek() != null) {
                double timeBetween = timeBetweenBeats.poll();

                // If the next stamp (index in the list) is valid, we move to the timestamp.
                // If not, we just keep recording, until no beat times are left
                // This is nice to have for ending sequences, where a last sequence is displayed for x seconds.
                if (nextStamp < videoClips.size()) {
                    final VideoClip videoClip = videoClips.get(nextStamp);
                    muteAudio = videoClip.isMuteAudio();

                    final long timeStamp = videoClip.getTimeStamp();
                    videoGrabber.setTimestamp(timeStamp);
                }

                // Write a new segment to disk
                final File segmentFile = new File(workingDirectory, String.format("segment %d.mp4", introStart == -1 ? nextStamp : nextStamp + 1));
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

                // Time passed in frame times.
                double localMs = 0;

                // Pick frames till the interim is filled...
                Frame frame;
                while ((frame = muteAudio ? videoGrabber.grabImage() : videoGrabber.grab()) != null && localMs < timeBetween) {
                    FFmpegUtil.pushToFilters(frame, recorder, filters);
                    localMs += frameTime;
                }

                // Close the filter(s) if there are any.
                for (final FFmpegFrameFilter filter : filters) {
                    filter.close();
                }

                // Close our local recorder.
                recorder.close();
                // Advance to the next timestamp.
                nextStamp++;
            }
            /* End beat loop */

            this.releaseFrameGrabber();

        } catch (IOException e) {
            e.printStackTrace();

        }
        return segmentFiles;
    }
}