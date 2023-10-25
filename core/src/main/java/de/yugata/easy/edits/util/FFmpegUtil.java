package de.yugata.easy.edits.util;


import de.yugata.easy.edits.editor.edit.EditInfo;
import de.yugata.easy.edits.editor.edit.EditingFlag;
import de.yugata.easy.edits.filter.Filter;
import de.yugata.easy.edits.filter.FilterManager;
import de.yugata.easy.edits.filter.FilterType;
import org.apache.commons.io.FileUtils;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * TODO: This needs some documentation, not only for other, but also for myself.
 */
public class FFmpegUtil {

    public static final File RESOURCE_DIRECTORY = new File("editor_resources");


    static {
        if (!RESOURCE_DIRECTORY.exists()) {
            RESOURCE_DIRECTORY.mkdir();
        }
    }

    /**
     * Attempts to download a font file into the resource directory.
     * Falls back to arial if a IOException is thrown. Formats the path to the file to a FFMPEG-acceptable format.
     *
     * @return cleaned string to the font file downloaded / fallback.
     */
    public static String getFontFile() {
        // Download the font file to the temp.
        File dalton = new File(RESOURCE_DIRECTORY, "dalton.otf");

        try {
            if (!dalton.exists())
                FileUtils.copyURLToFile(new URL("https://github.com/callisto-jovy/Fast-Edits/releases/download/external/Dalton.otf"), dalton);
        } catch (IOException e) {
            e.printStackTrace();
            return sanitizePath("C:/Windows/Fonts/Arial.ttf");
        }

        // Our font file is in the resources, but ffmpeg needs an absolute path
        return sanitizePath(dalton.getAbsolutePath());
    }

    /**
     * Sanitizes a given filepath to ensure that FFMPEG will accept it.
     * Removes all backwards slashes and replaces them with forwards slashes.
     * The colon (i.e. C:) is escaped.
     *
     * @param filePath the file path to sanitize
     * @return the sanitized filepath
     */
    public static String sanitizePath(final String filePath) {
        return filePath
                .replace('\\', '/')
                .replace(":", "\\:");
    }

    /**
     * Pushes a {@link Frame} through a pipeline (array) of {@link FFmpegFrameFilter}.
     * Lastly the frame(s) are recorded by the supplied {@link FFmpegFrameRecorder}.
     *
     * @param frame    the frame to push through the filters that is supposed to be recorded.
     * @param recorder the recorder to record the frame(s) to.
     * @param filters  array of filters the frame is pushed through and pulled from.
     */
    public static void pushToFilters(final Frame frame, final FFmpegFrameRecorder recorder, final FFmpegFrameFilter... filters) {
        try {
            // just record if no filters are in the chain.
            if (filters.length == 0) {
                recorder.record(frame);
                return;
            }

            // Feed the frame to the first filter
            filters[0].push(frame);

            // Loop through the following filters.
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
            // Push the frames that moved through the entire filter chain to the recorder
            Frame processedFrame;
            while ((processedFrame = finalFilter.pull()) != null) {
                recorder.record(processedFrame, finalFilter.getPixelFormat());
            }
        } catch (FFmpegFrameRecorder.Exception | FrameFilter.Exception e) {
            e.printStackTrace();
        }
    }


    public static void pushToFilterOrElse(final Frame frame, final FFmpegFrameFilter filter, final Consumer<Frame> acceptFunction) throws FFmpegFrameFilter.Exception {
        if (filter == null) {
            acceptFunction.accept(frame);
            return;
        }

        filter.push(frame);

        Frame filterFrame;
        while ((filterFrame = filter.pull()) != null) {
            acceptFunction.accept(filterFrame);
        }
    }



    public static void configureGrabber(final FFmpegFrameGrabber grabber) {
        grabber.setOption("allowed_extensions", "ALL");
        grabber.setOption("hwaccel", "cuda");
        grabber.setVideoBitrate(0);
    }

    public static FFmpegFrameRecorder createRecorder(final File outputFile, final EnumSet<EditingFlag> editingFlags, final FFmpegFrameGrabber inputGrabber) throws FFmpegFrameRecorder.Exception {
        final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, inputGrabber.getImageWidth(), inputGrabber.getImageHeight(), 2);

        recorder.setFormat("mkv");

        // Preserve the color range for the HDR video files.
        // This lets us tone-map the hdr content later on if we want to.
        //TODO: get this from the grabber
        if (editingFlags.contains(EditingFlag.WRITE_HDR_OPTIONS)) {
            recorder.setVideoOption("color_range", "tv");
            recorder.setVideoOption("colorspace", "bt2020nc");
            recorder.setVideoOption("color_primaries", "bt2020");
            recorder.setVideoOption("color_trc", "smpte2084");
        }

