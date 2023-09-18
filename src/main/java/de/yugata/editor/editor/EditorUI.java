package de.yugata.editor.editor;

import de.yugata.editor.playback.VideoPlayer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import static de.yugata.editor.editor.EditorUI.LINE_HEIGHT;
import static de.yugata.editor.editor.EditorUI.SIDE_OFFSET;
import static de.yugata.editor.util.SWTUtil.color;
import static de.yugata.editor.util.SWTUtil.getColor;
import static org.eclipse.swt.SWT.*;

public class EditorUI {

    public static final int SIDE_OFFSET = 5;
    public static final int LINE_HEIGHT = 25;


    public static void main(String[] args) {
        displayEditor();
    }

    public static void displayEditor() {
        final Display display = new Display();
        final Shell shell = new Shell(display);

        final OnPaintListener onPaintListener = new OnPaintListener();

        shell.setText("Easy Editor");
        shell.addListener(SWT.Paint, event -> {
            onPaintListener.handleEvent(event);
        });

        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }


}

class OnPaintListener implements Listener {


    @Override
    public void handleEvent(Event event) {
        final GC gc = event.gc;
        final Rectangle bounds = event.getBounds();
        final int halfHeight = bounds.height / 2;
        // Draw the line

        color(event, COLOR_BLUE, unused -> {
            gc.setForeground(getColor(event, COLOR_BLUE));
            gc.setLineWidth(2);
            gc.drawLine(SIDE_OFFSET, halfHeight, bounds.width - SIDE_OFFSET, halfHeight);
        });


        color(event, COLOR_DARK_BLUE, unused -> {
            gc.drawLine(SIDE_OFFSET, halfHeight - LINE_HEIGHT, SIDE_OFFSET, halfHeight + LINE_HEIGHT);
            gc.drawLine(bounds.width - SIDE_OFFSET, halfHeight - LINE_HEIGHT, bounds.width - SIDE_OFFSET, halfHeight + LINE_HEIGHT);
        });

        // Draw the start and end text
        color(event, COLOR_BLACK, unused -> {
            gc.drawText("0.0", SIDE_OFFSET, halfHeight + LINE_HEIGHT);

        });

        color(event, COLOR_GRAY, unused -> {
            final int lenBeats = Editor.INSTANCE.beats();

            for (int i = 0; i < lenBeats; i++) {
                // Draw beat line relative to the screen position
                final int xPos = getPosFromIndex(i, lenBeats, bounds.width);

                gc.drawLine(xPos, halfHeight - LINE_HEIGHT, xPos, halfHeight + LINE_HEIGHT);
            }

            for (int i = 0; i < Editor.INSTANCE.stamps(); i++) {
                final double timeStamp = Editor.INSTANCE.timeStampAt(i);
                final int xPos = getPosFromIndex(i, lenBeats, bounds.width);
                final int yPos = halfHeight - LINE_HEIGHT - 5;

                final Rectangle stampBounds = new Rectangle(xPos, yPos, 4, 4);

                gc.setForeground(getColor(event, COLOR_DARK_MAGENTA));
                gc.drawRectangle(stampBounds);

                final TimeStampHoverListener hoverListener = new TimeStampHoverListener(i, stampBounds);
                event.display.getActiveShell().addMouseListener(hoverListener);
            }
        });


        gc.dispose();
    }

    private int getPosFromIndex(final int index, final int length, final int width) {
        return (int) ((index / (double) length) * (width - SIDE_OFFSET * 2) + SIDE_OFFSET);
    }

}

class TimeStampHoverListener implements MouseListener {

    private final int index;
    private final Rectangle bounds;

    TimeStampHoverListener(int index, final Rectangle bounds) {
        this.index = index;
        this.bounds = bounds;

    }


    @Override
    public void mouseDoubleClick(org.eclipse.swt.events.MouseEvent e) {

    }

    @Override
    public void mouseDown(org.eclipse.swt.events.MouseEvent e) {
        if (bounds.contains(e.x, e.y)) {
            if (e.button == 1) {
                // Open the playback if it isn't playing already...
                if (!VideoPlayer.INSTANCE.isRunning()) {
                    VideoPlayer.INSTANCE.start();
                }
                final double timeStamp = Editor.INSTANCE.timeStampAt(index);
                VideoPlayer.INSTANCE.setTimeStamp((long) timeStamp);
            } else if (e.button == 2) {
                //TODO: Remove the timestamp & reroute the next recorded stamp to the empty spot
                Editor.INSTANCE.removeStampAt(index);
            }
        }
    }

    @Override
    public void mouseUp(org.eclipse.swt.events.MouseEvent e) {

    }
}
