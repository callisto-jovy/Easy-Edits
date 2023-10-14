package de.yugata.easy.edits.editor;

import java.nio.Buffer;
import java.util.Map;

public class EditInfoBuilder {
    private String videoCodecName;
    private String audioCodecName;
    private int imageWidth;
    private int imageHeight;
    private int audioChannels;
    private int pixelFormat;
    private int videoCodec;
    private int videoBitrate;
    private int imageScalingFlags;
    private double aspectRatio;
    private double frameRate;
    private int sampleFormat;
    private int audioCodec;
    private int audioBitrate;
    private int sampleRate;
    private int bpp;
    private double gamma;
    private boolean deinterlace;
    private Map<String, String> options;
    private Map<String, String> videoOptions;
    private Map<String, String> audioOptions;
    private Map<String, String> metadata;
    private Map<String, String> videoMetadata;
    private Map<String, String> audioMetadata;
    private Map<String, Buffer> videoSideData;
    private Map<String, Buffer> audioSideData;
    private long editTime;

    private long introStart, introEnd;


    public EditInfoBuilder setIntroEnd(long introEnd) {
        this.introEnd = introEnd;
        return this;
    }

    public EditInfoBuilder setIntroStart(long introStart) {
        this.introStart = introStart;
        return this;
    }

    public EditInfoBuilder setVideoCodecName(String videoCodecName) {
        this.videoCodecName = videoCodecName;
        return this;
    }

    public EditInfoBuilder setAudioCodecName(String audioCodecName) {
        this.audioCodecName = audioCodecName;
        return this;
    }

    public EditInfoBuilder setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
        return this;
    }

    public EditInfoBuilder setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
        return this;
    }

    public EditInfoBuilder setAudioChannels(int audioChannels) {
        this.audioChannels = audioChannels;
        return this;
    }

    public EditInfoBuilder setPixelFormat(int pixelFormat) {
        this.pixelFormat = pixelFormat;
        return this;
    }

    public EditInfoBuilder setVideoCodec(int videoCodec) {
        this.videoCodec = videoCodec;
        return this;
    }

    public EditInfoBuilder setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
        return this;
    }

    public EditInfoBuilder setImageScalingFlags(int imageScalingFlags) {
        this.imageScalingFlags = imageScalingFlags;
        return this;
    }

    public EditInfoBuilder setAspectRatio(double aspectRatio) {
        this.aspectRatio = aspectRatio;
        return this;
    }

    public EditInfoBuilder setFrameRate(double frameRate) {
        this.frameRate = frameRate;
        return this;
    }

    public EditInfoBuilder setSampleFormat(int sampleFormat) {
        this.sampleFormat = sampleFormat;
        return this;
    }

    public EditInfoBuilder setAudioCodec(int audioCodec) {
        this.audioCodec = audioCodec;
        return this;
    }

    public EditInfoBuilder setAudioBitrate(int audioBitrate) {
        this.audioBitrate = audioBitrate;
        return this;
    }

    public EditInfoBuilder setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        return this;
    }

    public EditInfoBuilder setBpp(int bpp) {
        this.bpp = bpp;
        return this;
    }

    public EditInfoBuilder setGamma(double gamma) {
        this.gamma = gamma;
        return this;
    }

    public EditInfoBuilder setDeinterlace(boolean deinterlace) {
        this.deinterlace = deinterlace;
        return this;
    }

    public EditInfoBuilder setOptions(Map<String, String> options) {
        this.options = options;
        return this;
    }

    public EditInfoBuilder setVideoOptions(Map<String, String> videoOptions) {
        this.videoOptions = videoOptions;
        return this;
    }

    public EditInfoBuilder setAudioOptions(Map<String, String> audioOptions) {
        this.audioOptions = audioOptions;
        return this;
    }

    public EditInfoBuilder setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public EditInfoBuilder setVideoMetadata(Map<String, String> videoMetadata) {
        this.videoMetadata = videoMetadata;
        return this;
    }

    public EditInfoBuilder setAudioMetadata(Map<String, String> audioMetadata) {
        this.audioMetadata = audioMetadata;
        return this;
    }

    public EditInfoBuilder setVideoSideData(Map<String, Buffer> videoSideData) {
        this.videoSideData = videoSideData;
        return this;
    }

    public EditInfoBuilder setAudioSideData(Map<String, Buffer> audioSideData) {
        this.audioSideData = audioSideData;
        return this;
    }

    public EditInfoBuilder setEditTime(long editTime) {
        this.editTime = editTime;
        return this;
    }

    public EditInfo createEditInfo() {
        return new EditInfo(videoCodecName, audioCodecName, imageWidth, imageHeight, audioChannels, pixelFormat, videoCodec, videoBitrate, imageScalingFlags, aspectRatio, frameRate, sampleFormat, audioCodec, audioBitrate, sampleRate, bpp, gamma, deinterlace, options, videoOptions, audioOptions, metadata, videoMetadata, audioMetadata, videoSideData, audioSideData, editTime, introStart, introEnd);
    }
}