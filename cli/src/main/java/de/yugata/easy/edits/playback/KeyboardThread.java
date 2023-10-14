package de.yugata.easy.edits.playback;

import java.awt.event.KeyEvent;

import static java.awt.event.KeyEvent.*;

public class KeyboardThread extends Thread {

    private final VideoPlayer videoPlayer;

    public KeyboardThread(final VideoPlayer videoPlayer) {
        this.videoPlayer = videoPlayer;
    }

    private SkipIntervalls skipSeconds = SkipIntervalls.SKIP_TEN_SECONDS;


    @Override
    public void run() {
        super.run();

        try {
            while (!Thread.interrupted()) {

                final KeyEvent key = videoPlayer.waitKey();

                switch (key.getKeyCode()) {

                    case VK_SPACE:
                        videoPlayer.pause();
                        break;

                    /* Second intervalls */
                    case VK_1:
                        setSkipSeconds(SkipIntervalls.SKIP_SECOND);
                        break;

                    case VK_2:
                        setSkipSeconds(SkipIntervalls.SKIP_TWO_SECONDS);
                        break;

                    case VK_3:
                        setSkipSeconds(SkipIntervalls.SKIP_FIVE_SECONDS);
                        break;

                    case VK_4:
                        setSkipSeconds(SkipIntervalls.SKIP_TEN_SECONDS);
                        break;
                    /* End setting */

                    case VK_LEFT:
                        videoPlayer.seek(skipSeconds, -1);
                        break;

                    case VK_RIGHT:
                        videoPlayer.seek(skipSeconds, 1);
                        break;

                    case VK_UP:
                        videoPlayer.seek(SkipIntervalls.SKIP_SIXTY_SECONDS, 1);
                        break;

                    case VK_DOWN:
                        videoPlayer.seek(SkipIntervalls.SKIP_SIXTY_SECONDS, -1);
                        break;


                    // Ending the editing
                    case VK_ESCAPE:
                        videoPlayer.stop();
                        break;

                    case VK_X:
                        videoPlayer.stamp();
                        break;

                    case VK_I:
                        videoPlayer.setIntro();
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

    public void setSkipSeconds(SkipIntervalls skipSeconds) {
        System.out.printf("Now skipping %s seconds %n", skipSeconds.name());
        this.skipSeconds = skipSeconds;
    }
}
