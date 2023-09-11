package de.yugata.editor.commands;


import de.yugata.editor.model.CLIArgs;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.standard.ShellComponent;

@ShellComponent
public class AudioCommand {
    private final static String DESCRIPTION = "Set the audio file path.";

    @Bean
    public CommandRegistration audio() {
        return CommandRegistration
                .builder()
                .command("audio")
                .withOption()
                .longNames("set")
                .type(String.class)
                .description("The file path to your audio.")
                .and()
                .withOption()
                .longNames("path")
                .description("The current audio file path.")
                .and()
                .group("Input")
                .description(DESCRIPTION)
                .withTarget()
                .function(ctx -> {

                    if (ctx.hasMappedOption("path")) {
                        return "Current path:" + CLIArgs.getAudioInput();
                    } else if (ctx.hasMappedOption("set")) {
                        final String path = ctx.getOptionValue("set");
                        CLIArgs.setAudioInput(path);
                        return "Audio path set: " + path;
                    }

                    return "Option not found.";
                })
                .and()
                .build();
    }


}
