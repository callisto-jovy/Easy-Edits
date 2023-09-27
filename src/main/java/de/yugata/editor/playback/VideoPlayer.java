package de.yugata.editor.playback;

import de.yugata.editor.editor.Editor;
import org.bytedeco.javacv.Frame;

import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class VideoPlayer {

    public static final VideoPlayer INSTANCE = new VideoPlayer();

    private VideoCanvas videoCanvas;
    private VideoThread videoThread;
    private KeyboardThread keyboardThread;

    public VideoPlayer() {
    }


    public void start() {
        if (videoCanvas != null)
            this.videoCanvas.dispose();
        this.videoCanvas = new VideoCanvas();

        this.keyboardThread = new KeyboardThread(this);
        this.keyboardThread.start();

        this.videoThread = new VideoThread(this);
        this.videoThread.start();
    }

    private void checkCanvas() {
        if (this.videoCanvas == null) {
            throw new RuntimeException("Video canvas may not be null and has to be initialized!");
        }
    }

    public void showImage(final Frame frame) {
        this.checkCanvas();
        this.videoCanvas.showImage(frame);
    }

    private boolean checkVideoThread() {
        return videoThread.isAlive() && !videoThread.isInterrupted();
    }


    public void stamp() {
        if (checkVideoThread())
            Editor.INSTANCE.addTimeStamp(videoThread.getCurrentTimeStamp());
    }

    public void removeStamp() {
        if (checkVideoThread())
            Editor.INSTANCE.removeLastTimeStamp();
    }

    public void pause() {
        if (checkVideoThread())
            videoThread.pause();
    }

    public void seek(SkipIntervalls intervall, int mul) {
        if (checkVideoThread())
            videoThread.seek(mul * intervall.getIntervall());
    }

    public void seekTo(final long stamp) {
        if (checkVideoThread())
            videoThread.seekTo(stamp);
    }

    public void stop() {
        this.videoCanvas.dispose();

        synchronized (videoThread) {
            videoThread.interrupt();
        }

        synchronized (keyboardThread) {
            keyboardThread.interrupt();
        }

        System.out.println("Stopped threads.");
    }


    public KeyEvent waitKey() throws InterruptedException {
        this.checkCanvas();

        synchronized (keyboardThread) {
            return videoCanvas.waitKey();
        }
    }

    public void setIntro() {
        if (Editor.INSTANCE.getIntroStart() == -1) {
            Editor.INSTANCE.setIntroStart(videoThread.getCurrentTimeStamp());
        } else {
            Editor.INSTANCE.setIntoEnd(videoThread.getCurrentTimeStamp());
        }
    }
}
