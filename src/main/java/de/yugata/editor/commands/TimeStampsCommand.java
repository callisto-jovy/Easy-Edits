package de.yugata.editor.commands;


import de.yugata.editor.editor.Editor;
import de.yugata.editor.playback.VideoPlayer;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.standard.ShellComponent;


@ShellComponent
public class TimeStampsCommand {

    private static final String DESCRIPTION = "";

    @Bean
    public CommandRegistration timeStamps() {
        return CommandRegistration
                .builder()
                .command("stamp")
                .withOption()
                .longNames("remove")
                .type(Integer.class)
                .description("Remove a certain timestamp")
                .and()
                .withOption()
                .longNames("all")
                .description("Prints all the timestamps.")
                .and()
                .withOption()
                .longNames("length")
                .description("Prints the length of all the list.")
                .and()
                .withOption()
                .longNames("override")
                .description("Override the next missing timestamp.")
                .and()
                .withOption()
                .longNames("visit")
                .description("Visit any timestamps starting position at its index")
                .type(Integer.class)
                .and()
                .group("Workflow")
                .description(DESCRIPTION)
                .withTarget()
                .function(ctx -> {
                    if (ctx.hasMappedOption("length")) {
                        return "Total amount of stamps: " + Editor.INSTANCE.stamps();

                    } else if (ctx.hasMappedOption("all")) {
                        return getTimeStamps();

                    } else if (ctx.hasMappedOption("remove")) {
                        final int index = ctx.getOptionValue("remove");
                        Editor.INSTANCE.removeStampAt(index);
                        return String.format("Removed stamp at %d. If nothing happened, the index was out of bounds", index);

                    } else if (ctx.hasMappedOption("override")) {
                        VideoPlayer.INSTANCE.start();
                        return "Starting player. The next placed stamp will override the first removed stamp in the list.";

                    } else if (ctx.hasMappedOption("visit")) {
                        final int index = ctx.getOptionValue("visit");
                        final Long stamp = Editor.INSTANCE.timeStampAt(index);
                        if (stamp == null) {
                            return "This stamp currently is marked as null or is out of bounds.";
                        }

                        VideoPlayer.INSTANCE.seek(Editor.INSTANCE.timeStampAt(index));
                        return "Seeking to stamp";
                    }
                    return "Unknown option.";
                })
                .and()
                .build();
    }


    private String getTimeStamps() {
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < Editor.INSTANCE.stamps(); i++) {
            builder.append(i)
                    .append(":")
                    .append(Editor.INSTANCE.timeStampAt(i))
                    .append("\n");

        }

        return builder.toString();
    }


}
