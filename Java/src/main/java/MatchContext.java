public class MatchContext {
    private final String surface;
    private final String tourneyLevel;
    private final Integer tourneyDate;
    private final String round;
    private final Integer bestOf;

    public MatchContext(String surface, String tourneyLevel, Integer tourneyDate, String round, Integer bestOf) {
        this.surface = surface;
        this.tourneyLevel = tourneyLevel;
        this.tourneyDate = tourneyDate;
        this.round = round;
        this.bestOf = bestOf;
    }

    // Getters
    public String getSurface() { return surface; }
    public String getTourneyLevel() { return tourneyLevel; }
    public Integer getTourneyDate() { return tourneyDate; }
    public String getRound() { return round; }
    public Integer getBestOf() { return bestOf; }
}
