public class Match {
    private final String tourneyId;
    private final String tourneyName;
    private final String surface;
    private final Integer drawSize;
    private final String tourneyLevel;
    private final Integer tourneyDate;
    private final Integer matchNum;
    private final String round;
    private final Integer bestOf;
    private final Integer minutes;
    private final String score;

    private final Player winner;
    private final Player loser;
    private final MatchStats winnerStats;
    private final MatchStats loserStats;

    private Match(Builder builder) {
        this.tourneyId = builder.tourneyId;
        this.tourneyName = builder.tourneyName;
        this.surface = builder.surface;
        this.drawSize = builder.drawSize;
        this.tourneyLevel = builder.tourneyLevel;
        this.tourneyDate = builder.tourneyDate;
        this.matchNum = builder.matchNum;
        this.round = builder.round;
        this.bestOf = builder.bestOf;
        this.minutes = builder.minutes;
        this.score = builder.score;
        this.winner = builder.winner;
        this.loser = builder.loser;
        this.winnerStats = builder.winnerStats;
        this.loserStats = builder.loserStats;
    }

    // All your existing getters remain the same...
    public String getTourneyId() { return tourneyId; }
    public String getTourneyName() { return tourneyName; }
    public String getSurface() { return surface; }
    public Integer getDrawSize() { return drawSize; }
    public String getTourneyLevel() { return tourneyLevel; }
    public Integer getTourneyDate() { return tourneyDate; }
    public Integer getMatchNum() { return matchNum; }
    public String getRound() { return round; }
    public Integer getBestOf() { return bestOf; }
    public Integer getMinutes() { return minutes; }
    public String getScore() { return score; }
    public Player getWinner() { return winner; }
    public Player getLoser() { return loser; }
    public MatchStats getWinnerStats() { return winnerStats; }
    public MatchStats getLoserStats() { return loserStats; }

    // NESTED BUILDER CLASS - Move content from Builder.java here
    public static class Builder {
        String tourneyId;
        String tourneyName;
        String surface;
        String tourneyLevel;
        String round;
        String score;
        Integer drawSize;
        Integer tourneyDate;
        Integer matchNum;
        Integer bestOf;
        Integer minutes;
        Player winner;
        Player loser;
        MatchStats winnerStats;
        MatchStats loserStats;

        public Builder tourneyId(String val) { tourneyId = val; return this; }
        public Builder tourneyName(String val) { tourneyName = val; return this; }
        public Builder surface(String val) { surface = val; return this; }
        public Builder drawSize(Integer val) { drawSize = val; return this; }
        public Builder tourneyLevel(String val) { tourneyLevel = val; return this; }
        public Builder tourneyDate(Integer val) { tourneyDate = val; return this; }
        public Builder matchNum(Integer val) { matchNum = val; return this; }
        public Builder round(String val) { round = val; return this; }
        public Builder bestOf(Integer val) { bestOf = val; return this; }
        public Builder minutes(Integer val) { minutes = val; return this; }
        public Builder score(String val) { score = val; return this; }
        public Builder winner(Player val) { winner = val; return this; }
        public Builder loser(Player val) { loser = val; return this; }
        public Builder winnerStats(MatchStats val) { winnerStats = val; return this; }
        public Builder loserStats(MatchStats val) { loserStats = val; return this; }

        public Match build() { return new Match(this); }
    }
}