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

    public Match(Builder builder) {
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

    // Getters
    public String getTourneyId() {
        return tourneyId;
    }

    public String getTourneyName() {
        return tourneyName;
    }

    public String getSurface() {
        return surface;
    }

    public Integer getDrawSize() {
        return drawSize;
    }

    public String getTourneyLevel() {
        return tourneyLevel;
    }

    public Integer getTourneyDate() {
        return tourneyDate;
    }

    public Integer getMatchNum() {
        return matchNum;
    }

    public String getRound() {
        return round;
    }

    public Integer getBestOf() {
        return bestOf;
    }

    public Integer getMinutes() {
        return minutes;
    }

    public String getScore() {
        return score;
    }

    public Player getWinner() {
        return winner;
    }

    public Player getLoser() {
        return loser;
    }

    public MatchStats getWinnerStats() {
        return winnerStats;
    }

    public MatchStats getLoserStats() {
        return loserStats;
    }
}