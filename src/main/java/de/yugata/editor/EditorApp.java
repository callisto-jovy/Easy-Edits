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
    }
}
