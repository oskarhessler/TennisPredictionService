import java.util.*;

/**
 * Enhanced PlayerHistory class with better ELO management and form tracking
 */
public class PlayerHistory {
    private final List<MatchResult> matchHistory = new ArrayList<>();
    private final Map<String, SurfaceRecord> surfaceRecords = new HashMap<>();
    private final Map<String, Double> surfaceElo = new HashMap<>();
    private final Map<String, List<MatchResult>> surfaceSpecificHistory = new HashMap<>();

    // Time decay parameters for form calculation
    private static final double TIME_DECAY_FACTOR = 0.95;
    private static final int MAX_HISTORY_SIZE = 500;

    public PlayerHistory() {
        // Initialize surface ELO ratings to baseline
        surfaceElo.put("Hard", 1500.0);
        surfaceElo.put("Clay", 1500.0);
        surfaceElo.put("Grass", 1500.0);
        surfaceElo.put("Carpet", 1500.0);

        // Initialize surface-specific history
        surfaceSpecificHistory.put("Hard", new ArrayList<>());
        surfaceSpecificHistory.put("Clay", new ArrayList<>());
        surfaceSpecificHistory.put("Grass", new ArrayList<>());
        surfaceSpecificHistory.put("Carpet", new ArrayList<>());
    }

    public void addMatch(boolean won, String surface, Integer date) {
        if (surface == null || surface.isEmpty()) {
            surface = "Hard";
        }

        MatchResult result = new MatchResult(won, surface, date);
        matchHistory.add(result);

        // Add to surface-specific history
        surfaceSpecificHistory.computeIfAbsent(surface, k -> new ArrayList<>()).add(result);

        // Update surface-specific record
        surfaceRecords.computeIfAbsent(surface, k -> new SurfaceRecord()).addMatch(won);

        // Trim history if it gets too large
        if (matchHistory.size() > MAX_HISTORY_SIZE) {
            matchHistory.remove(0);
        }

        List<MatchResult> surfaceHistory = surfaceSpecificHistory.get(surface);
        if (surfaceHistory != null && surfaceHistory.size() > MAX_HISTORY_SIZE / 2) {
            surfaceHistory.remove(0);
        }
    }

    /**
     * Calculate recent form with time decay
     */
    public double getRecentForm(int lastNMatches) {
        if (matchHistory.isEmpty()) return 0.5;

        double weightedWins = 0.0;
        double totalWeight = 0.0;
        int matches = Math.min(lastNMatches, matchHistory.size());

        for (int i = 0; i < matches; i++) {
            int index = matchHistory.size() - 1 - i;
            MatchResult match = matchHistory.get(index);

            // Apply time decay (more recent matches have higher weight)
            double weight = Math.pow(TIME_DECAY_FACTOR, i);
            totalWeight += weight;

            if (match.won) {
                weightedWins += weight;
            }
        }

        return totalWeight > 0 ? weightedWins / totalWeight : 0.5;
    }

    /**
     * Calculate surface-specific form with improved time weighting
     */
    public double getSurfaceForm(String surface, int lastNMatches) {
        if (surface == null || surface.isEmpty()) {
            surface = "Hard";
        }

        List<MatchResult> surfaceMatches = surfaceSpecificHistory.get(surface);
        if (surfaceMatches == null || surfaceMatches.isEmpty()) {
            return 0.5;
        }

        double weightedWins = 0.0;
        double totalWeight = 0.0;
        int matches = Math.min(lastNMatches, surfaceMatches.size());

        for (int i = 0; i < matches; i++) {
            int index = surfaceMatches.size() - 1 - i;
            MatchResult match = surfaceMatches.get(index);

            // Apply time decay
            double weight = Math.pow(TIME_DECAY_FACTOR, i);
            totalWeight += weight;

            if (match.won) {
                weightedWins += weight;
            }
        }

        return totalWeight > 0 ? weightedWins / totalWeight : 0.5;
    }

    /**
     * Get surface ELO rating
     */
    public double getSurfaceElo(String surface) {
        if (surface == null || surface.isEmpty()) {
            surface = "Hard";
        }
        return surfaceElo.getOrDefault(surface, 1500.0);
    }

    /**
     * Update surface ELO rating (used by calibrator)
     */
    public void updateSurfaceElo(String surface, double newElo) {
        if (surface == null || surface.isEmpty()) {
            surface = "Hard";
        }
        surfaceElo.put(surface, newElo);
    }

