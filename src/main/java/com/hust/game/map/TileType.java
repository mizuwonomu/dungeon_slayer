package com.hust.game.map;

public enum TileType {
    EMPTY(0, null, true),

    T0_WALL(-1, "wall.png", true),
    T1_GROUND_1(1, "ground_1.png", false),
    T3_GROUND_2(2, "ground_2.png", false),
    T3_NO_WHEAT(3, "no_wheat.png", false),
    T4_WHEAT(4, "wheat.png", false),
    T5_ROAD_UP_1(5, "road_up_1.png", false),
    T6_ROAD_NGANG_1(6, "road_ngang_1.png", false),
    T7_ROAD_UP_2(7, "road_up_2.png", false),
    T8_ROAD_NGANG_2(8, "road_ngang_2.png", false),
    T9_ROAD_LEFT_UP(9, "road_left_up.png", false),
    T10_ROAD_LEFT_DOWN(10, "road_left_down.png", false),
    T11_ROAD_RIGHT_UP(11, "road_right_up.png", false),
    T12_ROAD_RIGHT_DOWN(12, "road_right_down.png", false),
    T13_POND(13, "pond.png", true),
    T26_DIRT_RU(26, "dirt_ru.png", false),
    T27_DIRT_LU(27, "dirt_lu.png", false),
    T28_DIRT_D(28, "dirt_d.png", false),
    T29_DIRT(29, "dirt.png", false),
    T30_DIRT_U(30, "dirt_u.png", false),
    T31_DIRT_R(31, "dirt_r.png", false),
    T32_DIRT_L(32, "dirt_l.png", false),
    T33_DIRT_LD(33, "dirt_ld.png", false),
    T34_DIRT_RD(34, "dirt_rd.png", false),

    BRICK_1(-18, "brick_1.png", false),
    BRICK_2(-19, "brick_2.png", false),
    BRICK_3(-20, "brick_3.png", false),
    BRICKWALL_1(-21, "brickwall_1.png", true),
    BRICKWALL_2(-22, "brickwall_2.png", true),
    BRICKWALL_3(-23, "brickwall_3.png", true),
    BRICKWALL_LEFT(-24, "brickwall_left.png", true),
    BRICKWALL_RIGHT(-25, "brickwall_right.png", true),
    C_INTERSEC(-26, "crater_intersec.png", true),
    C_CONNECT_LR(-27, "crater_connect_lr.png", true),
    C_CONNECT_UD(-28, "crater_connect_ud.png", true),
    C_L(-29, "crater_l.png", true),
    C_R(-30, "crater_r.png", true),
    C_U(-31, "crater_u.png", true),
    C_D(-32, "crater_d.png", true),
    C_CONNER_UL(-33, "crater_ul.png", true),
    C_CONNER_DL(-34, "crater_dl.png", true),
    C_CONNER_UR(-35, "crater_ur.png", true),
    C_CONNER_DR(-36, "crater_dr.png", true),
    C_THREE_ULR(-37, "crater_ulr.png", true),
    C_THREE_DLR(-38, "crater_dlr.png", true),
    C_THREE_LUD(-39, "crater_lud.png", true),
    C_THREE_RUD(-40, "crater_rud.png", true),
    SKULL(-41, "skull.png", true),
    BRICKWALL_4(-42, "brickwall_4.png", true),
    BRICK_BLOOD(-43, "blood.png", false),
    BRICK_BLOOD_2(-44, "blood_2.png", false),
    ROCK(-45, "rock.png", false),
    BRICKWALL_CORNER_RIGHT(-46, "brickwall_conner_right.png", true),
    BRICKWALL_CONNER_LEFT(-47, "brickwall_conner_left.png", true);

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