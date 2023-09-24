package de.yugata.editor.audio;

import be.tarsos.dsp.beatroot.*;
import be.tarsos.dsp.onsets.OnsetHandler;

import java.util.Iterator;

/**
 * Forms a bridge between the BeatRoot beat tracking system and an
 * interchangeable onset detector. The beat tracker does not work in real-time.
 * First all onsets need to be detected. In a post-processing step a beat
 * estimation is done using reoccurring inter onset intervals (IOI's). To return
 * the time of the beats an OnsetHandler is abused.
 *
 * @author Joren Six, modified for this project.
 */
public class BeatRootOnsetEventHandler implements OnsetHandler {

    private final EventList onsetList = new EventList();

    @Override
    public void handleOnset(double time, double salience) {
        Event e = newEvent(time, 0);
        e.salience = salience;
        onsetList.add(e);
    }


    /**
     * Creates a new Event object representing an onset or beat.
     *
     * @param time    The time of the beat in milliseconds
     * @param beatNum The index of the beat or onset.
     * @return The Event object representing the beat or onset.
     */
    private Event newEvent(double time, int beatNum) {
        return new Event(time, time, time, 56, 64, beatNum, 0, 1);
    }

    /**
     * Guess the beats using the populated list of onsets.
     *
     * @param beatHandler Use this handler to get the time of the beats. The salience of
     *                    the beat is not calculated: -1 is returned.
     */
    public void trackBeats(OnsetHandler beatHandler) {
        AgentList agents = null;
        // tempo not given; use tempo induction
        agents = Induction.beatInduction(onsetList);
        agents.beatTrack(onsetList, -1);
        Agent best = agents.bestAgent();
        if (best != null) {
            best.fillBeats(-1.0);
            EventList beats = best.events;
            Iterator<Event> eventIterator = beats.iterator();
            while (eventIterator.hasNext()) {
                Event beat = eventIterator.next();
                double time = beat.keyDown;
                beatHandler.handleOnset(time, -1);
            }
        } else {
            System.err.println("No best agent");
        }
    }

}