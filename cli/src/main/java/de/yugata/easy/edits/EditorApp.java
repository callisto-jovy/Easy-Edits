package de.yugata.easy.edits;

import org.bytedeco.ffmpeg.ffmpeg;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.opencv.opencv_java;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.shell.command.annotation.CommandScan;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_PANIC;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;


@SpringBootApplication
@CommandScan
public class EditorApp {


    public static void main(String... args) {
        Loader.load(opencv_java.class);
        Loader.load(ffmpeg.class);


        final SpringApplicationBuilder builder = new SpringApplicationBuilder(EditorApp.class)
                .bannerMode(Banner.Mode.OFF)
                .headless(false);

        final ConfigurableApplicationContext context = builder.run(args);
    }
}
