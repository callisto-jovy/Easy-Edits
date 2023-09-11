package de.yugata.editor.playback;

import de.yugata.editor.editor.Editor;

import java.awt.event.KeyEvent;

import static java.awt.event.KeyEvent.*;

public class KeyboardThread extends Thread {

    private final VideoPlayer videoPlayer;

    public KeyboardThread(final VideoPlayer videoPlayer) {
        this.videoPlayer = videoPlayer;
    }

    private static final long SKIP_SECONDS_10 = 10 * 1000000L;
    private static final long SKIP_SECONDS_60 = 6 * SKIP_SECONDS_10;


    @Override
    public void run() {
        try {
            while (!interrupted()) {
                final KeyEvent key = videoPlayer.waitKey();


                switch (key.getKeyCode()) {

                    case VK_SPACE:
                        videoPlayer.pause();
                        break;

                    case VK_LEFT:
                        videoPlayer.seek(-SKIP_SECONDS_10);
                        break;

                    case VK_RIGHT:
                        videoPlayer.seek(SKIP_SECONDS_10);
                        break;

                    case VK_UP:
                        videoPlayer.seek(SKIP_SECONDS_60);
                        break;

                    case VK_DOWN:
                        videoPlayer.seek(-SKIP_SECONDS_60);
                        break;

                    // Ending the editing
                    case VK_ESCAPE:
                        videoPlayer.stop();
                        break;

                    case VK_X:
                        videoPlayer.stamp();
                        break;

                    case VK_V:
                        videoPlayer.removeStamp();
                        break;

                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
