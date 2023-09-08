package de.yugata.editor;

import de.yugata.editor.editor.AudioAnalyser;
import de.yugata.editor.editor.Editor;
import de.yugata.editor.model.CLIArgs;
import de.yugata.editor.playback.VideoPlayer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.opencv.opencv_java;
import picocli.CommandLine;

public class Main {


    public static void main(String... args) {
        Loader.load(opencv_java.class);
        FFmpegLogCallback.set();

        final CLIArgs cliArgs = CommandLine.populateSpec(CLIArgs.class, args);
        //Analyse the audio
        final AudioAnalyser audioAnalyser = new AudioAnalyser(cliArgs.getAudioInput());
        audioAnalyser.analyseBeats(cliArgs.getPeakThreshold());

        // Create the video player & set callback
        final VideoPlayer videoPlayer = new VideoPlayer(cliArgs.getInput(), (timestamps) -> {
            final Editor editor = new Editor(cliArgs.getInput(), cliArgs.getAudioInput(), audioAnalyser.getTimeBetweenBeats(), timestamps);
            editor.edit();
        });
        videoPlayer.run();
    }
}
