package de.yugata.easy.edits.playback;

import org.bytedeco.javacv.CanvasFrame;

import javax.swing.*;
import java.awt.event.WindowAdapter;

public class VideoCanvas extends CanvasFrame {

    public VideoCanvas() {
        super("Easy Editor");
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

}
