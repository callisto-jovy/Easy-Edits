package de.yugata.easy.edits.util;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

import static de.yugata.easy.edits.util.FFmpegUtil.FFMPEG_BIN;
import static de.yugata.easy.edits.util.FFmpegUtil.RESOURCE_DIRECTORY;

public class AudioUtil {

    // private constructor to restrict object creation
    private AudioUtil() {

    }


    /**
     * @param audioPath
     * @return
     */

    public static String convertToWav(final String audioPath) {
        try {
            final File directTemp = File.createTempFile("temp_conversion", ".wav");

            // Convert the audio to a wav
            final FFmpeg fFmpeg = FFmpeg.atPath(FFMPEG_BIN.toPath())
                    .addInput(UrlInput.fromUrl(audioPath))
                    .addOutput(UrlOutput.toPath(directTemp.toPath()));

            fFmpeg.execute();
            return directTemp.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }


    public static long estimateEditLengthInMicros(final String audioPath) {
        // This limits us to AIFF, AU and WAV files only, however, it eliminates the need for the ffmpeg grabber, which reduces code complexity.
        try (final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(audioPath))) {
            final AudioFormat audioFormat = audioInputStream.getFormat();

            return (long) ((audioInputStream.getFrameLength() / audioFormat.getFrameRate()) * 1000000L);
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

}
