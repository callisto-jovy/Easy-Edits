package de.yugata.easy.edits.util;


import de.yugata.easy.edits.editor.EditingFlag;
import de.yugata.easy.edits.editor.filter.Filter;
import de.yugata.easy.edits.editor.filter.FilterManager;
import org.apache.commons.io.FileUtils;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.tools.Slf4jLogger;
import org.bytedeco.javacv.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.EnumSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * TODO: This needs some documentation, not only for other, but also for myself.
 */
public class FFmpegUtil {

    public static final File RESOURCE_DIRECTORY = new File("editor_resources");

    public static final File FFMPEG_BIN = new File(RESOURCE_DIRECTORY, "ffmpeg_bin");

    public static final String LATEST_FFMPEG = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip";

    static {
        if (!RESOURCE_DIRECTORY.exists()) {
            RESOURCE_DIRECTORY.mkdir();
        }

        if (!FFMPEG_BIN.exists()) {
            FFMPEG_BIN.mkdirs();
        }

        loadFFmpeg();
    }


    // private constructor to restrict object creation
    private FFmpegUtil() {

    }


    public static void loadFFmpeg() {
        // TODO: Versioning, editor_resources should have a file with the latest auto build version.
        // Or: timer that loads the new version every month / week

        if (FFMPEG_BIN.length() > 0)
            return;

        System.out.println("Downloading latest FFMPEG.");

        final File output = new File(FFMPEG_BIN, "ffmpeg.zip");

        try {
            FileUtils.copyURLToFile(new URL(LATEST_FFMPEG), output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Extract zip
        unzipFFmpeg(output, FFMPEG_BIN);

        // Delete zip
        output.delete();
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


    public static void configureGrabber(final FFmpegFrameGrabber grabber) {
        grabber.setOption("allowed_extensions", "ALL");
        grabber.setOption("hwaccel", "cuda");
        grabber.setAudioStream(1);
        grabber.setVideoBitrate(0);
    }

    public static FFmpegFrameRecorder createRecorder(final File outputFile, final EnumSet<EditingFlag> editingFlags, final FFmpegFrameGrabber inputGrabber) throws FFmpegFrameRecorder.Exception {
        final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, inputGrabber.getImageWidth(), inputGrabber.getImageHeight(), 2);

        recorder.setFormat("mp4");

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
            recorder.setVideoQuality(EditingFlag.BEST_QUALITY.getSetting()); // best quality --> Produces big files
            recorder.setVideoOption("cq", String.valueOf(EditingFlag.BEST_QUALITY.getSetting()));
            recorder.setOption("preset", "slow");
            recorder.setVideoOption("profile", "main10");
            recorder.setVideoOption("crf", String.valueOf(EditingFlag.BEST_QUALITY.getSetting()));
            recorder.setVideoOption("qmin", "0");
            recorder.setVideoOption("qmax", "0");
            recorder.setOption("tune", "hq");
            recorder.setOption("bf", "2");
            recorder.setOption("lookahead", "8");
            recorder.setOption("rc", "constqp");

            //  recorder.setOption("b:v", "0");
        }


        recorder.setVideoCodec(avcodec.AV_CODEC_ID_HEVC);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        // One of the pixel formats supported by h264 nvenc
        //   recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setFrameRate(inputGrabber.getFrameRate());
        recorder.setSampleRate(inputGrabber.getSampleRate()); //TODO: Audio sample rate
        // Select the "highest" bitrate.
        recorder.setVideoBitrate(0); // max bitrate
        //   recorder.setVideoCodecName("h264_nvenc"); // Hardware-accelerated encoding.

        return recorder;
    }


    /**
     * Chains a list of simple {@link Filter} together.
     * Will add an [in] and [out], as well as passthroughs to all the intermediary filters.
     * sample chaining result: <br>
     * [in]fade=t=in:st=0:d=120ms[f0]; [f0]curves=preset=medium_contrast[c0]; [c0]curves=preset=lighter[c1]; [c1]curves=all='0/0 0.5/0.4 1/1'[out]
     *
     * @param filters list of filters to chain.
     * @return all chained filters in one string.
     */
    private static String chainSimpleFilters(final List<Filter> filters) {
        if (filters.isEmpty()) return null;
        final StringBuilder chainedFilters = new StringBuilder();

        // Add in to first filter...
        chainedFilters.append("[in]");


        // Append destination for the last filter in the chain
        chainedFilters.append("[out]");

        return chainedFilters.toString();
    }

    /**
     * Chains a list of {@link Filter} together.
     *
     * @param filters list of filters to chain.
     * @param builder the initial {@link StringBuilder}. if left null an empty builder.
     * @return a {@link StringBuilder} that might be manipulated further.
     */
    private static StringBuilder chainFilters(final List<Filter> filters, StringBuilder builder) {
        if (filters.isEmpty())
            return new StringBuilder();

        if (builder == null)
            builder = new StringBuilder();

        for (int i = 0; i < filters.size(); i++) {
            final Filter videoFilter = filters.get(i);

            // Only add if the filter is not the first in the list.
            if (i > 0) {
                builder
                        .append("[f")
                        .append((i - 1))
                        .append("]");
            }
            // append the filter
            builder
                    .append(videoFilter.getFilter());

            // Append output if this filter is not the last one
            if (i < filters.size() - 1) {
                builder
                        .append("[f")
                        .append(i)
                        .append("]")
                        .append(";");
                // --> [f i]; so that in the next iteration this will be the input.
            }
        }

        return builder;
    }


    /**
     * TODO: This will be a massive fucking pain in the ass...
     * The filters will have to be "re-parsed", to set certain variables which are only clear from the number of segments that are available
     * & the filter's position in the filter chain...
     * fuck me, we also need to know the offset, so somehow, we need to read the segment & apply the filters...
     * this would be so much easier with javacv, but certain filters are not there, which are really needed...
     * I will figure this out somehow...
     * Maybe concat two videos, then the next, etc. in that case, the offset will always be 0.
     * Or maybe, there are no fade transitions...
     * and variables such as offset are not possible for this project
     * idk man
     *
     * @param segments
     * @return
     */
    public static String chainComplexFilters(final int segments) {
        final List<Filter> videoFilters = FilterManager.FILTER_MANAGER.getComplexVideoFilters();
        final List<Filter> audioFilters = FilterManager.FILTER_MANAGER.getComplexAudioFilters();

        final StringBuilder chainedFilters = new StringBuilder();

        if (videoFilters.isEmpty() && audioFilters.isEmpty()) {
            // prepare inputs for concat
            for (int i = 0; i < segments; i++) {
                chainedFilters.append("[").append(i).append(":v]");
            }
            return chainedFilters.toString();

            // --> [v0][v1][v2]...[vn] inputs to concat.
        }


        for (int i = 0; i < segments; i++) {

            // Video input for filter (x...)
            // [n:v] input
            chainedFilters.append("[").append(i).append(":v]");
            chainFilters(videoFilters, chainedFilters);
            // output [vn];
            chainedFilters
                    .append("[")
                    .append("v")
                    .append(i)
                    .append("]")
                    .append(";");
        }

        if (!audioFilters.isEmpty()) {
            // Audio is a bit easier...

            // Add audio in, we only have one audio-source
            // The audio in is the last input
            chainedFilters.append("[").append(segments).append(":a]");

            chainFilters(audioFilters, chainedFilters);

            chainedFilters.append("[a0]; [a0]");
        }


        // prepare inputs for concat
        for (int i = 0; i < segments; i++) {
            chainedFilters.append("[v").append(i).append("]");
        }

        // --> [v0][v1][v2]...[vn] inputs to concat.
        return chainedFilters.toString();
    }


    public static String chainAudioFilters() {
        final List<Filter> filters = FilterManager.FILTER_MANAGER.getAudioFilters();

        if (filters.isEmpty())
            return null;

        final String chained = chainSimpleFilters(filters);
        return chained;
    }


    public static String chainVideoFilters() {
        final List<Filter> filters = FilterManager.FILTER_MANAGER.getVideoFilters();

        if (filters.isEmpty())
            return null;

        final String chained = chainSimpleFilters(filters);
        return chained;
    }

    private static void unzipFFmpeg(final File zip, final File dest) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(zip);
            final ZipInputStream zis = new ZipInputStream(fis);

            ZipEntry zipEntry;

            while ((zipEntry = zis.getNextEntry()) != null) {
                // Skip subdirectories, we want all files in one place.
                if (zipEntry.isDirectory()) {
                    System.out.println(zipEntry.getName() + "is directory, skipping.");
                    continue;
                }

                // name includes the path, so we have to strip it.
                final String name = zipEntry.getName().substring(zipEntry.getName().lastIndexOf('/') + 1);

                final File newFile = new File(dest, name);
                System.out.println("Unzipping to " + newFile.getAbsolutePath());

                // Transfer data using channels

                final FileOutputStream fos = new FileOutputStream(newFile);
                fos.getChannel().transferFrom(Channels.newChannel(zis), 0, Long.MAX_VALUE);
                fos.close();

                //close this ZipEntry
                zis.closeEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