        if (editingFlags.contains(EditingFlag.BEST_QUALITY)) {
            recorder.setVideoQuality(12); // best quality --> Produces big files
            recorder.setVideoOption("cq", "12");
            recorder.setOption("preset", "slow");
            recorder.setVideoOption("profile", "main10");
            recorder.setVideoOption("crf", "12");
            recorder.setVideoOption("qmin", "0");
            recorder.setVideoOption("qmax", "0");
            recorder.setOption("tune", "hq");
            recorder.setOption("bf", "2");
            recorder.setOption("lookahead", "8");
            recorder.setOption("rc", "constqp");
        }

        recorder.setAudioOption("ac", "2"); // Downsample the 5.1 to stereo
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H265);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC); // Standard
        recorder.setFrameRate(inputGrabber.getFrameRate()); //
        recorder.setSampleRate(inputGrabber.getSampleRate()); // Sample rate from the audio source
        // Select the "highest" bitrate.
        recorder.setVideoBitrate(0); // max bitrate

        return recorder;
    }


    public static FFmpegFrameFilter configureAudioFilter(final String filter, final int sampleRate, final int sampleFormat) {
        final FFmpegFrameFilter fFmpegFrameFilter = new FFmpegFrameFilter(filter, 2);
        fFmpegFrameFilter.setSampleRate(sampleRate);
        fFmpegFrameFilter.setSampleFormat(sampleFormat);
        fFmpegFrameFilter.setVideoInputs(0); // This apparently is fucking important. The default video inputs = 1! Unfortunately, video inputs is not adjusted, I may have to open a PR for this.

        return fFmpegFrameFilter;
    }


    /**
     * Chains a list of {@link Filter} together.
     * Will add an [in] and [out], as well as passthroughs to all the intermediary filters.
     * sample chaining result: <br>
     * [in]fade=t=in:st=0:d=120ms[f0]; [f0]curves=preset=medium_contrast[c0]; [c0]curves=preset=lighter[c1]; [c1]curves=all='0/0 0.5/0.4 1/1'[out]
     *
     * @param filters list of filters to chain.
     * @return all chained filters in one string.
     */
    private static String chainFilters(final List<Filter> filters) {
        if (filters.isEmpty()) return null;
        final StringBuilder chainedFilters = new StringBuilder();

        // Add in to first filter...
        chainedFilters.append("[in]");

        for (int i = 0; i < filters.size(); i++) {
            final Filter videoFilter = filters.get(i);

            // Only add if the filter is not the first in the list.
            if (i > 0) {
                chainedFilters
                        .append("[f")
                        .append((i - 1))
                        .append("]");
            }
            // append the filter
            chainedFilters
                    .append(videoFilter.getFilter());

            // Append output if this filter is not the last one
            if (i < filters.size() - 1) {
                chainedFilters
                        .append("[f")
                        .append(i)
                        .append("]")
                        .append(";");
                // --> [f i]; so that in the next iteration this will be the input.
            }
        }
        // Append destination for the last filter in the chain
        chainedFilters.append("[out]");

        return chainedFilters.toString();
    }


    public static FFmpegFrameFilter populateVideoFilters(final List<Filter> filters, final EditInfo editInfo) throws FFmpegFrameFilter.Exception {
        if (filters.isEmpty())
            return null;

        final String chained = chainFilters(filters);


        final FFmpegFrameFilter filter = new FFmpegFrameFilter(chained, editInfo.getImageWidth(), editInfo.getImageHeight());
        filter.setPixelFormat(editInfo.getPixelFormat());
        filter.setFrameRate(editInfo.getFrameRate());
        filter.start();

        return filter;
    }


    public static FFmpegFrameFilter populateTransitionFilters(EditInfo editInfo) throws FFmpegFrameFilter.Exception {
        final List<Filter> filters = FilterManager.FILTER_MANAGER.getFilters(filter -> filter.getFilterType() == FilterType.TRANSITION);

        return populateVideoFilters(filters, editInfo);
    }


    public static FFmpegFrameFilter populateAudioFilters() throws FFmpegFrameFilter.Exception {
        final List<Filter> filters = FilterManager.FILTER_MANAGER.getFilters(filter -> filter.getFilterType() == FilterType.AUDIO);

        if (filters.isEmpty())
            return null;

        final String chained = chainFilters(filters);

        return new FFmpegFrameFilter(chained, 2);
    }


    public static FFmpegFrameFilter populateVideoFilters(final EditInfo editInfo) throws FFmpegFrameFilter.Exception {
        final List<Filter> filters = FilterManager.FILTER_MANAGER.getFilters(filter -> filter.getFilterType() == FilterType.VIDEO);

        return populateVideoFilters(filters, editInfo);
    }

}
