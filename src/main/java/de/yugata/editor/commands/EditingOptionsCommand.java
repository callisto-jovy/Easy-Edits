package de.yugata.editor.commands;


import de.yugata.editor.editor.EditingFlag;
import de.yugata.editor.editor.Editor;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.standard.ShellComponent;

import java.io.OutputStream;
import java.util.Arrays;

@ShellComponent
public class EditingOptionsCommand {

    private final static String DESCRIPTION = "Specify the options for rendering the edit.";

    @Bean
    public CommandRegistration options() {
        return CommandRegistration
                .builder()
                .command("option")
                .withOption()
                .longNames("set")
                .type(String.class)
                .description("Set a flag for rendering the edit.")
                .and()
                .withOption()
                .longNames("see")
                .description("Prints the state of all the editing flags.")
                .and()
                .withOption()
                .longNames("all")
                .description("Prints all the available editing flags.")
                .and()
                .group("Video")
                .description(DESCRIPTION)
                .withTarget()
                .function(ctx -> {

                    if (ctx.hasMappedOption("set")) {
                        final String set = ctx.getOptionValue("set");
                        Editor.INSTANCE.addOrRemoveFlag(EditingFlag.valueOf(set));

                        return String.format("Added flag %s to existing flags!", set);
                    } else if (ctx.hasMappedOption("see")) {
                        return Editor.INSTANCE.getEditingFlags();
                    } else if (ctx.hasMappedOption("all")) {
                        return Arrays.toString(EditingFlag.values());
                    }

                    return "Option not found.";
                })
                .and()
                .build();
    }

}
