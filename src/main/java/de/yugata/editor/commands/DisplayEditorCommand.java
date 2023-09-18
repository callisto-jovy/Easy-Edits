package de.yugata.editor.commands;


import de.yugata.editor.audio.AudioAnalyserUI;
import de.yugata.editor.editor.Editor;
import de.yugata.editor.editor.EditorUI;
import de.yugata.editor.model.CLIArgs;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import java.lang.reflect.InvocationTargetException;

@ShellComponent
public class DisplayEditorCommand {

    @ShellMethod(value = "Shows the editor.", group = "Workflow")
    @ShellMethodAvailability("editorAvailability")
    public void displayEditor() {
        EditorUI.displayEditor();
    }

    public Availability editorAvailability() {
        return Editor.INSTANCE.editingPossible() ? Availability.available() : Availability.unavailable(CLIArgs.checkArguments());
    }
}
