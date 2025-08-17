public class UpcomingMatch {
    private final Player player1;
    private final Player player2;
    private final MatchContext context;

    public UpcomingMatch(Player player1, Player player2, MatchContext context) {
        this.player1 = player1;
        this.player2 = player2;
        this.context = context;
    }

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public MatchContext getContext() { return context; }
}