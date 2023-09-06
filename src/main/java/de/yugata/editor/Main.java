package de.yugata.editor;

import de.yugata.editor.editor.AudioAnalyser;
import de.yugata.editor.editor.Editor;
import de.yugata.editor.model.CLIArgs;
import de.yugata.editor.playback.VideoPlayer;
import org.bytedeco.javacv.FFmpegLogCallback;
import picocli.CommandLine;

public class Main {


    public static void main(String... args) {
        final CLIArgs cliArgs = CommandLine.populateSpec(CLIArgs.class, args);

        FFmpegLogCallback.set();
        //Analyse the audio
        final AudioAnalyser audioAnalyser = new AudioAnalyser(cliArgs.getAudioInput());
        audioAnalyser.analyseBeats(cliArgs.getPeakThreshold());

        final VideoPlayer videoPlayer = new VideoPlayer(cliArgs.getInput(), (timestamps) -> {
            final Editor editor = new Editor(cliArgs.getInput(), cliArgs.getAudioInput(), audioAnalyser.getTimeBetweenBeats(), timestamps);
            editor.edit();
        });
        videoPlayer.run();
    }
}
