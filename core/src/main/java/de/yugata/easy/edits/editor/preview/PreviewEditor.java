package de.yugata.easy.edits.editor.preview;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.yugata.easy.edits.editor.edit.EditInfo;
import de.yugata.easy.edits.editor.edit.EditInfoBuilder;
import de.yugata.easy.edits.editor.edit.EditingFlag;
import de.yugata.easy.edits.editor.video.VideoClip;
import de.yugata.easy.edits.filter.FilterManager;
import de.yugata.easy.edits.filter.FilterWrapper;
import de.yugata.easy.edits.util.FFmpegUtil;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.*;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.ffmpeg.global.avutil.*;

public class PreviewEditor {

    private final String videoPath;

    private final File workingDirectory;
    private final EnumSet<EditingFlag> editingFlags;

    private final List<FilterWrapper> filters;


    //TODO: Maybe preview filters aswell?


    public PreviewEditor(final String videoPath, final File workingDirectory, final EnumSet<EditingFlag> flags, final List<FilterWrapper> filters) {
        this.videoPath = videoPath;
        this.workingDirectory = workingDirectory;
        this.editingFlags = flags;
        this.filters = filters;

        if (flags.contains(EditingFlag.PRINT_DEBUG)) {
            FFmpegLogCallback.set();
            av_log_set_level(AV_LOG_VERBOSE);
        }
    }

    public static PreviewEditor fromJson(final JsonObject root) {
        /* Base attributes */
        final String sourceVideo = root.get("source_video").getAsString();
        final String workingPath = root.get("working_path").getAsString();

        /* Filters */
        final JsonArray filters = root.getAsJsonArray("filters");

        final List<FilterWrapper> mappedFilters = new ArrayList<>();
        filters.forEach(jsonElement -> mappedFilters.add(new FilterWrapper(jsonElement.getAsJsonObject())));


        /* Editing flags */

        final JsonObject editingFlags = root.getAsJsonObject("editing_flags");
        final EnumSet<EditingFlag> mappedEditingFlags = EnumSet.noneOf(EditingFlag.class);

        editingFlags.asMap().forEach((key, value) -> {
            if (value.getAsBoolean()) mappedEditingFlags.add(EditingFlag.valueOf(key));
        });

        return new PreviewEditor(sourceVideo, new File(workingPath), mappedEditingFlags, mappedFilters);
    }

