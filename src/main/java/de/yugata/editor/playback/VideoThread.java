package de.yugata.editor.playback;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

public class VideoThread extends Thread {

    private final FFmpegFrameGrabber frameGrabber;

    private final CanvasFrame parent;

    private boolean paused;

    public VideoThread(final String videoInput, final CanvasFrame parent) {
        this.parent = parent;
        this.frameGrabber = new FFmpegFrameGrabber(videoInput);
    }

    @Override
    public void start() {
        try {
            frameGrabber.start();
        } catch (FFmpegFrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
        super.start();
    }

    @Override
    public void run() {
        super.run();
        try {
            Frame frame;
            while ((frame = frameGrabber.grab()) != null && !interrupted()) {
                parent.showImage(frame);

                synchronized (this) {
                    if (paused) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

            }
            frameGrabber.stop();
        } catch (FFmpegFrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stopGrabber() {
        try {
            frameGrabber.stop();
        } catch (FFmpegFrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void pause() {
        // Unpause
        if (paused) {
            synchronized (this) {
                this.notify();
            }
        }
        this.paused = !paused;
    }

    public void seek(final long amount) {
        try {
            frameGrabber.setTimestamp(frameGrabber.getTimestamp() + amount);
        } catch (FFmpegFrameGrabber.Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public double getCurrentTimeStamp() {
        return frameGrabber.getTimestamp();
    }
}
