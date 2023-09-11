package de.yugata.editor.commands;


import de.yugata.editor.model.CLIArgs;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.standard.ShellComponent;

@ShellComponent
public class MsThresholdCommand {

    //TODO: Description
    private final static String DESCRIPTION = "The minimum time passed between onsets in ms.";

    @Bean
    public CommandRegistration msThreshold() {
        return CommandRegistration
                .builder()
                .command("ms")
                .withOption()
                .longNames("threshold")
                .type(Double.class)
                .required()
                .defaultValue("0")
                .description("The milliseconds that have to pass between beats.")
                .and()
                .group("Audio")
                .description(DESCRIPTION)
                .withTarget()
                .function(ctx -> {
                    final double threshold = ctx.getOptionValue("threshold");
                    CLIArgs.setMsThreshold(threshold);
                    return "MS threshold set to: " + threshold;
                })
                .and()
                .build();
    }


}