    private void configureRecorder(final FFmpegFrameRecorder recorder, final FFmpegFrameGrabber grabber) {
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

        recorder.setAudioOption("ac", "2"); // Downsample the 5.1 to stereo

        recorder.setVideoQuality(36);
        recorder.setPixelFormat(AV_PIX_FMT_YUV420P);
        recorder.setSampleFormat(avutil.AV_SAMPLE_FMT_FLTP);
        recorder.setSampleRate(grabber.getSampleRate());
        recorder.setFrameRate(grabber.getFrameRate());

        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC); // Standard
        recorder.setVideoCodecName("h264_nvenc");
    }

    private void configureFrameGrabber(final FFmpegFrameGrabber grabber) {
        FFmpegUtil.configureGrabber(grabber);
        grabber.setImageWidth(grabber.getImageWidth() / 8);
        grabber.setImageHeight(grabber.getImageHeight() / 8);
        grabber.setVideoCodecName("hevc_cuvid");
    }


    public String generatePreview(VideoClip videoClip) {

        try (final FFmpegFrameGrabber videoGrabber = new FFmpegFrameGrabber(videoPath)) {
            this.configureFrameGrabber(videoGrabber);

            // idk why this is needed, it works in the editor, the code is almost the same, but hey, at least it works now.
            // hours_wasted=2
            if (videoClip.isMuteAudio()) {
                videoGrabber.setAudioStream(Integer.MAX_VALUE);
            }

            videoGrabber.start();

            videoGrabber.setTimestamp(videoClip.getTimeStamp());

            final File previewFile = new File(workingDirectory, "clip_" + UUID.randomUUID() + ".mkv");

            final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(previewFile, videoGrabber.getImageWidth(), videoGrabber.getImageHeight(), 2);
            this.configureRecorder(recorder, videoGrabber);
            recorder.start();

            Frame frame;
            while ((frame = videoGrabber.grab()) != null && videoGrabber.getTimestamp() - videoClip.getTimeStamp() < videoClip.getLength()) {
                recorder.record(frame);
            }

            recorder.close();
            return previewFile.getAbsolutePath();
        } catch (FrameGrabber.Exception | FrameRecorder.Exception e) {
            e.printStackTrace();
            return "";
        }
    }


    public String editPreviews(final List<String> previewPaths, final String audioPath) {
        final File editOutput = new File(workingDirectory, "preview_" + UUID.randomUUID() + ".mkv");

        // get image width & height


        try (final FFmpegFrameGrabber videoGrabber = new FFmpegFrameGrabber(videoPath)) {
            this.configureFrameGrabber(videoGrabber);
            videoGrabber.start();

            // Configure the recorder
            final FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(editOutput, videoGrabber.getImageWidth(), videoGrabber.getImageHeight(), 2);
            this.configureRecorder(recorder, videoGrabber);
            recorder.start();

            // Grabs the audio frames from the supplied audio
            final FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(audioPath);
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
                    .createEditInfo();


            // Populate the filters
            FilterManager.FILTER_MANAGER.populateFilters(filters, editInfo);


            // optional: audio filter with chained  audio filters.
            final FFmpegFrameFilter simpleAudioFiler = FFmpegUtil.populateAudioFilters();
            if (simpleAudioFiler != null) {
                simpleAudioFiler.setSampleRate(recorder.getSampleRate());
                simpleAudioFiler.setSampleFormat(recorder.getSampleFormat());
                simpleAudioFiler.start();
            }
            // Audio filter which overlays audio and input audio if there is input audio given.
            final FFmpegFrameFilter overlayFilter = FFmpegUtil.configureAudioFilter("[0:a]volume=0.25[a1]; [a1][1:a]amerge=inputs=2[a]", recorder.getSampleRate(), recorder.getSampleFormat());
            overlayFilter.setAudioInputs(2);
            overlayFilter.start();

            // Filter to convert the supplied audio into the same format & sample rate as the recorder.
            final BytePointer sampleFormatName = avutil.av_get_sample_fmt_name(recorder.getSampleFormat());
            final FFmpegFrameFilter convertAudioFilter = FFmpegUtil.configureAudioFilter(String.format("aformat=sample_fmts=%s:sample_rates=%d", sampleFormatName.getString(), recorder.getSampleRate()), recorder.getSampleRate(), recorder.getSampleFormat());
            convertAudioFilter.start();
            sampleFormatName.close(); // release reference

            // optional: Frame filter with chained together video filters
            final FFmpegFrameFilter simpleVideoFiler = FFmpegUtil.populateVideoFilters(editInfo);


            for (final String previewPath : previewPaths) {
                // grabber for the individual clip segment
                final FFmpegFrameGrabber segmentGrabber = new FFmpegFrameGrabber(previewPath);
                FFmpegUtil.configureGrabber(segmentGrabber);
                segmentGrabber.setSampleFormat(recorder.getSampleFormat());
                segmentGrabber.setSampleRate(recorder.getSampleRate());
                segmentGrabber.setPixelFormat(recorder.getPixelFormat());
                segmentGrabber.setVideoCodecName("h264_cuvid"); // HW-Accelerated grabbing
                segmentGrabber.start();

                // Optional: Populate the transition filters, we have to reconfigure them every time, as the offsets depend on it.
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
                    FFmpegUtil.pushToFilters(frame, recorder, filters);
                }

                if (!segmentGrabber.hasAudio()) {
                    Frame audioFrame;
                    // Add as many audio frames as are needed for the segment.
                    final long startTime = audioGrabber.getTimestamp();

                    while ((audioFrame = audioGrabber.grab()) != null && audioGrabber.getTimestamp() - startTime < segmentGrabber.getLengthInTime()) {
                        recorder.setTimestamp(audioGrabber.getTimestamp());
                        recorder.record(audioFrame);
                    }
                } else { // mix audio & video audio
                    // reset the framegrabber
                    // TODO: maybe restart?
                    segmentGrabber.setTimestamp(0);

                    //NOTE:: we grab the audio frames at the same rate as the video samples, as they have their rates synchronized.

                    while ((frame = segmentGrabber.grabSamples()) != null) {

                        Frame audioFrame;
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
                }

                // Close the transition filter, free the resources
                if (transitionFilter != null)
                    transitionFilter.close();

                segmentGrabber.close();
            }

            if (simpleVideoFiler != null)
                simpleVideoFiler.close();

            if (simpleAudioFiler != null)
                simpleAudioFiler.close();


            overlayFilter.close();
            recorder.close();
            audioGrabber.close();
        } catch (FrameRecorder.Exception | FrameGrabber.Exception | FrameFilter.Exception e) {
            e.printStackTrace();
            return "";
        }
        return editOutput.getAbsolutePath();
    }


}
