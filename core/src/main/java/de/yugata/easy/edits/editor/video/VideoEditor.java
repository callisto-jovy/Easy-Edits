package de.yugata.easy.edits.editor.video;


import de.yugata.easy.edits.editor.BasicEditor;
import de.yugata.easy.edits.editor.BasicEditorBuilder;
import de.yugata.easy.edits.editor.Editor;
import de.yugata.easy.edits.editor.edit.EditInfo;
import de.yugata.easy.edits.editor.edit.EditInfoBuilder;
import de.yugata.easy.edits.editor.edit.EditingFlag;
import de.yugata.easy.edits.filter.*;
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
public class VideoEditor implements Editor {

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

        this.initFrameGrabber();

        try {
            // Configure the recorder
            final FFmpegFrameRecorder recorder = FFmpegUtil.createRecorder(outputFile, editingFlags, videoGrabber);
            recorder.start();


            final BasicEditor basicEditor = new BasicEditorBuilder()
                    .setAudioPath(audioPath)
                    .setEditingFlags(editingFlags)
                    .setFilters(filters)
                    .setRecorder(recorder)
                    .setVideoGrabber(videoGrabber)
                    .createBasicEditor();

            basicEditor.editFootage(segments.stream().map(File::getAbsolutePath).collect(Collectors.toList()), "h265_cuvid");

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

            //FIXME: one training file is created which has no audio, nor video..

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

            for (final VideoClip videoClip : videoClips) {
                // Navigate to the clip.
                videoGrabber.setTimestamp(videoClip.getTimeStamp());

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


                Frame frame;
                while (videoGrabber.getTimestamp() - videoClip.getTimeStamp() < videoClip.getLength() && (frame = videoGrabber.grab()) != null) {
                    FFmpegUtil.pushToFilters(frame, recorder, filters);
                }

                // Close the filter(s) if there are any.
                for (final FFmpegFrameFilter filter : filters) {
                    filter.close();
                }

                // Close our local recorder.
                recorder.close();
            }

            /* End beat loop */
            this.releaseFrameGrabber();

        } catch (IOException e) {
            e.printStackTrace();

        }
        return segmentFiles;
    }
}