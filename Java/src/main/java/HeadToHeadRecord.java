public class HeadToHeadRecord {
    private int wins = 0;
    private int losses = 0;

    public void addMatch(boolean won) {
        if (won) wins++;
        else losses++;
    }

    public double getWinRate() {
        int total = wins + losses;
        return total > 0 ? (double) wins / total : 0.5;
    }

    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getTotalMatches() { return wins + losses; }
}