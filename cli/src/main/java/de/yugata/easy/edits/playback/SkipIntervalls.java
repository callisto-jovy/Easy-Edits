package de.yugata.easy.edits.playback;

public enum SkipIntervalls {
    SKIP_SECOND(1000000L),
    SKIP_TEN_SECONDS(10L * SKIP_SECOND.intervall),

    SKIP_FIVE_SECONDS(5L * SKIP_SECOND.intervall),

    SKIP_SIXTY_SECONDS(60L * SKIP_SECOND.intervall),

    SKIP_TWO_SECONDS(2L * SKIP_SECOND.intervall);

    private final long intervall;


    SkipIntervalls(final long intervall) {
        this.intervall = intervall;
    }

    public long getIntervall() {
        return intervall;
    }
}
