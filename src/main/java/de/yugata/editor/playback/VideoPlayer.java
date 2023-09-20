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
        this.keyboardThread = new KeyboardThread(this);
        this.videoThread = new VideoThread(this);
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

    public void start() {
        this.videoCanvas = new VideoCanvas(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                synchronized (videoThread) {
                    videoThread.interrupt();
                }

                synchronized (keyboardThread) {
                    keyboardThread.interrupt();
                }
            }
        });


        if (keyboardThread.getState() == Thread.State.TERMINATED)
            this.keyboardThread.run();
        else
            this.keyboardThread.start();


        if (videoThread.getState() == Thread.State.TERMINATED)
            this.videoThread.run();
        else
            this.videoThread.start();
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

    public void seek(long l) {
        if (checkVideoThread())

            videoThread.seek(l);
    }

    public void stop() {
        synchronized (videoThread) {
            videoThread.interrupt();
        }

        synchronized (keyboardThread) {
            keyboardThread.interrupt();
        }
    }


    public KeyEvent waitKey() throws InterruptedException {
        this.checkCanvas();
        return videoCanvas.waitKey();
    }

    public void setIntro() {
        if (Editor.INSTANCE.getIntroStart() == -1) {
            Editor.INSTANCE.setIntroStart(videoThread.getCurrentTimeStamp());
        } else {
            Editor.INSTANCE.setIntoEnd(videoThread.getCurrentTimeStamp());
        }
    }
}
