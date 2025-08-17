public class Player {
    private final String playerId;
    private final String name;
    private final String hand;
    private final String nationality;
    private final Integer seed;
    private final String entry;
    private final Integer height;
    private final Double age;
    private final Integer rank;
    private final Integer rankPoints;

    public Player(String playerId, String name, String hand, String nationality,
                  Integer seed, String entry, Integer height, Double age,
                  Integer rank, Integer rankPoints) {
        this.playerId = playerId;
        this.name = name;
        this.hand = hand;
        this.nationality = nationality;
        this.seed = seed;
        this.entry = entry;
        this.height = height;
        this.age = age;
        this.rank = rank;
        this.rankPoints = rankPoints;
    }

    // Getters
    public String getPlayerId() { return playerId; }
    public String getName() { return name; }
    public String getHand() { return hand; }
    public String getNationality() { return nationality; }
    public Integer getSeed() { return seed; }
    public String getEntry() { return entry; }
    public Integer getHeight() { return height; }
    public Double getAge() { return age; }
    public Integer getRank() { return rank; }
    public Integer getRankPoints() { return rankPoints; }
}
