package de.yugata.easy.edits.editor;

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
    private long editTime;
    private long introStart;
    private long introEnd;

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

    public EditInfoBuilder setEditTime(long editTime) {
        this.editTime = editTime;
        return this;
    }

    public EditInfoBuilder setIntroStart(long introStart) {
        this.introStart = introStart;
        return this;
    }

    public EditInfoBuilder setIntroEnd(long introEnd) {
        this.introEnd = introEnd;
        return this;
    }

    public EditInfo createEditInfo() {
        return new EditInfo(videoCodecName, audioCodecName, imageWidth, imageHeight, audioChannels, pixelFormat, videoCodec, videoBitrate, imageScalingFlags, aspectRatio, frameRate, sampleFormat, audioCodec, audioBitrate, sampleRate, editTime, introStart, introEnd);
    }
}