package de.yugata.easy.edits.commands;


import de.yugata.easy.edits.model.CLIArgs;
import de.yugata.easy.edits.playback.VideoPlayer;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

@ShellComponent
public class PlayVideoCommand {
    @ShellMethod(value = "Start the video playback.", group = "Workflow")
    @ShellMethodAvailability("playbackAvailability")
    public void play() {
        VideoPlayer.INSTANCE.start();
        System.out.println("Starting playback. Press 'X' to place a timestamp, press 'V' to remove the recent timestamp, press 'ESC' to exit from the playback-editing process.");
    }

    public Availability playbackAvailability() {
        return CLIArgs.inputValid() ? Availability.available() : Availability.unavailable(CLIArgs.checkArguments());
    }

}
