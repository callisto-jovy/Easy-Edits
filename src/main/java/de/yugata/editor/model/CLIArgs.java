package de.yugata.editor.model;

import picocli.CommandLine;

public class CLIArgs {


    @CommandLine.Option(names = {"--input", "-i"}, required = true)
    private String input;

    @CommandLine.Option(names = {"--audio", "-a"}, required = true)
    private String audioInput;

    @CommandLine.Option(names = {"--peak", "-p"}, defaultValue = "0.2")
    private double peakThreshold;

    public String getInput() {
        return input;
    }

    public String getAudioInput() {
        return audioInput;
    }

    public double getPeakThreshold() {
        return peakThreshold;
    }
}
