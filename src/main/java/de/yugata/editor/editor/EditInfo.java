package de.yugata.editor.editor;

import java.nio.Buffer;
import java.util.Map;

public class EditInfo {

    private final String  videoCodecName, audioCodecName;
    private final int imageWidth, imageHeight , audioChannels ;
    private final int pixelFormat, videoCodec, videoBitrate, imageScalingFlags;
    private final double aspectRatio, frameRate;
    private final int sampleFormat, audioCodec, audioBitrate, sampleRate;
    private final int bpp;
    private final double gamma;
    private final boolean deinterlace;
    private final Map<String, String> options;
    private final Map<String, String> videoOptions;
    private final Map<String, String> audioOptions ;
    private final Map<String, String> metadata;
    private final Map<String, String> videoMetadata ;
    private final Map<String, String> audioMetadata;
    private final Map<String, Buffer> videoSideData ;
    private final Map<String, Buffer> audioSideData;

    private final long editTime;

    private final long introStart, introEnd;


    public EditInfo(String videoCodecName, String audioCodecName, int imageWidth, int imageHeight, int audioChannels, int pixelFormat, int videoCodec, int videoBitrate, int imageScalingFlags, double aspectRatio, double frameRate, int sampleFormat, int audioCodec, int audioBitrate, int sampleRate, int bpp, double gamma, boolean deinterlace, Map<String, String> options, Map<String, String> videoOptions, Map<String, String> audioOptions, Map<String, String> metadata, Map<String, String> videoMetadata, Map<String, String> audioMetadata, Map<String, Buffer> videoSideData, Map<String, Buffer> audioSideData, long editTime, long introStart, long introEnd) {
        this.videoCodecName = videoCodecName;
        this.audioCodecName = audioCodecName;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.audioChannels = audioChannels;
        this.pixelFormat = pixelFormat;
        this.videoCodec = videoCodec;
        this.videoBitrate = videoBitrate;
        this.imageScalingFlags = imageScalingFlags;
        this.aspectRatio = aspectRatio;
        this.frameRate = frameRate;
        this.sampleFormat = sampleFormat;
        this.audioCodec = audioCodec;
        this.audioBitrate = audioBitrate;
        this.sampleRate = sampleRate;
        this.bpp = bpp;
        this.gamma = gamma;
        this.deinterlace = deinterlace;
        this.options = options;
        this.videoOptions = videoOptions;
        this.audioOptions = audioOptions;
        this.metadata = metadata;
        this.videoMetadata = videoMetadata;
        this.audioMetadata = audioMetadata;
        this.videoSideData = videoSideData;
        this.audioSideData = audioSideData;
        this.editTime = editTime;
        this.introStart = introStart;
        this.introEnd = introEnd;
    }

    public String getVideoCodecName() {
        return videoCodecName;
    }

    public String getAudioCodecName() {
        return audioCodecName;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public int getAudioChannels() {
        return audioChannels;
    }

    public int getPixelFormat() {
        return pixelFormat;
    }

    public int getVideoCodec() {
        return videoCodec;
    }

    public int getVideoBitrate() {
        return videoBitrate;
    }

    public int getImageScalingFlags() {
        return imageScalingFlags;
    }

    public double getAspectRatio() {
        return aspectRatio;
    }

    public double getFrameRate() {
        return frameRate;
    }

    public int getSampleFormat() {
        return sampleFormat;
    }

    public int getAudioCodec() {
        return audioCodec;
    }

    public int getAudioBitrate() {
        return audioBitrate;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getBpp() {
        return bpp;
    }

    public double getGamma() {
        return gamma;
    }

    public boolean isDeinterlace() {
        return deinterlace;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public Map<String, String> getVideoOptions() {
        return videoOptions;
    }

    public Map<String, String> getAudioOptions() {
        return audioOptions;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public Map<String, String> getVideoMetadata() {
        return videoMetadata;
    }

    public Map<String, String> getAudioMetadata() {
        return audioMetadata;
    }

    public Map<String, Buffer> getVideoSideData() {
        return videoSideData;
    }

    public Map<String, Buffer> getAudioSideData() {
        return audioSideData;
    }

    public long getEditTime() {
        return editTime;
    }

    public long getIntroStart() {
        return introStart;
    }

    public long getIntroEnd() {
        return introEnd;
    }
}
