package de.yugata.editor;

import org.bytedeco.ffmpeg.ffmpeg;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.opencv.opencv_java;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.shell.command.annotation.CommandScan;


@SpringBootApplication
@CommandScan
public class EditorApp {


    public static void main(String... args) {
        Loader.load(opencv_java.class);
        Loader.load(ffmpeg.class);

        FFmpegLogCallback.set();

        final SpringApplicationBuilder builder = new SpringApplicationBuilder(EditorApp.class)
                .bannerMode(Banner.Mode.OFF)
                .headless(false);

        final ConfigurableApplicationContext context = builder.run(args);
        /*

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

         */
    }
}
