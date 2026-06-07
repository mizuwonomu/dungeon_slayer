# Dungeon Slayer

## Mục Lục

- [Giới Thiệu](#giới-thiệu)
- [Chủ Đề Game](#chủ-đề-game)
- [Tính Năng Chính](#tính-năng-chính)
- [Công Nghệ Sử Dụng](#công-nghệ-sử-dụng)
- [Cách Cài Đặt Và Chạy Project](#cách-cài-đặt-và-chạy-project)
- [Cách Chơi](#cách-chơi)
- [Cấu Trúc Package](#cấu-trúc-package)
- [Nền Tảng OOP Trong Project](#nền-tảng-oop-trong-project)
- [Tài Nguyên Game](#tài-nguyên-game)
- [Ghi Chú Phát Triển](#ghi-chú-phát-triển)

## Giới Thiệu

**Dungeon Slayer** là game hành động nhập vai 2D được xây dựng bằng Java và JavaFX. Người chơi điều khiển nhân vật chính đi qua nhiều màn chơi, chiến đấu với quái vật, thu thập vật phẩm, mở rương phần thưởng và tiến tới màn boss cuối.

Project được phát triển theo hướng áp dụng các nguyên lý lập trình hướng đối tượng, tách riêng các nhóm chức năng như nhân vật, quái vật, bản đồ, chiến đấu, giao diện, âm thanh và tiến trình màn chơi.

## Chủ Đề Game

Game lấy bối cảnh phiêu lưu trong dungeon / khu vực nguy hiểm. Người chơi nhập vai một hồn ma có khả năng dùng kiếm, kỹ năng cường hóa và các vật phẩm hỗ trợ để vượt qua nhiều loại kẻ địch. Đi qua quá trình tìm lại bản thể thật sự của mình.

Mục tiêu chính:

- Hoàn thành tutorial để học cách điều khiển.
- Vượt qua level 1 và level 2 bằng cách đánh bại quái vật.
- Thu thập coin, bình máu, bình mana và phần thưởng từ rương.
- Đối đầu boss ở level cuối.

## Tính Năng Chính

- Di chuyển nhân vật theo 4 hướng.
- Tấn công thường bằng kiếm.
- Hệ thống combo và phản hồi khi đánh trúng.
- Kỹ năng cường hóa sử dụng mana.
- Bình máu và bình mana.
- Rương phần thưởng mở bằng đòn đánh.
- Nhiều loại enemy: Slime, Knight, Tree, Witch và Final Boss.
- Minimap hỗ trợ định vị.
- HUD hiển thị máu, mana, coin, potion và cooldown kỹ năng.
- Màn hình Menu, Pause, Settings, Level Clear, Tutorial Complete và Game Finish.
- Cài đặt âm lượng, hiệu ứng âm thanh và chế độ màn hình.

## Công Nghệ Sử Dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 17 |
| Giao diện / game loop | JavaFX 17 |
| Quản lý project | Maven |
| Âm thanh | JavaFX Media / AudioClip |
| Test | JUnit 4 |
| Hỗ trợ code | Lombok |
| Tài nguyên | Sprite sheet, tile map text file, font, audio |

## Cách Cài Đặt Và Chạy Project

### Yêu Cầu

- Java JDK 17 hoặc cao hơn.
- Maven.
- Hệ điều hành có thể chạy JavaFX.

Kiểm tra phiên bản:

```bash
java -version
mvn -version
```

### Chạy Game

Từ thư mục gốc của project:

```bash
mvn javafx:run
```

### Chạy Ở Dev Mode

Dev mode bật thêm một số hỗ trợ debug như overlay tọa độ tile:

```bash
mvn javafx:run -Pdev
```

### Chạy Test

```bash
mvn test
```

### Compile Không Chạy Test

```bash
mvn -DskipTests compile
```

## Cách Chơi

| Phím | Chức năng |
|---|---|
| `W`, `A`, `S`, `D` hoặc phím mũi tên | Di chuyển nhân vật |
| `Shift` + hướng di chuyển | Lướt / né đòn |
| `J` | Tấn công thường |
| `L` | Kích hoạt kỹ năng cường hóa |
| `K` | Kích hoạt kỹ năng đặc biệt / companion nếu đã mở khóa |
| `E` | Dùng Health Potion |
| `Q` | Dùng Mana Potion |
| `M` | Mở / đóng minimap |
| `ESC` | Pause game |
| `Space` hoặc chuột | Bỏ qua / tua nhanh một số đoạn hội thoại |

### Gameplay Cơ Bản

Người chơi bắt đầu từ tutorial để học di chuyển, tấn công và dùng kỹ năng. Sau đó, người chơi đi qua các level chính, đánh bại quái vật, mở rương bằng đòn đánh và thu thập phần thưởng.

Rương không được nhặt bằng va chạm. Người chơi cần đánh rương bằng `J`; sau khi mở, rương trao thưởng và biến mất.

## Cấu Trúc Package

```text
com.hust.game
├── audio          # Quản lý âm thanh, nhạc nền, hiệu ứng
├── collision      # Kiểm tra va chạm với map, tường, line-of-sight
├── combat         # Xử lý combat, sát thương, combo, hiệu ứng phần thưởng
├── constants      # Hằng số dùng chung cho game
├── dev            # Thiết lập dev mode
├── enemy          # Enemy base class và các loại quái
├── entities       # Các thực thể trong game
│   ├── ally        # Đồng minh / companion
│   ├── base        # BaseEntity, MovingEntity, StaticEntity
│   ├── environment # Cổng, vật thể môi trường
│   ├── interfaces  # Interface như Attackable, Damageable, Interactable
│   ├── items       # Potion, chest
│   ├── npc         # NPC và tương tác NPC
│   └── player      # Player, combat effect, merge controller
├── main           # Entry point, game loop, scene management
├── map            # Tile, TileType, MapManager
├── progression    # Level, GameManager, TutorialManager
└── ui             # HUD, menu, pause, settings, minimap, dialog
```

## Nền Tảng OOP Trong Project

### 1. Encapsulation - Đóng Gói

Các class quản lý dữ liệu và hành vi riêng của chúng. Ví dụ:

- `Player` quản lý máu, mana, di chuyển, tấn công và trạng thái kỹ năng.
- `Enemy` và các enemy con quản lý hành vi, HP, AI và render riêng.
- `MapManager` chịu trách nhiệm load tile map và entity trên bản đồ.

### 2. Inheritance - Kế Thừa

Project sử dụng hệ thống entity nền:

- `BaseEntity`: lớp gốc cho các thực thể có vị trí, sprite, kích thước render và boundary.
- `MovingEntity`: mở rộng `BaseEntity` cho các thực thể có thể di chuyển.
- `StaticEntity`: mở rộng `BaseEntity` cho vật thể đứng yên như item, chest, tile entity.

Enemy như `Slime`, `Knight`, `Tree`, `Witch`, `FinalBoss` được tổ chức quanh logic enemy chung.

### 3. Polymorphism - Đa Hình

Game dùng interface để xử lý hành vi chung:

- `Damageable`: đối tượng có thể nhận sát thương.
- `Attackable`: đối tượng có thể tấn công.
- `Collidable`: đối tượng có thể va chạm.
- `Interactable`: đối tượng có thể tương tác / nhặt.

Nhờ đó, các hệ thống như combat hoặc collision có thể làm việc với nhiều loại entity mà không cần phụ thuộc quá chặt vào class cụ thể.

### 4. Separation of Concerns - Tách Trách Nhiệm

Các module được chia theo vai trò:

- `CombatManager` xử lý tấn công, hitbox, reward và hiệu ứng combat.
- `GameManager` quản lý level hiện tại.
- `Level` spawn enemy, gate, NPC và cập nhật trạng thái màn chơi.
- `HUD`, `MenuScreen`, `PauseScreen`, `SettingsScreen` xử lý giao diện.
- `SoundManager` xử lý âm thanh tập trung.

Cách tách này giúp project dễ bảo trì, dễ sửa lỗi và dễ mở rộng thêm level, enemy hoặc item mới.

## Tài Nguyên Game

Tài nguyên nằm trong:

```text
src/main/resources
├── assets
│   ├── items       # Sprite vật phẩm: coin, potion, chest và hiệu ứng item
│   ├── maps        # File text định nghĩa tile map từng level và layer phụ
│   ├── tiles       # Sprite tile nền, tường, cổng, bụi cây, nhà, vật cản
│   ├── cursors     # Cursor riêng của game
│   └── ...         # Sprite nhân vật, enemy, UI, background, button
├── fonts           # Font pixel/font tiếng Việt dùng cho HUD, menu, dialog
└── sounds          # Nhạc nền, âm thanh tấn công, click button, potion, enemy
```

Các thư mục resource được load thông qua classpath JavaFX. Ví dụ, item như potion và chest được dùng bởi `entities/items`, map text được dùng bởi `MapManager`, tile sprite được dùng để render bản đồ, font được dùng bởi UI/dialog, còn sound được quản lý tập trung qua `SoundManager`.

## Ghi Chú Phát Triển

- Entry point chính là `com.hust.game.main.App`.
- Có thể chạy `mvn javafx:run -Pdev` để bật dev mode.
- Khi thêm asset mới, cần đặt đúng đường dẫn trong `src/main/resources`.
- Khi thêm entity mới, nên kế thừa từ `BaseEntity`, `MovingEntity` hoặc `StaticEntity` tùy hành vi.
- Khi thêm item có thể nhặt, cân nhắc dùng `Interactable`.
- Khi thêm vật thể mở bằng đòn đánh như chest, không nên dùng `Interactable` nếu không muốn player nhặt bằng va chạm.
