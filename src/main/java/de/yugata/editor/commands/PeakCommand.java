package de.yugata.editor.commands;


import de.yugata.editor.model.CLIArgs;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.standard.ShellComponent;

@ShellComponent
public class PeakCommand {
    private final static String DESCRIPTION = "Specify the audio input.";

    @Bean
    public CommandRegistration peak() {
        return CommandRegistration
                .builder()
                .command("peak")
                .withOption()
                .longNames("peak")
                .type(Double.class)
                .required()
                .defaultValue("0.3")
                .description("The file path to your audio")
                .and()
                .group("Input")
                .description(DESCRIPTION)
                .withTarget()
                .function(ctx -> {
                    final double peak = ctx.getOptionValue("peak");
                    CLIArgs.setPeakThreshold(peak);
                    return String.format("Peak threshold set to: %f. Values between 0.1 and 0.8. Default is 0.3, if too many onsets are detected adjust to 0.4 or 0.5 or more.", peak);
                })
                .and()
                .build();
    }


}
