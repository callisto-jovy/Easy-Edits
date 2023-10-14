package de.yugata.easy.edits.model;


import java.io.File;
import java.util.function.Function;

public class CLIArgs {


    private static String input;
    private static String audioInput;
    private static double peakThreshold;
    private static double msThreshold;


    public static String checkArguments() {
        if (input == null || input.isEmpty() || !new File(input).exists()) {
            return "Input file does not exist. Aborting!";
        }

        if (audioInput == null || audioInput.isEmpty() || !new File(audioInput).exists()) {
            return "Audio file does not exist. Aborting!";
        }

        if (peakThreshold <= 0 || peakThreshold >= 1) {
            return "Peak threshold not in range.";
        }

        return "";
    }

    public static boolean audioValid() {
        return audioInput != null && !audioInput.isEmpty() && new File(audioInput).exists();
    }


    public static boolean inputValid() {
        return input != null && !input.isEmpty() && new File(input).exists();
    }


    public static String getInput() {
        return input;
    }

    public static void setInput(String input) {
        CLIArgs.input = input;
    }

    public static String getAudioInput() {
        return audioInput;
    }

    public static void setAudioInput(String audioInput) {
        CLIArgs.audioInput = audioInput;
    }

    public static double getPeakThreshold() {
        return peakThreshold;
    }

    public static void setPeakThreshold(double peakThreshold) {
        CLIArgs.peakThreshold = peakThreshold;
    }

    public static double getMsThreshold() {
        return msThreshold;
    }

    public static void setMsThreshold(double msThreshold) {
        CLIArgs.msThreshold = msThreshold;
    }
}