    /**
     * Get momentum factor (winning/losing streak impact)
     */
    public double getMomentum(int lookbackMatches) {
        if (matchHistory.isEmpty()) return 0.0;

        int matches = Math.min(lookbackMatches, matchHistory.size());
        double momentum = 0.0;
        double streakWeight = 1.0;

        for (int i = 0; i < matches; i++) {
            int index = matchHistory.size() - 1 - i;
            MatchResult match = matchHistory.get(index);

            double result = match.won ? 1.0 : -1.0;
            momentum += result * streakWeight;
            streakWeight *= 0.8; // Decay weight for older matches
        }

        return Math.tanh(momentum / matches); // Normalize to [-1, 1]
    }

    /**
     * Get surface-specific ELO reliability (based on number of matches played)
     */
    public double getSurfaceEloReliability(String surface) {
        List<MatchResult> surfaceMatches = surfaceSpecificHistory.get(surface);
        if (surfaceMatches == null || surfaceMatches.isEmpty()) {
            return 0.0;
        }

        // Reliability increases with matches played, plateaus at 50 matches
        int matchesPlayed = surfaceMatches.size();
        return Math.min(1.0, matchesPlayed / 50.0);
    }

    /**
     * Calculate average opponent strength (for ELO adjustments)
     */
    public double getAverageOpponentStrength(String surface, int lastNMatches) {
        // This would need opponent ranking data stored in MatchResult
        // For now, return neutral value
        return 1500.0;
    }

    public int getTotalMatches() {
        return matchHistory.size();
    }

    public int getTotalWins() {
        return (int) matchHistory.stream().mapToInt(m -> m.won ? 1 : 0).sum();
    }

    public int getSurfaceMatches(String surface) {
        List<MatchResult> matches = surfaceSpecificHistory.get(surface);
        return matches != null ? matches.size() : 0;
    }

    public int getSurfaceWins(String surface) {
        List<MatchResult> matches = surfaceSpecificHistory.get(surface);
        if (matches == null) return 0;

        return (int) matches.stream().mapToInt(m -> m.won ? 1 : 0).sum();
    }

    public SurfaceRecord getSurfaceRecord(String surface) {
        return surfaceRecords.getOrDefault(surface, new SurfaceRecord());
    }

    /**
     * Get all surface ELO ratings
     */
    public Map<String, Double> getAllSurfaceElos() {
        return new HashMap<>(surfaceElo);
    }

    /**
     * Reset ELO ratings to baseline (useful for new seasons)
     */
    public void resetEloRatings() {
        surfaceElo.replaceAll((k, v) -> 1500.0);
    }

    /**
     * Get recent results for detailed analysis
     */
    public List<MatchResult> getRecentMatches(int count) {
        if (matchHistory.isEmpty()) return new ArrayList<>();

        int fromIndex = Math.max(0, matchHistory.size() - count);
        return new ArrayList<>(matchHistory.subList(fromIndex, matchHistory.size()));
    }

    /**
     * Enhanced MatchResult class with additional metadata
     */
    public static class MatchResult {
        final boolean won;
        final String surface;
        final Integer date;
        final Double opponentRank;
        final String tournamentLevel;

        public MatchResult(boolean won, String surface, Integer date) {
            this(won, surface, date, null, null);
        }

        public MatchResult(boolean won, String surface, Integer date, Double opponentRank, String tournamentLevel) {
            this.won = won;
            this.surface = surface != null ? surface : "Hard";
            this.date = date;
            this.opponentRank = opponentRank;
            this.tournamentLevel = tournamentLevel;
        }

        public boolean isWon() { return won; }
        public String getSurface() { return surface; }
        public Integer getDate() { return date; }
        public Double getOpponentRank() { return opponentRank; }
        public String getTournamentLevel() { return tournamentLevel; }

        /**
         * Calculate days since this match
         */
        public int getDaysSince(Integer currentDate) {
            if (date == null || currentDate == null) return 0;

            // Simple approximation - in reality you'd want proper date parsing
            int yearDiff = (currentDate / 10000) - (date / 10000);
            int monthDiff = ((currentDate / 100) % 100) - ((date / 100) % 100);
            int dayDiff = (currentDate % 100) - (date % 100);

            return yearDiff * 365 + monthDiff * 30 + dayDiff;
        }
    }
}