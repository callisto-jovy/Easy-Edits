package de.yugata.easy.edits.editor;


import com.github.kokorin.jaffree.LogLevel;
import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.*;
import de.yugata.easy.edits.editor.filter.Filter;
import de.yugata.easy.edits.editor.filter.FilterManager;
import de.yugata.easy.edits.editor.filter.FilterWrapper;
import de.yugata.easy.edits.util.AudioUtil;
import de.yugata.easy.edits.util.FFmpegUtil;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static de.yugata.easy.edits.util.FFmpegUtil.FFMPEG_BIN;
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

    private final File outputFile, workingDirectory, outputDirectory;


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

        this.outputDirectory = new File(workingDirectory.getParent(), "output");

        if (!outputDirectory.exists())
            outputDirectory.mkdirs();


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


    public List<File> collectSegments() {
        return Arrays.stream(Objects.requireNonNull(workingDirectory.listFiles()))
                .sorted(Comparator.comparingInt(value -> Integer.parseInt(value.getName().substring("segment ".length(), value.getName().lastIndexOf(".")))))
                .collect(Collectors.toList());
    }

    public List<File> collectProcessedSegments() {
        return Arrays.stream(Objects.requireNonNull(outputDirectory.listFiles()))
                .sorted(Comparator.comparingInt(value -> Integer.parseInt(value.getName().substring("segment ".length(), value.getName().lastIndexOf(".")))))
                .collect(Collectors.toList());
    }


    public List<File> processSegments(final List<File> segments) {
        final List<File> files = new ArrayList<>();

        for (final File segment : segments) {
            final FFmpeg builder = FFmpeg.atPath(FFMPEG_BIN.toPath());

            final File outputFile = new File(outputDirectory, segment.getName());

            builder.addInput(UrlInput.fromPath(segment.toPath()))       // Segment is the input
                    .addOutput(UrlOutput.toPath(outputFile.toPath())) // Output segment in output dir
                    .setLogLevel(LogLevel.INFO)
                    .addArguments("-c:v", "libx265")
                    .setOverwriteOutput(true)
                    .setOutputListener(System.out::println);


            // Add filters

            // TODO: new data class
            final EditInfo editInfo = new EditInfoBuilder()
                    .setEditTime(AudioUtil.estimateEditLengthInMicros(audioPath))
                    .setIntroStart(introStart)
                    .setIntroEnd(introEnd)
                    .createEditInfo();

            // Populate the filters
            FilterManager.FILTER_MANAGER.populateFilters(filters, editInfo);

            /* Configure the  filters. */
            final String videoFilter = FFmpegUtil.chainVideoFilters();
            final String audioFilter = FFmpegUtil.chainAudioFilters();

            builder.setFilter(StreamType.AUDIO, audioFilter);
            builder.setFilter(StreamType.VIDEO, videoFilter);

            // Execute new command
            builder.execute();

            files.add(outputFile);
        }
        return files;
    }


    public void concatSegments(final List<File> segments) {

        // TODO: re add intro support, just export the intro segments & stich them in the front
        final FFmpeg builder = FFmpeg.atPath(FFMPEG_BIN.toPath());

        // Add input(s)
        for (final File segment : segments) {
            builder.addInput(UrlInput.fromPath(segment.toPath()));
        }

        // just add the output here
        builder.addOutput(UrlOutput.toPath(outputFile.toPath()))
                .addInput(UrlInput.fromUrl(audioPath)) // The audio input is the last input added (segments.size())
                .setLogLevel(LogLevel.INFO)
                .addArguments("-c:v", "libx265")
                .setOutputListener(System.out::println);

/*
        // Add intro offset if requested
        if (introStart != -1 && introEnd != -1) {
            // offset the audio timestamp by the intro start, so that the intro keeps its original audio if that's requested.
            // this then can be paired with a slow fade in of x seconds for the intro
            if (editingFlags.contains(EditingFlag.OFFSET_AUDIO_FOR_INTRO)) {
                final long introLength = TimeUnit.MICROSECONDS.toMillis(introEnd - introStart);
                builder.addArguments("-itsoffset", introLength + "ms")
                        .addArgument("-async");
            }

            //TODO Intro
        }
 */

        // Todo: maybe complex filters can be passed here.

        final EditInfo editInfo = new EditInfoBuilder()
                .setEditTime(AudioUtil.estimateEditLengthInMicros(audioPath))
                .setIntroStart(introStart)
                .setIntroEnd(introEnd)
                .createEditInfo();

        FilterManager.FILTER_MANAGER.populateFilters(filters, editInfo);

        final List<FilterChain> filterChains = new ArrayList<>();

        for (final Filter complexVideoFilter : FilterManager.FILTER_MANAGER.getComplexVideoFilters()) {
            filterChains.add(FilterChain.of());
        }
        /*

        // Chain filters

        // split filter
        for (final Filter complexVideoFilter : FilterManager.FILTER_MANAGER.getComplexVideoFilters()) {
            // The chain to complete

            final FilterChain filterChain = new FilterChain();
            // the complex filter to parse
            final String filter = complexVideoFilter.getFilter();

            final String[] filters = filter.split(","); // split filter chain

            // parse the string
            if (filters.length == 0) {
                final GenericFilter genericFilter = FFmpegUtil.parseGenericFilter(filter);
                filterChain.addFilter(genericFilter);
            } else {
                for (final String f : filters) {
                    final GenericFilter genericFilter = FFmpegUtil.parseGenericFilter(f);
                    filterChain.addFilter(genericFilter);
                }
            }

            // parse the multiple strings
            filterChains.add(filterChain);
        }

         */

        filterChains.add(FilterChain.of(
                        com.github.kokorin.jaffree.ffmpeg.Filter
                                .fromInputLink(StreamType.VIDEO)
                                .setName("concat")
                                .addArgument("n", String.valueOf(segments.size()))
                                .addArgument("v", "1")
                                .addArgument("a", "0")
                                .addOutputLink("v")
                )
        );


        builder.setComplexFilter(FilterGraph.of(filterChains.toArray(new FilterChain[]{})));


        builder.addArguments("-map", "[v]");
        builder.addArguments("-map", String.format("%d:a", segments.size())); //TODO: Complex audio filters

        // execute, concat the segments. & add the audio
        builder.execute();
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


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.releaseFrameGrabber();

        return segmentFiles;
    }


}