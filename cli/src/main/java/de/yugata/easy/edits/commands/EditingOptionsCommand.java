package de.yugata.easy.edits.commands;


import de.yugata.easy.edits.editor.edit.EditingFlag;
import de.yugata.easy.edits.editor.Editor;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.standard.ShellComponent;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                        return formatFlags(Editor.INSTANCE.getEditingFlags().stream());
                    } else if (ctx.hasMappedOption("all")) {
                        return formatFlags(Arrays.stream(EditingFlag.values()));
                    }
                    return "Option not found.";
                })
                .and()
                .build();
    }

    private String formatFlags(final Stream<EditingFlag> flags) {
        return flags
                .map(this::formatFlag)
                .collect(Collectors.joining("\n"));
    }

    private String formatFlag(final EditingFlag flag) {
        return String.format("%s : %s", flag.name(), flag.getDescription());
    }

}
