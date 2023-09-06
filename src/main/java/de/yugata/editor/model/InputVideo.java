package de.yugata.editor.model;

public class InputVideo {

    private final int width, height, videoCodec, bitrate;
    private final double frameRate;

    public InputVideo(int width, int height, double frameRate, int videoCodec, int bitrate) {
        this.width = width;
        this.height = height;
        this.videoCodec = videoCodec;
        this.bitrate = bitrate;
        this.frameRate = frameRate;
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
}

