package de.yugata.easy.edits.editor.edit;

import java.nio.Buffer;
import java.util.Map;

public class EditInfo {

    private final String  videoCodecName, audioCodecName;
    private final int imageWidth, imageHeight , audioChannels ;
    private final int pixelFormat, videoCodec, videoBitrate, imageScalingFlags;
    private final double aspectRatio, frameRate;
    private final int sampleFormat, audioCodec, audioBitrate, sampleRate;
    private final long editTime;
    private final long introStart, introEnd;


    public EditInfo(String videoCodecName, String audioCodecName, int imageWidth, int imageHeight, int audioChannels, int pixelFormat, int videoCodec, int videoBitrate, int imageScalingFlags, double aspectRatio, double frameRate, int sampleFormat, int audioCodec, int audioBitrate, int sampleRate, long editTime, long introStart, long introEnd) {
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
