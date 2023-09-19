package de.yugata.editor.commands;


import org.springframework.context.annotation.Bean;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.standard.ShellComponent;

@ShellComponent
public class IntroCommand {


    private final static String DESCRIPTION = "Set an optional intro sequence.";

    @Bean
    public CommandRegistration setIntro() {
        return CommandRegistration
                .builder()
                .command("set", "intro")
                .withOption()
                .longNames("from")
                .type(Integer.class)
                .description("The start of the intro. If not specified, this will be 0.")
                .and()
                .withOption()
                .longNames("to")
                .type(Integer.class)
                .description("The end of the intro. If not specified, this will be 0.")
                .required()
                .and()
                .group("Workflow")
                .description(DESCRIPTION)
                .withTarget()
                .function(ctx -> {
                    final int from = ctx.hasMappedOption("from") ? ctx.getOptionValue("from") : 0;
                    final int to = ctx.getOptionValue("to");



                    return String.format("The intro has been set to %d to %d", from, to);
                })
                .and()
                .build();
    }


}
