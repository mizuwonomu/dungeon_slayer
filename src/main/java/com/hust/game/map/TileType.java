package com.hust.game.map;

public enum TileType {
    GROUND_1(-1, "ground_1.png", false),
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
    WHEAT_HOUSE(17, "wheat_house.png", true),
    BRICK_1(18, "brick_1.png", false),
    BRICK_2(19, "brick_2.png", false),
    BRICK_3(20, "brick_3.png", false),
    BRICKWALL_1(21, "brickwall_1.png", true),
    BRICKWALL_2(22, "brickwall_2.png", true),
    BRICKWALL_3(23, "brickwall_3.png", true),
    BRICKWALL_LEFT(24, "brickwall_left.png", true),
    BRICKWALL_RIGHT(25, "brickwall_right.png", true),
    C_INTERSEC(26, "crater_intersec.png", true),
    C_CONNECT_LR(27, "crater_connect_lr.png", true),
    C_CONNECT_UD(28, "crater_connect_ud.png", true),
    C_L(29, "crater_l.png", true),
    C_R(30, "crater_r.png", true),
    C_U(31, "crater_u.png", true),
    C_D(32, "crater_d.png", true),
    C_CONNER_UL(33, "crater_ul.png", true),
    C_CONNER_DL(34, "crater_dl.png", true),
    C_CONNER_UR(35, "crater_ur.png", true),
    C_CONNER_DR(36, "crater_dr.png", true),
    C_THREE_ULR(37, "crater_ulr.png", true),
    C_THREE_DLR(38, "crater_dlr.png", true),
    C_THREE_LUD(39, "crater_lud.png", true),
    C_THREE_RUD(40, "crater_rud.png", true),
    SKULL(41, "skull.png", true),
    BRICKWALL_4(42, "brickwall_4.png", true),
    BRICK_BLOOD(43, "blood.png", false),
    BRICK_BLOOD_2(44, "blood_2.png", false),
    ROCK(45, "rock.png", false);

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