package de.yugata.editor;

import de.yugata.editor.editor.AudioAnalyser;
import de.yugata.editor.editor.Editor;
import de.yugata.editor.model.CLIArgs;
import de.yugata.editor.playback.VideoPlayer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.opencv.opencv_java;
import picocli.CommandLine;

import java.util.Queue;

public class Main {


    public static void main(String... args) {
        Loader.load(opencv_java.class);
        FFmpegLogCallback.set();

        final CLIArgs cliArgs = CommandLine.populateSpec(CLIArgs.class, args);
        //Analyse the audio
        final AudioAnalyser audioAnalyser = new AudioAnalyser(cliArgs.getAudioInput());
        audioAnalyser.analyseBeats(cliArgs.getPeakThreshold());

        // Queue of needed beat times
        final Queue<Double> timeBetweenBeats = audioAnalyser.getTimeBetweenBeats();

        // Create the video player & set callback
        final VideoPlayer videoPlayer = new VideoPlayer(cliArgs.getInput(), timeBetweenBeats.size(), (timestamps) -> {
            final Editor editor = new Editor(cliArgs.getInput(), cliArgs.getAudioInput(), timeBetweenBeats, timestamps);
            editor.edit();
        });
        videoPlayer.run();
    }
}
