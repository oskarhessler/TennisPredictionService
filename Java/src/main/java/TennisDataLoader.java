import java.io.*;
import java.util.*;

/**
 * Loads and parses CSV tennis data into Match objects
 */
public class TennisDataLoader {

    public List<Match> loadMatches(String csvFilePath) throws IOException {
        List<Match> matches = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return matches;

            String[] headers = headerLine.split(",", -1);
            Map<String, Integer> columnIndex = createColumnIndex(headers);

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",", -1);
                Match match = parseMatch(values, columnIndex);
                if (match != null) {
                    matches.add(match);
                }
            }
        }

        return matches;
    }

    private Map<String, Integer> createColumnIndex(String[] headers) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            index.put(headers[i].trim(), i);
        }
        return index;
    }

    private Match parseMatch(String[] values, Map<String, Integer> columnIndex) {
        try {
            // Parse tournament info
            String tourneyId = getValue(values, columnIndex, "tourney_id");
            String tourneyName = getValue(values, columnIndex, "tourney_name");
            String surface = getValue(values, columnIndex, "surface");
            Integer drawSize = getIntegerValue(values, columnIndex, "draw_size");
            String tourneyLevel = getValue(values, columnIndex, "tourney_level");
            Integer tourneyDate = getIntegerValue(values, columnIndex, "tourney_date");
            Integer matchNum = getIntegerValue(values, columnIndex, "match_num");
            String round = getValue(values, columnIndex, "round");
            Integer bestOf = getIntegerValue(values, columnIndex, "best_of");
            Integer minutes = getIntegerValue(values, columnIndex, "minutes");
            String score = getValue(values, columnIndex, "score");

            // Parse winner
            Player winner = new Player(
                    getValue(values, columnIndex, "winner_id"),
                    getValue(values, columnIndex, "winner_name"),
                    getValue(values, columnIndex, "winner_hand"),
                    getValue(values, columnIndex, "winner_ioc"),
                    getIntegerValue(values, columnIndex, "winner_seed"),
                    getValue(values, columnIndex, "winner_entry"),
                    getIntegerValue(values, columnIndex, "winner_ht"),
                    getDoubleValue(values, columnIndex, "winner_age"),
                    getIntegerValue(values, columnIndex, "winner_rank"),
                    getIntegerValue(values, columnIndex, "winner_rank_points")
            );

            // Parse loser
            Player loser = new Player(
                    getValue(values, columnIndex, "loser_id"),
                    getValue(values, columnIndex, "loser_name"),
                    getValue(values, columnIndex, "loser_hand"),
                    getValue(values, columnIndex, "loser_ioc"),
                    getIntegerValue(values, columnIndex, "loser_seed"),
                    getValue(values, columnIndex, "loser_entry"),
                    getIntegerValue(values, columnIndex, "loser_ht"),
                    getDoubleValue(values, columnIndex, "loser_age"),
                    getIntegerValue(values, columnIndex, "loser_rank"),
                    getIntegerValue(values, columnIndex, "loser_rank_points")
            );

            // Parse winner stats
            MatchStats winnerStats = new MatchStats(
                    getIntegerValue(values, columnIndex, "w_ace"),
                    getIntegerValue(values, columnIndex, "w_df"),
                    getIntegerValue(values, columnIndex, "w_svpt"),
                    getIntegerValue(values, columnIndex, "w_1stIn"),
                    getIntegerValue(values, columnIndex, "w_1stWon"),
                    getIntegerValue(values, columnIndex, "w_2ndWon"),
                    getIntegerValue(values, columnIndex, "w_SvGms"),
                    getIntegerValue(values, columnIndex, "w_bpSaved"),
                    getIntegerValue(values, columnIndex, "w_bpFaced")
            );

            // Parse loser stats
            MatchStats loserStats = new MatchStats(
                    getIntegerValue(values, columnIndex, "l_ace"),
                    getIntegerValue(values, columnIndex, "l_df"),
                    getIntegerValue(values, columnIndex, "l_svpt"),
                    getIntegerValue(values, columnIndex, "l_1stIn"),
                    getIntegerValue(values, columnIndex, "l_1stWon"),
                    getIntegerValue(values, columnIndex, "l_2ndWon"),
                    getIntegerValue(values, columnIndex, "l_SvGms"),
                    getIntegerValue(values, columnIndex, "l_bpSaved"),
                    getIntegerValue(values, columnIndex, "l_bpFaced")
            );

            return new Match.Builder()
                    .tourneyId(tourneyId)
                    .tourneyName(tourneyName)
                    .surface(surface)
                    .drawSize(drawSize)
                    .tourneyLevel(tourneyLevel)
                    .tourneyDate(tourneyDate)
                    .matchNum(matchNum)
                    .round(round)
                    .bestOf(bestOf)
                    .minutes(minutes)
                    .score(score)
                    .winner(winner)
                    .loser(loser)
                    .winnerStats(winnerStats)
                    .loserStats(loserStats)
                    .build();

        } catch (Exception e) {
            System.err.println("Error parsing match: " + e.getMessage());
            return null;
        }
    }

    private String getValue(String[] values, Map<String, Integer> index, String column) {
        Integer pos = index.get(column);
        if (pos == null || pos >= values.length) return null;
        String value = values[pos].trim();
        return value.isEmpty() ? null : value;
    }

    private Integer getIntegerValue(String[] values, Map<String, Integer> index, String column) {
        String value = getValue(values, index, column);
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double getDoubleValue(String[] values, Map<String, Integer> index, String column) {
        String value = getValue(values, index, column);
        if (value == null) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
