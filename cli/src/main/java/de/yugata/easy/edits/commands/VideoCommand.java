package de.yugata.easy.edits.commands;


import de.yugata.easy.edits.model.CLIArgs;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.standard.ShellComponent;

@ShellComponent
public class VideoCommand {
    private final static String DESCRIPTION = "Specify the video input.";

    @Bean
    public CommandRegistration video() {
        return CommandRegistration
                .builder()
                .command("video")
                .withOption()
                .longNames("set")
                .type(String.class)
                .description("The file path to your video.")
                .and()
                .withOption()
                .longNames("path")
                .description("The current video file path.")
                .and()
                .group("Video")
                .description(DESCRIPTION)
                .withTarget()
                .function(ctx -> {

                    if (ctx.hasMappedOption("path")) {
                        return "Current path:" + CLIArgs.getInput();
                    } else if (ctx.hasMappedOption("set")) {
                        final String path = ctx.getOptionValue("set");
                        CLIArgs.setInput(path);
                        return "Video path set: " + path;
                    }

                    return "Option not found.";
                })
                .and()
                .build();
    }


}
