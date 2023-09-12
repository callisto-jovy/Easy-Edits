package de.yugata.editor.editor;


import de.yugata.editor.audio.AudioAnalyser;
import de.yugata.editor.model.CLIArgs;

import java.io.File;
import java.util.*;

public class Editor {

    public static final Editor INSTANCE = new Editor();

    /**
     * A queue with all the time's between the beats. Supplied from the audio analyser after the analysis is done.
     */
    private final Queue<Double> timeBetweenBeats = new ArrayDeque<>();

    /**
     * A List with all the user's time stamps.
     */
    private final List<Double> timeStamps = new ArrayList<>();

    /**
     * The application's working directory.
     */
    private final File workingDirectory;

    private final EnumSet<EditingFlag> editingFlags;


    /**
     * Default constructor, creates a working temp.
     */
    public Editor() {
        this.editingFlags = EnumSet.of(EditingFlag.BEST_QUALITY, EditingFlag.WRITE_HDR_OPTIONS);

        this.workingDirectory = new File("editor_temp");
        workingDirectory.mkdir();
    }

    /**
     * Performs the audio analysis on the given audio input
     * with the given peak threshold & the ms-Threshold.
     * Clears the queue and adds the new queue with new beats.
     */
    public void runAudioAnalysis() {
        this.timeBetweenBeats.clear();
        this.timeBetweenBeats.addAll(AudioAnalyser.analyseBeats(CLIArgs.getAudioInput(), CLIArgs.getPeakThreshold(), CLIArgs.getMsThreshold()));
        System.out.println("We need a total of " + timeBetweenBeats.size() + " segments.");
    }

    /**
     * Runs the editing process.
     * Throws an IllegalArgumentException if the beat queue is empty.
     * Then edits the video with the given inputs.
     */
    public void runEditing() {
        if (timeBetweenBeats.isEmpty()) {
            throw new IllegalArgumentException("The audio has not been analysed or no beats have been detected, in that case adjust the threshold.");
        }

        //TODO: Output file name
        final File inputFile = new File(CLIArgs.getInput());
        final File outputFile = new File(workingDirectory, inputFile.getName() + "_edit.mp4");

        final VideoEditor editor = new VideoEditor(outputFile, CLIArgs.getInput(), CLIArgs.getAudioInput(), timeBetweenBeats, timeStamps);
        editor.edit(editingFlags);
    }

    /**
     * Checks whether the editing process is possible.
     *
     * @return true if all arguments are set & the timeBetweenBeats is not empty. Otherwise, false.
     */
    public boolean editingPossible() {
        return CLIArgs.checkArguments().isEmpty() && !timeBetweenBeats.isEmpty();
    }

    public void addTimeStamp(final double stamp) {
        this.timeStamps.add(stamp);
    }

    public void removeLastTimeStamp() {
        if (timeStamps.size() - 1 >= 0) {
            this.timeStamps.remove(timeStamps.size() - 1);
        }
    }

    public int beats() {
        return timeBetweenBeats.size();
    }

    public int stamps() {
        return timeStamps.size();
    }

    public void addOrRemoveFlag(final EditingFlag editingFlag) {
        if (editingFlags.contains(editingFlag)) {
            editingFlags.removeIf(editingFlag1 -> editingFlag1 == editingFlag);
        } else {
            editingFlags.add(editingFlag);
        }
    }

    public EnumSet<EditingFlag> getEditingFlags() {
        return editingFlags;
    }
}
