package de.yugata.editor;

import de.yugata.editor.editor.AudioAnalyser;
import de.yugata.editor.editor.Editor;
import de.yugata.editor.playback.VideoPlayer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.opencv.opencv_java;

public class Main {

    public static void main(String[] args) {
        //TODO: selection
        FFmpegLogCallback.set();
        final String video = "D:\\out\\edits\\Fight club\\Fight.Club.1999.2160p.UHD.BluRay.x265.HDR.DD5.1-Pahe.in.mkv";
        final String audio = "C:\\Users\\kursc\\Downloads\\short_s.wav";
        //Analyse the audio
        final AudioAnalyser audioAnalyser = new AudioAnalyser(audio);
        audioAnalyser.analyseBeats(0.2);

        final VideoPlayer videoPlayer = new VideoPlayer(video, (timestamps) -> {
            final Editor editor = new Editor(video, audio, audioAnalyser.getTimeBetweenBeats(), timestamps);
            editor.edit();
        });
        videoPlayer.run();
    }
}
