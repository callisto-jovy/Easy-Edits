package de.yugata.editor.audio;


import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;


public class AudioAnalyserUI extends JFrame {

    private double msThreshold;
    private double threshold;


    private final WaveForm waveForm;


    // The File selected
    private File audioFile;

    public AudioAnalyserUI(final String audioPath) {
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setTitle("Percussion Detector");


        final JSlider sensitivitySlider = initializeSensitivitySlider();
        final JSlider thresholdSlider = initializeThresholdSlider();
        final JPanel params = new JPanel(new GridLayout(0, 1));
        params.setBorder(new TitledBorder("Set the algorithm parameters"));

        JLabel label = new JLabel("Threshold");
        label.setToolTipText("Energy rise within a frequency bin necessary to count toward broadband total (dB).");
        params.add(label);
        params.add(thresholdSlider);

        label = new JLabel("Min Intervall (ms)");
        label.setToolTipText("The minimum time passed between onsets in ms.");
        params.add(label);
        params.add(sensitivitySlider);


        final JPanel paramsAndInputPanel = new JPanel(new GridLayout(1, 0));
        paramsAndInputPanel.add(params);


        this.add(paramsAndInputPanel, BorderLayout.NORTH);
        this.add(waveForm = new WaveForm(), BorderLayout.CENTER);

        this.addClips(audioPath);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("msThreshold = " + msThreshold);
                System.out.println("threshold = " + (threshold / 100));
                super.windowClosing(e);
            }
        });
    }

    private JSlider initializeThresholdSlider() {
        final JSlider thresholdSlider = new JSlider(1, 100);
        thresholdSlider.setValue((int) threshold);
        thresholdSlider.setPaintLabels(true);
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setMajorTickSpacing(1);
        thresholdSlider.setMinorTickSpacing(1);
        thresholdSlider.addChangeListener(e -> {
            final JSlider source = (JSlider) e.getSource();
            if (!source.getValueIsAdjusting()) {
                threshold = source.getValue();
                update();
            }
        });
        return thresholdSlider;
    }

    private JSlider initializeSensitivitySlider() {
        final JSlider sensitivitySlider = new JSlider(0, 5000);
        sensitivitySlider.setValue((int) msThreshold);
        sensitivitySlider.setPaintLabels(true);
        sensitivitySlider.setPaintTicks(true);
        sensitivitySlider.setMajorTickSpacing(100);
        sensitivitySlider.setMinorTickSpacing(10);

        sensitivitySlider.addChangeListener(e -> {
            final JSlider source = (JSlider) e.getSource();
            if (!source.getValueIsAdjusting()) {
                msThreshold = source.getValue();
                update();
            }

        });
        return sensitivitySlider;
    }

    private void addClips(final String audioPath) {
        this.audioFile = new File(audioPath);


        try (final BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(audioFile.toPath()));
             final AudioInputStream stream = AudioSystem.getAudioInputStream(inputStream)) {

            // load the sound into memory (a Clip)
            final DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat());

            final Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(stream);

            waveForm.audioFileChanged(clip, audioFile);
            waveForm.setMarker(0, true);
            waveForm.setMarker(0, false);
            waveForm.clearIndicators();

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }


    private void update() {
        waveForm.clearIndicators();

        final double[] lastMs = {0};

        AudioAnalyser.analyseBeats(audioFile.getAbsolutePath(), threshold / 100, (timeStamp, salience) -> {
            final double time = (timeStamp * 1000);
            final double msPassed = time - lastMs[0];

            if (msPassed >= msThreshold) {
                waveForm.addIndicator(timeStamp);
                lastMs[0] = time;

            }
        });

        waveForm.repaint();
    }


    public static void displayDetector(final String audioPath) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                //ignore failure to set default look and feel;
            }
            final JFrame frame = new AudioAnalyserUI(audioPath);
            frame.pack();
            frame.setSize(640, 480);
            frame.setVisible(true);
        });
    }
}