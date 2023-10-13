package de.yugata.editor.model;

public class InputVideo {

    private final int width, height, videoCodec, bitrate, sampleRate;
    private final double frameRate;

    private final long totalLength;


    public InputVideo(int width, int height, double frameRate, int videoCodec, int bitrate, int sampleRate, long totalLength) {
        this.width = width;
        this.height = height;
        this.videoCodec = videoCodec;
        this.bitrate = bitrate;
        this.frameRate = frameRate;
        this.sampleRate = sampleRate;
        this.totalLength = totalLength;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int videoCodec() {
        return videoCodec;
    }

    public int bitrate() {
        return bitrate;
    }

    public double frameRate() {
        return frameRate;
    }

    public int sampleRate() {
        return sampleRate;
    }

    public long totalLength() {
        return totalLength;
    }
}

