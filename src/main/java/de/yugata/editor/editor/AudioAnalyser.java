package de.yugata.editor.editor;


import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.beatroot.BeatRootOnsetEventHandler;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Logger;

public class AudioAnalyser {

    private static final Logger LOG = Logger.getLogger(AudioAnalyser.class.getName());
    /**
     * The input path of the supplied audio. From the given arguments
     */
    private final String inputPath;
    /**
     * A queue with all the times between the beats which indicate a sequence switch
     */
    private final Queue<Double> timeBetweenBeats = new ArrayDeque<>();

    public AudioAnalyser(final String audioInput) {
        this.inputPath = audioInput;
        this.validatePath();
    }


    public void analyseBeats(final double peakThreshold) {
        // This limits us to AIFF, AU and WAV files only, however, it eliminates the need for the ffmpeg grabber, which reduces code complexity.
        try (final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(inputPath))) {
            final AudioFormat audioFormat = audioInputStream.getFormat();


            // Sample rate, the buffer's size and overlap between buffers
            final float sampleRate = audioFormat.getSampleRate(); //We get the sample rate from the file
            final int bufferSize = 2048, // Just hardcoded
                    bufferOverlap = 0; // We don't need any overlap.

            // Don't continue from here, just abort.
            if (sampleRate == -1)
                throw new RuntimeException("Samplerate cannot be negative.");


            final JVMAudioInputStream audioStream = new JVMAudioInputStream(audioInputStream);
            final AudioDispatcher dispatcher = new AudioDispatcher(audioStream, bufferSize, bufferOverlap);


            final ComplexOnsetDetector detector = new ComplexOnsetDetector(bufferSize, peakThreshold);
            final BeatRootOnsetEventHandler handler = new BeatRootOnsetEventHandler();
            detector.setHandler(handler);
            dispatcher.addAudioProcessor(detector);
            dispatcher.run();

            handler.trackBeats((time, salience) -> handleDetection(time));


            LOG.info("We need a total of " + timeBetweenBeats.size() + " segments.");
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    double lastMs = 0;


    private void handleDetection(final double timeStamp) {
        final double time = (timeStamp * 1000);
        final double msPassed = time - lastMs;
        timeBetweenBeats.add(msPassed);
        lastMs = time;
    }

    /*
     * Checks whether the input exists & creates the output file
     */
    private void validatePath() {
        final File inputFile = new File(inputPath);

        // Check the input's validity
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("The input file supplied does not exist. Aborting!");
        }
    }

    public String getInputPath() {
        return inputPath;
    }

    public Queue<Double> getTimeBetweenBeats() {
        return timeBetweenBeats;
    }
}
