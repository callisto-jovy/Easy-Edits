package de.yugata.editor.commands;


import de.yugata.editor.editor.Editor;
import de.yugata.editor.model.CLIArgs;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

@ShellComponent
public class AnalyseAudioCommand {

    @ShellMethod(value = "Analyse the given audio.", group = "Workflow")
    @ShellMethodAvailability("analyseAvailability")
    public void analyseAudio() {
        Editor.INSTANCE.runAudioAnalysis();
    }

    public Availability analyseAvailability() {
        return CLIArgs.audioValid() ? Availability.available() : Availability.unavailable("The audio input is empty");
    }

}
