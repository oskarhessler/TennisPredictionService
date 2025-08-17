public class SurfaceRecord {
    private int matches = 0;
    private int wins = 0;

    public void addMatch(boolean won) {
        matches++;
        if (won) wins++;
    }

    public double getWinRate() {
        return matches > 0 ? (double) wins / matches : 0.5;
    }

    public int getMatches() { return matches; }
    public int getWins() { return wins; }
    public int getLosses() { return matches - wins; }
}