import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks historical performance data for players
 */
public class PlayerHistoryManager {
    private final Map<String, PlayerHistory> playerHistories = new ConcurrentHashMap<>();
    private final Map<String, Map<String, HeadToHeadRecord>> headToHeadRecords = new ConcurrentHashMap<>();

    public void updateWithMatch(Match match) {
        String winnerId = match.getWinner().getPlayerId();
        String loserId = match.getLoser().getPlayerId();
        String surface = match.getSurface();

        // Update individual player histories
        getOrCreateHistory(winnerId).addMatch(true, surface, match.getTourneyDate());
        getOrCreateHistory(loserId).addMatch(false, surface, match.getTourneyDate());

        // Update head-to-head records
        updateHeadToHead(winnerId, loserId, true);
        updateHeadToHead(loserId, winnerId, false);
    }

    public PlayerHistory getPlayerHistory(String playerId) {
        return playerHistories.get(playerId);
    }

    public HeadToHeadRecord getHeadToHeadRecord(String player1Id, String player2Id) {
        return headToHeadRecords.getOrDefault(player1Id, Collections.emptyMap()).get(player2Id);
    }

    private PlayerHistory getOrCreateHistory(String playerId) {
        return playerHistories.computeIfAbsent(playerId, k -> new PlayerHistory());
    }

    private void updateHeadToHead(String player1Id, String player2Id, boolean player1Won) {
        headToHeadRecords.computeIfAbsent(player1Id, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(player2Id, k -> new HeadToHeadRecord())
                .addMatch(player1Won);
    }

    public void reset() {
        playerHistories.clear();
        headToHeadRecords.clear();
    }
}