package de.yugata.editor.audio;


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

    public static Queue<Double> analyseBeats(final String audioInput, final double peakThreshold) {
        validatePath(audioInput);

        final Queue<Double> timeBetweenBeats = new ArrayDeque<>();


        // This limits us to AIFF, AU and WAV files only, however, it eliminates the need for the ffmpeg grabber, which reduces code complexity.
        try (final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(audioInput))) {
            final AudioFormat audioFormat = audioInputStream.getFormat();


            // Sample rate, the buffer's size and overlap between buffers
            final float sampleRate = audioFormat.getSampleRate(); //We get the sample rate from the file
            final int bufferSize = 512, // Just hardcoded
                    bufferOverlap = 256; // We don't need any overlap.

            // Don't continue from here, just abort.
            if (sampleRate == -1)
                throw new RuntimeException("Samplerate cannot be negative.");


            //
            final JVMAudioInputStream audioStream = new JVMAudioInputStream(audioInputStream);
            final AudioDispatcher dispatcher = new AudioDispatcher(audioStream, bufferSize, bufferOverlap);


            final ComplexOnsetDetector detector = new ComplexOnsetDetector(bufferSize, peakThreshold);
            final BeatRootOnsetEventHandler handler = new BeatRootOnsetEventHandler();
            detector.setHandler(handler);
            dispatcher.addAudioProcessor(detector);
            dispatcher.run();

            final double[] lastMs = {0};

            handler.trackBeats((timeStamp, salience) -> {
                final double time = (timeStamp * 1000);
                final double msPassed = time - lastMs[0];

                timeBetweenBeats.add(msPassed);

                lastMs[0] = time;
            });


            LOG.info("We need a total of " + timeBetweenBeats.size() + " segments.");
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }

        return timeBetweenBeats;
    }

    /*
     * Checks whether the input exists & creates the output file
     */
    private static void validatePath(final String inputPath) {
        final File inputFile = new File(inputPath);

        // Check the input's validity
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("The input file supplied does not exist. Aborting!");
        }
    }
}
