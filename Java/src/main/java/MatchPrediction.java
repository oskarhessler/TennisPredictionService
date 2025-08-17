public class MatchPrediction {
    private final Player player1;
    private final Player player2;
    private final MatchContext context;
    private final double player1WinProbability;

    public MatchPrediction(Player player1, Player player2, MatchContext context, double player1WinProbability) {
        this.player1 = player1;
        this.player2 = player2;
        this.context = context;
        this.player1WinProbability = player1WinProbability;
    }

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public MatchContext getContext() { return context; }
    public double getPlayer1WinProbability() { return player1WinProbability; }
    public double getPlayer2WinProbability() { return 1.0 - player1WinProbability; }

    public Player getFavorite() {
        return player1WinProbability > 0.5 ? player1 : player2;
    }

    public double getFavoriteProbability() {
        return Math.max(player1WinProbability, 1.0 - player1WinProbability);
    }

    @Override
    public String toString() {
        Player favorite = getFavorite();
        double favProb = getFavoriteProbability();
        return String.format("%s vs %s - %s %.1f%% favorite (Surface: %s)",
                player1.getName(), player2.getName(),
                favorite.getName(), favProb * 100, context.getSurface());
    }
}
