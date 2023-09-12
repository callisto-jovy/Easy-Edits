package de.yugata.editor.commands;


import de.yugata.editor.model.CLIArgs;
import de.yugata.editor.playback.VideoPlayer;
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
