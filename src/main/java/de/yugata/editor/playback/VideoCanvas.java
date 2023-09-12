package de.yugata.editor.playback;

import org.bytedeco.javacv.CanvasFrame;

import javax.swing.*;
import java.awt.event.WindowAdapter;

public class VideoCanvas extends CanvasFrame {

    public VideoCanvas(final WindowAdapter closing) {
        super("Easy Editor");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.addWindowListener(closing);
    }

}
