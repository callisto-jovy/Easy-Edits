package de.yugata.editor.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.yugata.editor.editor.Editor;
import de.yugata.editor.model.CLIArgs;
import org.apache.commons.io.FileUtils;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static de.yugata.editor.util.JsonUtil.GSON;

@ShellComponent
public class EditorStateCommand {

    private static final File SAVE_FILE = new File(Editor.WORKING_DIRECTORY, "saved_state.json");


    @ShellMethod(value = "Exports all the settings to load again.", group = "Workflow")
    @ShellMethodAvailability("exportAvailability")
    public void exportSequences() {

        final JsonObject root = new JsonObject();
        // Add input / audio
        root.addProperty("source_video", CLIArgs.getInput());
        root.addProperty("source_audio", CLIArgs.getAudioInput());
        root.addProperty("peak_threshold", CLIArgs.getPeakThreshold());
        root.addProperty("ms_threshold", CLIArgs.getMsThreshold());
        // Add editor object
        root.add("editor_state", Editor.INSTANCE.toJson());

        try {
            FileUtils.write(SAVE_FILE, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ShellMethod(value = "Imports all the settings to load again.", group = "Workflow")
    public void importSequences() {
        try {
            final String json = FileUtils.readFileToString(SAVE_FILE, StandardCharsets.UTF_8);
            final JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            CLIArgs.setInput(root.get("source_video").getAsString());
            CLIArgs.setAudioInput(root.get("source_audio").getAsString());
            CLIArgs.setPeakThreshold(root.get("peak_threshold").getAsDouble());
            CLIArgs.setMsThreshold(root.get("ms_threshold").getAsDouble());

            final JsonObject editorState = root.getAsJsonObject("editor_state");
            Editor.INSTANCE.fromJson(editorState);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public Availability exportAvailability() {
        return Editor.INSTANCE.editingPossible() ? Availability.available() : Availability.unavailable(CLIArgs.checkArguments());
    }
}
