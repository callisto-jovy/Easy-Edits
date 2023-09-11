package de.yugata.editor.audio;

/*
 *              _______
 *             |__   __|
 *                | | __ _ _ __ ___  ___  ___
 *                | |/ _` | '__/ __|/ _ \/ __|
 *                | | (_| | |  \__ \ (_) \__ \
 *                |_|\__,_|_|  |___/\___/|___/
 *
 * -----------------------------------------------------------
 *
 * Tarsos is developed by Joren Six at IPEM, University Ghent
 *
 * -----------------------------------------------------------
 *
 *  Info: http://tarsos.0110.be
 *  Github: https://github.com/JorenSix/Tarsos
 *  Releases: http://0110.be/releases/Tarsos/
 *
 *  Tarsos includes some source code by various authors,
 *  for credits, license and info: see README.
 *
 */


import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import de.yugata.editor.util.AudioUtil;

import javax.sound.sampled.Clip;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public final class WaveForm extends JComponent implements ComponentListener {

    /**
     *
     */
    private static final long serialVersionUID = 3730361987954996673L;

    /**
     * Logs messages.
     */
    private static final Logger LOG = Logger.getLogger(WaveForm.class.getName());

    private Clip audioClip;
    private File audioFile;

    private double minMarkerPosition; // position in seconds
    private double maxMarkerPosition; // position in seconds

    private final List<Double> indicatorPositions = new CopyOnWriteArrayList<>();

    /**
     * A cached waveform image used to scale to the correct height and width.
     */
    private BufferedImage waveFormImage;

    /**
     * The same image scaled to the current height and width.
     */
    private BufferedImage scaledWaveFormImage;

    /**
     * The font used to draw axis labels.
     */
    private static final Font AXIS_FONT = new Font("SansSerif", Font.PLAIN, 10);

    public WaveForm() {
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent event) {
                setMarkerInPixels(event.getX(), event.getButton() != MouseEvent.BUTTON1);
            }
        });
        setMarker(0, true);
        setMarker(0, false);

        this.addComponentListener(this);
    }

    /**
     * Sets the marker position in pixels.
     *
     * @param newPosition The new position in pixels.
     */
    private void setMarkerInPixels(final int newPosition, final boolean minMarker) {
        double pixelsToSeconds = getLengthInMilliSeconds() / 1000.0 / getWidth();
        setMarker(pixelsToSeconds * newPosition, minMarker);
    }

    private boolean waveFormCreationFinished = false;

    private void setWaveFormCreationFinished(boolean isFinished) {
        waveFormCreationFinished = isFinished;
    }

    /**
     * Sets the marker position in seconds.
     *
     * @param newPosition The new position of the marker in seconds.
     * @param minMarker   True if the marker to place is the marker at the left, the minimum. False otherwise.
     */
    public void setMarker(final double newPosition, final boolean minMarker) {
        if (minMarker) {
            minMarkerPosition = newPosition;
            if (newPosition > maxMarkerPosition) {
                maxMarkerPosition = minMarkerPosition;
            }
        } else {
            maxMarkerPosition = newPosition;
            if (newPosition < minMarkerPosition) {
                minMarkerPosition = maxMarkerPosition;
            }
        }
        requestRepaint();
    }

    public void clearIndicators() {
        this.indicatorPositions.clear();
    }

    /**
     * Adds a red indicator over the waveform at the desired position relative to the ui.
     *
     * @param position the position in the audio, in s.
     */
    public void addIndicator(final double position) {
        indicatorPositions.add(position);
    }

    public double getMarker(final boolean minMarker) {
        final double markerValue;
        if (minMarker) {
            markerValue = minMarkerPosition;
        } else {
            markerValue = maxMarkerPosition;
        }
        return markerValue;
    }

    private void requestRepaint() {
        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    protected void paintComponent(Graphics g) {
        final Graphics2D graphics = (Graphics2D) g;
        initializeGraphics(graphics);
        if (waveFormImage != null && waveFormCreationFinished) {
            drawWaveForm(graphics);
        } else {
            graphics.transform(getSaneTransform());
            drawReference(graphics);
            graphics.transform(getInverseSaneTransform());
        }
        drawIndicators(graphics);
        drawMarker(graphics);
        super.paintComponent(g);
    }


    /**
     * <pre>
     *  (0,h/2)              (+w,h/2)
     *      -------------------|
     *      |                  |
     *      |                  |
     *      |                  |
     * (0,0)|------------------| (0,h/2)
     *      |                  |
     *      |                  |
     *      |                  |
     *      -------------------
     *  (0,-h/2)            (w,-h/2)
     * </pre>
     *
     * @return A transform where (0,0) is in the middle left of the screen.
     * Positive y is up, negative y down.
     */
    private AffineTransform getSaneTransform() {
        return getSaneTransform(getHeight());
    }

    private AffineTransform getSaneTransform(final float height) {
        return new AffineTransform(1.0, 0.0, 0.0, -1.0, 0, height / 2);
    }

    private AffineTransform getInverseSaneTransform() {
        return getInverseSaneTransform(getHeight());
    }

    private AffineTransform getInverseSaneTransform(final float height) {
        return new AffineTransform(1.0, 0.0, 0.0, -1.0, 0, height / 2);
    }

    private void drawIndicators(final Graphics2D graphics) {
        graphics.transform(getSaneTransform());
        for (final Double indicatorPosition : indicatorPositions) {
            final int x = (int) secondsToPixels(indicatorPosition);

            graphics.setColor(Color.red);
            graphics.drawLine(x, getHeight() / 2, x, -getHeight() / 2);
        }
        graphics.transform(getInverseSaneTransform());
    }


    private void drawMarker(final Graphics2D graphics) {
        final int minX = (int) secondsToPixels(minMarkerPosition);
        graphics.transform(getSaneTransform());
        graphics.setColor(Color.black);
        graphics.drawLine(minX, getHeight() / 2, minX, -getHeight() / 2);
        final int maxX = (int) secondsToPixels(maxMarkerPosition);
        graphics.setColor(Color.black);
        graphics.drawLine(maxX, getHeight() / 2, maxX, -getHeight() / 2);
        final Color color = new Color(0.0f, 0.0f, 0.0f, 0.15f); // black
        // graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
        // 0.5f));
        graphics.setPaint(color);
        Rectangle rectangle = new Rectangle(minX + 1, -getHeight() / 2, maxX - minX, getHeight() * 4);
        graphics.fill(rectangle);
        graphics.transform(getInverseSaneTransform());
    }

    private void initializeGraphics(final Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.BLACK);
    }

    private void drawWaveForm(final Graphics2D g) {
        // Render the cached image.
        scaleWaveFormImage();
        g.drawImage(scaledWaveFormImage, 0, 0, null);
    }

    public BufferedImage scaleWaveFormImage() {
        if (scaledWaveFormImage == null || scaledWaveFormImage.getHeight() != getHeight()
                || scaledWaveFormImage.getWidth() != getWidth()) {

            final int sourceWidth = waveFormImage.getWidth();
            final int sourceHeight = waveFormImage.getHeight();
            final int destWidth = getWidth();
            final int destHeight = getHeight();

            double xScale = (double) getWidth() / (double) sourceWidth;
            double yScale = (double) getHeight() / (double) sourceHeight;

            final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            final GraphicsDevice gd = ge.getDefaultScreenDevice();
            final GraphicsConfiguration gc = gd.getDefaultConfiguration();

            scaledWaveFormImage = gc.createCompatibleImage(destWidth, destHeight, waveFormImage.getColorModel().getTransparency());

            Graphics2D g2d = null;
            try {
                g2d = scaledWaveFormImage.createGraphics();
                g2d.getTransform();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                        RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                AffineTransform at = AffineTransform.getScaleInstance(xScale, yScale);
                g2d.drawRenderedImage(waveFormImage, at);
                g2d.transform(getInverseSaneTransform());
                drawReference(g2d);
            } finally {
                if (g2d != null) {
                    g2d.dispose();
                }
            }
            LOG.finer("Rescaled wave form image in ");
        }
        return scaledWaveFormImage;
    }

    /**
     * Draws a string on canvas using the current transform but makes sure the
     * text is not flipped.
     *
     * @param graphics The canvas.
     * @param text     The string to draw.
     * @param x        The ( by the current affine transform transformed) x location.
     * @param y        The ( by the current affine transform transformed) y location.
     */
    private void drawString(final Graphics2D graphics, final String text, final double x, final double y) {
        final Point2D source = new Point2D.Double(x, y);
        final Point2D destination = new Point2D.Double();
        final AffineTransform transform = graphics.getTransform();
        graphics.transform(getInverseSaneTransform());
        transform.transform(source, destination);
        graphics.drawString(text, (int) destination.getX(), (int) destination.getY());
        graphics.transform(getSaneTransform());
    }

    private double secondsToPixels(double seconds) {
        return seconds * 1000 * getWidth() / getLengthInMilliSeconds();
    }

    private long getLengthInMilliSeconds() {
        final long lengthInMilliSeconds;
        if (audioClip == null) {
            // default length = 200 sec;
            lengthInMilliSeconds = 200000;
        } else {
            lengthInMilliSeconds = AudioUtil.calculateLengthInMilliseconds(audioClip.getFrameLength(), audioClip.getFormat().getFrameRate());
        }
        return lengthInMilliSeconds;
    }

    /**
     * Draw reference lines on the canvas: minute markers and 1 and -1 amplitude
     * markers.
     *
     * @param g The canvas.
     */
    private void drawReference(final Graphics2D g) {
        final int width = getWidth();
        final int height = getHeight();
        final int one = (int) (height / 2 * 0.85);
        g.setColor(Color.GREEN);
        // line in center
        g.drawLine(0, 0, width, 0);
        // mark one and minus one left (y axis)
        g.drawLine(0, one, 3, one);
        g.drawLine(0, -one, 3, -one);

        g.setFont(AXIS_FONT);
        drawString(g, " 1.0", 6, one - 3);
        drawString(g, "-1.0", 6, -one - 3);

        // mark one and minus one right (y axis)
        g.drawLine(width, one, width - 3, one);
        g.drawLine(width, -one, width - 3, -one);

        // start at 10 sec;
        for (int i = 10; i < getLengthInMilliSeconds() / 1000; i += 10) {
            final int x = (int) secondsToPixels(i);
            final int y = height / 2;

            final int markerSize;
            if (i % 60 == 0) {
                markerSize = (int) (height / 2 * 0.15);
                // minute markers
                drawString(g, i / 60 + ":00", x - 8, y - markerSize - 9);
            } else {
                // marker every 10 sec
                markerSize = (int) (height / 2 * 0.05);
            }
            g.drawLine(x, y, x, y - markerSize);
            g.drawLine(x, -y, x, -y + markerSize);
        }
        g.setColor(Color.BLACK);
    }

    public void audioFileChanged(final Clip newAudioFile, final File audioFile) {
        this.audioFile = audioFile;
        this.audioClip = newAudioFile;
        this.waveFormImage = null;
        this.scaledWaveFormImage = null;
        clearIndicators();
        setWaveFormCreationFinished(false);
        createWaveFormImage();
        requestRepaint();
    }

    private void createWaveFormImage() {
        try {
            final int waveFormHeight = 200;
            final int waveFormWidth = 2000;

            this.waveFormImage = new BufferedImage(waveFormWidth, waveFormHeight, BufferedImage.TYPE_INT_RGB);

            final Graphics2D waveFormGraphics = waveFormImage.createGraphics();
            initializeGraphics(waveFormGraphics);
            waveFormGraphics.clearRect(0, 0, waveFormWidth, waveFormHeight);
            waveFormGraphics.transform(getSaneTransform(waveFormHeight));

            final float frameRate = audioClip.getFormat().getFrameRate();
            final int framesPerPixel = audioClip.getFrameLength() / waveFormWidth / 8;

            waveFormGraphics.setColor(Color.black);

            final int one = (int) (waveFormHeight / 2 * 0.85);
            final double secondsToX = 1000D * waveFormWidth / AudioUtil.calculateLengthInMilliseconds(audioClip.getFrameLength(), frameRate);


            final AudioDispatcher adp = AudioDispatcherFactory.fromFile(audioFile, framesPerPixel, 0);
            adp.addAudioProcessor(new AudioProcessor() {
                private int frame = 0;

                public void processingFinished() {
                    setWaveFormCreationFinished(true);
                    invalidate();
                    requestRepaint();
                }

                public boolean process(AudioEvent audioEvent) {
                    final float[] audioFloatBuffer = audioEvent.getFloatBuffer();
                    final double seconds = frame / frameRate;
                    frame += audioFloatBuffer.length;
                    final int x = (int) (secondsToX * seconds);
                    final int y = (int) (audioFloatBuffer[0] * one);
                    waveFormGraphics.drawLine(x, 0, x, y);
                    return true;
                }
            });

            new Thread(adp, "Waveform image builder").start();
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void componentResized(ComponentEvent e) {
        invalidate();
        requestRepaint();
    }

    @Override
    public void componentMoved(ComponentEvent e) {

    }

    @Override
    public void componentShown(ComponentEvent e) {

    }

    @Override
    public void componentHidden(ComponentEvent e) {

    }
}