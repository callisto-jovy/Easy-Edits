package de.yugata.editor.commands;


import de.yugata.editor.audio.AudioAnalyserUI;
import de.yugata.editor.model.CLIArgs;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import java.lang.reflect.InvocationTargetException;

@ShellComponent
public class DisplayAnalyserCommand {

    @ShellMethod(value = "Show the audio wave and how the peak detection will slice the sequences.", group = "Workflow")
    @ShellMethodAvailability("analyseAvailability")

    public void displayAnalyser() {
        try {
            AudioAnalyserUI.displayDetector(CLIArgs.getAudioInput());
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public Availability analyseAvailability() {
        return CLIArgs.audioValid() ? Availability.available() : Availability.unavailable("The audio input is empty");
    }

}
