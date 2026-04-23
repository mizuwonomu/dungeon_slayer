package com.hust.game.map;

public enum TileType {
    GROUND_1(0, "ground_1.png", false),
    GROUND_2(1, "ground_2.png", false),
    ROAD_1(2, "road_1.png", false),
    ROAD_2(3, "road_2.png", false),
    LEFT_UP_ROAD(4, "left_up_road.png", false),
    LEFT_DOWN_ROAD(5, "left_down_road.png", false),
    RIGHT_UP_ROAD(6, "right_up_road.png", false),
    RIGHT_DOWN_ROAD(7, "right_down_road.png", false),
    LEFT_ROAD(8, "left_road.png", false),
    RIGHT_ROAD(9, "right_road.png", false),
    UP_ROAD(10, "up_road.png", false),
    DOWN_ROAD(11, "down_road.png", false),
    NO_WHEAT(12, "no_wheat.png", false),
    GREEN_WHEAT(13, "green_wheat.png", false),
    GOLD_WHEAT(14, "gold_wheat.png", false),
    WALL(15, "wall.png", true),
    POND(16, "pond.png", true),
    WHEAT_HOUSE(17, "wheat_house.png", true);

    private final int id;
    private final String imageName;
    private final boolean isSolid;

    TileType(int id, String imageName, boolean isSolid) {
        this.id = id;
        this.imageName = imageName;
        this.isSolid = isSolid;
    }

    public int getId() { return id; }
    public String getImageName() { return imageName; }
    public boolean isSolid() { return isSolid; }

    public static TileType fromId(int id) {
        for (TileType type : values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        // Trả về null hoặc throw exception nếu ID không hợp lệ.
        // Trong trường hợp này, trả về null để tránh crash game nếu map file có lỗi.
        return null;
    }
}