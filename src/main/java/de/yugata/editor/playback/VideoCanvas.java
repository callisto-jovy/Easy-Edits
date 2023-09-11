package de.yugata.editor.playback;

import org.bytedeco.javacv.CanvasFrame;

import java.awt.event.WindowAdapter;

public class VideoCanvas extends CanvasFrame {

    public VideoCanvas(final WindowAdapter closing) {
        super("Easy Editor");
        this.addWindowListener(closing);
    }

}
