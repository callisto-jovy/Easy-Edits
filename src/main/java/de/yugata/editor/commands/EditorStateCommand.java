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
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static de.yugata.editor.util.JsonUtil.GSON;

@ShellComponent
public class EditorStateCommand {

    private static final File SAVE_FILE = new File(Editor.WORKING_DIRECTORY, "saved_state.json");


    @ShellMethod(key = {"export"}, value = "Exports all the settings to load again.", group = "Workflow")
    @ShellMethodAvailability("exportAvailability")
    public void exportSequences(@ShellOption(value = {"file"}) String filePath) {

        final JsonObject root = new JsonObject();
        // Add input / audio
        root.addProperty("source_video", CLIArgs.getInput());
        root.addProperty("source_audio", CLIArgs.getAudioInput());
        root.addProperty("peak_threshold", CLIArgs.getPeakThreshold());
        root.addProperty("ms_threshold", CLIArgs.getMsThreshold());
        // Add editor object
        root.add("editor_state", Editor.INSTANCE.toJson());


        final File fileOutput = filePath == null ? SAVE_FILE : new File(filePath).canWrite() ? new File(filePath) : SAVE_FILE;
        try {
            FileUtils.write(fileOutput, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ShellMethod(key = {"import"}, value = "Imports all the settings to load again.", group = "Workflow")
    public void importSequences(@ShellOption(value = {"file"}) String filePath) {
        try {
            final String json = FileUtils.readFileToString(filePath == null ? SAVE_FILE : new File(filePath), StandardCharsets.UTF_8);
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
