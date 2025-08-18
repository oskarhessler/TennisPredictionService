import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerHistory {
    private final List<MatchResult> matchHistory = new ArrayList<>();
    private final Map<String, SurfaceRecord> surfaceRecords = new HashMap<>();
    private final Map<String, Double> surfaceElo = new HashMap<>();

    public PlayerHistory() {
        // Initialize surface ELO ratings
        surfaceElo.put("Hard", 1500.0);
        surfaceElo.put("Clay", 1500.0);
        surfaceElo.put("Grass", 1500.0);
        surfaceElo.put("Carpet", 1500.0);
    }

    public void addMatch(boolean won, String surface, Integer date) {
        MatchResult result = new MatchResult(won, surface, date);
        matchHistory.add(result);

        // Update surface-specific record
        surfaceRecords.computeIfAbsent(surface, k -> new SurfaceRecord()).addMatch(won);

        // Update ELO (simple implementation)
        double currentElo = surfaceElo.getOrDefault(surface, 1500.0);
        double eloChange = won ? 15.0 : -15.0;
        surfaceElo.put(surface, Math.max(800.0, Math.min(2800.0, currentElo + eloChange)));
    }

    public double getRecentForm(int lastNMatches) {
        if (matchHistory.isEmpty()) return 0.5;

        int wins = 0;
        int matches = Math.min(lastNMatches, matchHistory.size());

        for (int i = matchHistory.size() - matches; i < matchHistory.size(); i++) {
            if (matchHistory.get(i).won) wins++;
        }

        return (double) wins / matches;
    }

    public double getSurfaceForm(String surface, int lastNMatches) {
        if (surface == null || surface.isEmpty()) {
            surface = "Hard"; // fallback om surface saknas
        }
        final String finalSurface = surface;
        List<MatchResult> surfaceMatches = new ArrayList<>();
        for (MatchResult match : matchHistory) {
            if (finalSurface.equals(match.surface)) {
                surfaceMatches.add(match);
            }
        }

        if (surfaceMatches.isEmpty()) return 0.5;

        int wins = 0;
        int matches = Math.min(lastNMatches, surfaceMatches.size());

        for (int i = Math.max(0, surfaceMatches.size() - matches); i < surfaceMatches.size(); i++) {
            if (surfaceMatches.get(i).won) wins++;
        }

        return matches > 0 ? (double) wins / matches : 0.5;
    }

    public double getSurfaceElo(String surface) {
        return surfaceElo.getOrDefault(surface, 1500.0);
    }

    public int getTotalMatches() {
        return matchHistory.size();
    }

    public int getTotalWins() {
        return (int) matchHistory.stream().mapToInt(m -> m.won ? 1 : 0).sum();
    }

    public SurfaceRecord getSurfaceRecord(String surface) {
        return surfaceRecords.getOrDefault(surface, new SurfaceRecord());
    }

    private static class MatchResult {
        final boolean won;
        final String surface;
        final Integer date;

        MatchResult(boolean won, String surface, Integer date) {
            this.won = won;
            this.surface = surface;
            this.date = date;
        }
    }
}