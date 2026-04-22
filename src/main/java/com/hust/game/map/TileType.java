package com.hust.game.map;

public enum TileType {
    GROUND_1(0, "ground_1.png", false),
    POND(1, "pond.png", true),
    GROUND_2(2, "ground_2.png", false),
    WALL(3, "wall.png", true);

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