public class Builder {
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