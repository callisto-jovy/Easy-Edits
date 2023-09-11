package de.yugata.editor.editor;


import de.yugata.editor.model.CLIArgs;
import de.yugata.editor.audio.AudioAnalyser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class Editor {

    public static final Editor INSTANCE = new Editor();

    private final Queue<Double> timeBetweenBeats = new ArrayDeque<>();
    private final List<Double> timeStamps = new ArrayList<>();


    public Editor() {
    }


    public void runAudioAnalysis() {
        this.timeBetweenBeats.clear();
        this.timeBetweenBeats.addAll(AudioAnalyser.analyseBeats(CLIArgs.getAudioInput(), CLIArgs.getPeakThreshold()));
    }

    public void runEditing() {
        if (timeBetweenBeats.isEmpty()) {
            throw new IllegalArgumentException("The audio has not been analysed or no beats have been detected, in that case adjust the threshold.");
        }

        final VideoEditor editor = new VideoEditor(CLIArgs.getInput(), CLIArgs.getAudioInput(), timeBetweenBeats, timeStamps);
        editor.edit();
    }


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
}
