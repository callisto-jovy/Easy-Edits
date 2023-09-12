package de.yugata.editor.commands;


import de.yugata.editor.model.CLIArgs;
import org.bytedeco.javacpp.Loader;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import java.io.IOException;

@ShellComponent
public class ProbeHDRCommand {

    /*
TODO: FIX THIS SHIT

     */


    @ShellMethod(value = "Probe the HDR availability of the input.", group = "Workflow")
    @ShellMethodAvailability("probeHDRAvailability")
    public void probeHDR() {
        final String ffprobe = Loader.load(org.bytedeco.ffmpeg.ffprobe.class);
        final ProcessBuilder pb = new ProcessBuilder(ffprobe, CLIArgs.getInput());

        try {
            pb.inheritIO().start().waitFor();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public Availability probeHDRAvailability() {
        return CLIArgs.inputValid() ? Availability.available() : Availability.unavailable(CLIArgs.checkArguments());
    }


}
