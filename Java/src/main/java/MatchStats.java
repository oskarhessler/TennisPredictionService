public class MatchStats {
    private final Integer aces;
    private final Integer doubleFaults;
    private final Integer servePoints;
    private final Integer firstServeIn;
    private final Integer firstServeWon;
    private final Integer secondServeWon;
    private final Integer serviceGames;
    private final Integer breakPointsSaved;
    private final Integer breakPointsFaced;

    public MatchStats(Integer aces, Integer doubleFaults, Integer servePoints,
                      Integer firstServeIn, Integer firstServeWon, Integer secondServeWon,
                      Integer serviceGames, Integer breakPointsSaved, Integer breakPointsFaced) {
        this.aces = aces;
        this.doubleFaults = doubleFaults;
        this.servePoints = servePoints;
        this.firstServeIn = firstServeIn;
        this.firstServeWon = firstServeWon;
        this.secondServeWon = secondServeWon;
        this.serviceGames = serviceGames;
        this.breakPointsSaved = breakPointsSaved;
        this.breakPointsFaced = breakPointsFaced;
    }

    // Getters
    public Integer getAces() { return aces; }
    public Integer getDoubleFaults() { return doubleFaults; }
    public Integer getServePoints() { return servePoints; }
    public Integer getFirstServeIn() { return firstServeIn; }
    public Integer getFirstServeWon() { return firstServeWon; }
    public Integer getSecondServeWon() { return secondServeWon; }
    public Integer getServiceGames() { return serviceGames; }
    public Integer getBreakPointsSaved() { return breakPointsSaved; }
    public Integer getBreakPointsFaced() { return breakPointsFaced; }

    // Derived statistics
    public Double getFirstServePercentage() {
        return (servePoints != null && servePoints > 0 && firstServeIn != null)
                ? (double) firstServeIn / servePoints : null;
    }

    public Double getFirstServeWinPercentage() {
        return (firstServeIn != null && firstServeIn > 0 && firstServeWon != null)
                ? (double) firstServeWon / firstServeIn : null;
    }

    public Double getSecondServeWinPercentage() {
        if (servePoints == null || firstServeIn == null || secondServeWon == null) return null;
        int secondServePoints = servePoints - firstServeIn;
        return secondServePoints > 0 ? (double) secondServeWon / secondServePoints : null;
    }

    public Double getAceRate() {
        return (servePoints != null && servePoints > 0 && aces != null)
                ? (double) aces / servePoints : null;
    }

    public Double getDoubleFaultRate() {
        return (servePoints != null && servePoints > 0 && doubleFaults != null)
                ? (double) doubleFaults / servePoints : null;
    }

    public Double getBreakPointSavePercentage() {
        return (breakPointsFaced != null && breakPointsFaced > 0 && breakPointsSaved != null)
                ? (double) breakPointsSaved / breakPointsFaced : null;
    }
}