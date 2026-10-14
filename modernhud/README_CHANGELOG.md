# ModernHUD — rebuild sạch từ v2.0.0

## v3.2.2 — fix bug thật: attachment dots hiện sai (getAttachmentId không bao giờ null), dots canh
## phải thay vì canh giữa, giảm border outset, đổi icon ammo-type sang texture full-auto có sẵn
1. **Fix bug attachment dots hiện cả khi súng KHÔNG có phụ kiện nào — tìm ra root cause thật qua
   TACZ source, không phải đoán**: `IGun#getAttachmentId()` được đánh dấu `@Nonnull` trong chính API
   của TACZ — nó KHÔNG BAO GIỜ trả về `null`, kể cả khi slot trống! Khi trống, nó trả về sentinel
   `DefaultAssets.EMPTY_ATTACHMENT_ID` (xác nhận trực tiếp trong source TACZ, nơi mọi chỗ TACZ tự
   check "slot có trống không" đều dùng đúng so sánh này, không phải `!= null`). Code cũ check
   `!= null` nên luôn đúng bất kể có lắp gì hay không → mọi slot súng CHO PHÉP đều hiện vàng dù súng
   trần trụi hoàn toàn. Đã sửa dùng đúng `DefaultAssets.isEmptyAttachmentId(...)` (cũng là API public
   ổn định của TACZ, `com.tacz.guns.api.DefaultAssets`).
2. **Attachment dots canh phải (top-right) thay vì canh giữa** trong box icon chính, đúng yêu cầu.
3. **Giảm border outset mặc định 2→1px** (cả 2 file config) — 2px kết hợp với padding sẵn có khiến
   bracket nhìn "hơi xa" khỏi rìa box như bạn báo, dù kỹ thuật fill/border đã khớp nhau từ bản trước.
4. **Icon "ammo type" đổi từ 3 vạch vẽ tay sang dùng luôn texture full-auto có sẵn**
   (`fire_mode_auto.png`), tint theo màu reserve — nhìn đẹp/nhất quán hơn hẳn 3 gạch thô sơ trước đó.

## v3.2.1 — fix 4 lỗi thật từ ảnh in-game bạn gửi: glow mismatch border, fire-mode box sai kiểu viền,
## attachment dots lạc ra ngoài box, tắt glow default vì kỹ thuật hiện tại quá thô
1. **Glow không match viền/box — đã bỏ hẳn glow ở dòng ammo, quay lại flat fill dùng CHUNG kích thước
   với border**: nguyên nhân gốc là glow (`GlowEffect`) lan ra rộng hơn box thật (dùng để tính vị trí
   bracket), nên viền và "vùng sáng" không bao giờ khớp nhau — nhìn xấu/lệch như ảnh bạn gửi. Sửa:
   dòng ammo giờ dùng `HudPanel.draw()` (fill phẳng) với CHÍNH XÁC `boxX/boxY/boxW/boxH` mà border
   cũng dùng — 2 thứ giờ luôn khớp nhau tuyệt đối.
2. **Tắt TOÀN BỘ tính năng glow theo default** (`showWeaponGlow`, `showVitalsGlow`) — nhận ra vấn đề
   không chỉ ở dòng ammo mà là ở chính kỹ thuật `GlowEffect` (chỉ là vài rect chồng lệch vị trí +
   additive blend, không có blur thật) — nhìn "blocky/thô" ở BẤT KỲ đâu dùng nó, không riêng gì ammo.
   Giữ nguyên tính năng (glow khi đổi súng, pulse khi ít đạn/máu thấp) nhưng tắt default cho tới khi
   cải thiện kỹ thuật render tốt hơn — màu chữ vẫn đổi khi ít đạn/máu thấp như cũ nên không mất cảnh
   báo, chỉ là không còn glow phụ trợ.
3. **Fire-mode box bỏ hẳn 4-góc-bracket**: trước đây dùng chung `drawChipBox` nên bị áp luôn style
   viền chung (CORNERS mặc định = 4 góc bracket) — SAI, box này không nên có kiểu viền đó theo đúng
   yêu cầu. Giờ chỉ còn fill phẳng, không border ngoài nào cả — chỉ còn vạch chia giữa các ô + khung
   riêng cho ô đang active (đúng tinh thần "chế độ nào active thì đóng khung chế độ đó").
4. **Attachment dots chuyển hẳn vào TRONG shadowbox súng chính**: trước đây là 1 hàng riêng phía TRÊN
   box (cần chừa thêm chiều cao, trông "lạc" khỏi box khi glow/border không rõ) — giờ vẽ ĐÈ LÊN icon
   box, sát mép trên, bên trong ranh giới box thật. Chỉ hiện dot cho slot ĐÃ LẮP — slot trống hoặc
   súng không có loại đó thì không hiện gì cả (bỏ hẳn `attachmentDotEmptyColor`, không cần nữa). Revert
   `frameHeight` mặc định 68→60 vì không cần chừa hàng riêng nữa.

## v3.2.0 — tái hiện chính xác hơn theo ảnh MW4 tham chiếu: tách ammo/name/firemode thành 3 phần
## độc lập, corner bracket ra ngoài box, secondary box chấm góc dưới, dot phụ kiện trên icon chính
1. **Corner bracket ra NGOÀI shadowbox** (không còn nằm sát/trong cạnh box nữa): thêm tham số
   `outset` cho `HudPanel.drawCornerBrackets`/`drawBorder` — bracket giờ nổi cách mép box 1 khoảng
   (mặc định 2px), đúng như ảnh ref. Config: `borderOutset` (cả `modernhud-hud.toml` lẫn
   `modernhud-weapon.toml`).
2. **Tách ammo/name/fire-mode thành 3 phần ĐỘC LẬP** (sửa lại nhận định từ bản trước — nhìn kỹ ảnh ref
   lại thì chỉ CÓ RIÊNG dòng ammo mới có nền, không phải cả khối đóng khung):
   - **Dòng ammo** (`drawAmmoRow`): nền giờ là GLOW mềm (dùng `GlowEffect`) thay vì shadowbox phẳng,
     border outset riêng quanh đúng dòng này. Số 0 thừa (leading zero khi bật `zeroPadAmmo`) giờ mờ
     đi rõ rệt so với chữ số có nghĩa (`drawZeroPaddedNumber`) — áp dụng cho cả mag lẫn reserve.
     Thêm icon "3 vạch" (hamburger, vẽ bằng code không cần texture) cạnh reserve
     (`showAmmoTypeIcon`).
   - **Tên súng** (`drawNameRow`): chữ nổi trần, KHÔNG còn nền/box riêng, tách hẳn khỏi dòng ammo.
   - **Fire-mode** (`drawFireModeSegmented`): viết lại hoàn toàn thành segmented control — 1 box chia
     đều theo số chế độ, MỖI CHẾ ĐỘ 1 ô, chế độ đang active được đóng khung THÊM riêng bên trong (viền
     phụ quanh đúng ô đó), có vạch chia mảnh giữa các ô.
3. **Secondary weapon box**: đổi hẳn kiểu viền — bỏ 4 góc bracket, chỉ còn 2 chấm vuông nhỏ ở 2 GÓC
   DƯỚI, không có gì ở 2 góc trên (`HudPanel.drawBottomDots`, config `secondaryDotSize`).
4. **Dot phụ kiện trên icon súng chính** (tính năng mới hoàn toàn): thêm `TaczCompat.attachmentSlotCounts()`
   dùng đúng API public của TACZ (`IGun#allowAttachmentType` + `IGun#getAttachmentId`) — đếm chính
   xác gun này CÓ BAO NHIÊU slot phụ kiện (không phải lúc nào cũng đủ 6 loại: scope/muzzle/stock/grip/
   laser/extended-mag, tùy súng) và bao nhiêu đang lắp. Hiện thành hàng ô vuông nhỏ ngay trên icon
   chính, ô vàng = đã lắp, ô xám = trống (`showAttachmentDots`/`attachmentDotColor`/
   `attachmentDotEmptyColor`). Bump `frameHeight` mặc định 60→68px để chừa chỗ cho hàng dot này.
5. Đã tự rà soát và bắt được 1 lỗi cú pháp javadoc tự gây ra (thiếu dấu `{` mở trong comment) trước
   khi đóng gói — không phải chỉ trông cậy vào bạn build để bắt lỗi lần này.

## v3.1.0 — fix shadowbox "biến mất" (root cause thật), shadowbox riêng cho ammo/name/firemode, gold
## border flash + toggle bật/tắt slide animation
1. **Fix "shadowbox không compat với 1 số mod tối ưu, biến mất luôn" — tìm ra root cause thật qua
   code audit, không phải đoán**: vòng lặp vẽ icon fire-mode set `RenderSystem.setShaderColor()` làm
   mờ icon KHÔNG active, rồi gọi `g.blit()` — nếu texture đó lỗi/thiếu (conflict resource pack, hay
   bất kỳ lý do gì khiến blit throw exception) thì dòng RESTORE alpha về bình thường ngay sau đó bị
   BỎ QUA hoàn toàn vì không có try/finally. Vanilla shader gui NHÂN mọi màu fill với giá trị này —
   nên 1 lần lỗi ở icon fire-mode có thể làm MỌI shadowbox khác (kể cả không liên quan) render mờ đi
   hoặc biến mất hẳn cho tới hết frame đó. Tìm thêm 1 chỗ tương tự ở `TaczCompat.renderHudTexture`
   (còn nặng hơn — `pose.popPose()` cũng bị bỏ qua nếu blit throw, làm lệch pose stack toàn bộ phần
   còn lại của frame). Đã bọc try/finally đảm bảo restore 100% ở cả 2 chỗ trên + `drawCarriedIcon` +
   `FeedbackIconOverlay` — dù lỗi ở bất kỳ đâu, state global không bao giờ bị kẹt nữa.
2. **Lớp phòng thủ thứ 2 trong `HudPanel.java`**: MỌI lệnh fill/border giờ tự ép `shaderColor` về
   trắng đục ngay trước khi vẽ rồi khôi phục lại sau (try/finally) — không còn tin tưởng mù quáng vào
   GL state có sẵn từ bất kỳ đâu (mod khác, hay chính ModernHUD). Đây là fix triệt để nhất có thể làm
   mà không cần biết chính xác tên mod tối ưu bạn dùng — nếu vẫn còn thấy vấn đề, cho mình biết tên mod
   cụ thể để kiểm tra sâu hơn.
3. **Shadowbox riêng cho khối ammo+name+fire-mode**: tách hẳn khỏi màu/opacity của icon box (vẫn tối
   như cũ). Mặc định mới: trắng-xám nhạt (`0xE8ECF0`) ở opacity thấp (14%) — giống phong cách Modern
   Warfare 2026, thay vì nền tối như icon box. Toggle riêng `showTextPanel` — tắt hẳn cũng được nếu
   muốn hoàn toàn không có shadowbox ở khối này, chỉ còn viền (nếu bật). Config:
   `showTextPanel`/`textPanelColor`/`textPanelOpacity` (`modernhud-weapon.toml`).
4. **Animation khi đổi súng — làm lại theo hướng bạn đề xuất**: slide animation giờ có toggle bật/tắt
   riêng (`showSwitchSlide`, mặc định vẫn bật) + giảm khoảng cách trượt mặc định 16→8px (gọn hơn, bớt
   "lỏ"). Thêm **hiệu ứng nháy viền vàng gold** kiểu vanilla Minecraft (giống flash màn hình totem of
   undying) quanh icon chính khi đổi súng — nháy vàng rồi mờ dần về màu viền bình thường, ĐỘC LẬP với
   slide (chạy cả 2, chỉ 1, hoặc tắt hết tùy ý). Config: `showSwitchBorderFlash`/
   `switchBorderFlashColor`/`switchBorderFlashMs`/`switchSlideDistance`.

## v3.0.0 — bắt đầu "animation/rendering overhaul": eased animator thời gian thực + glow effect,
## áp dụng cho weapon hud và health bar trước (theo đúng yêu cầu bắt đầu từ đâu)
1. **Đã học kỹ thuật từ source `gd656killicon` bạn gửi** (mod kill-feed/combat-feedback rất chi
   tiết) — không copy code (kiến trúc 2 mod hoàn toàn khác nhau: bên đó config-driven qua JSON cho
   từng element, ModernHUD thì hard-code từng widget), mà học 2 kỹ thuật cốt lõi rồi viết lại hoàn
   toàn theo phong cách riêng của ModernHUD:
   - **`EasedAnimator.java`** (mới) — animator dựa trên THỜI GIAN THỰC (duration cố định tính bằng
     ms + easing curve: LINEAR/CUBIC_OUT/QUINTIC_OUT), thay cho kiểu "exponential decay theo từng
     frame" cũ (`BarRender.smooth()`) vốn phụ thuộc framerate và không bao giờ "đến đích" thật sự
     rõ ràng. Animator mới: retarget giữa chừng thì tiếp tục mượt từ giá trị hiện tại (không giật/
     restart), y hệt tinh thần "update target không phá animation" của `DigitalScrollEffect` bên
     mod tham chiếu.
   - **`GlowEffect.java`** (mới) — hiệu ứng glow/bloom giá rẻ: vẽ nhiều bản sao lệch vị trí của cùng
     1 rect với additive blend + alpha giảm dần ra ngoài, học từ `IconGlowRenderEffect` bên mod tham
     chiếu nhưng viết lại cho rect phẳng (health bar, ammo box) thay vì sprite icon.
2. **Health bar**: đổi từ smoothing cũ sang `EasedAnimator` (280ms, quintic ease-out) — cảm giác
   "premium" hơn hẳn, mượt đều bất kể framerate. Thêm **glow rung khi bị dính đòn** (flash đỏ ngắn,
   falloff bình phương cho cảm giác "hit" rõ) và **glow thở chậm khi máu thấp** (sine pulse, cùng
   công thức pulse đã dùng cho ammo alert). Toggle + màu chỉnh được qua `showVitalsGlow`/
   `vitalsGlowColor` (`modernhud-hud.toml`).
3. **Weapon hud**: animation "push up" đổi từ ease-out-quad sang **quintic ease-out** (khớp với
   `EasedAnimator`, cảm giác "chốt vào vị trí" gọn hơn). Thêm **glow flash khi đổi súng** (bùng sáng
   ngắn phía sau icon chính, kéo dài hơn animation trượt 1 chút để icon "lắng" lại rõ ràng) và
   **glow thở khi ít đạn** (pulse quanh cả khối ammo+name+firemode, dùng màu `ammoLowColor`). Toggle
   + màu chỉnh được qua `showWeaponGlow`/`weaponGlowColor` (`modernhud-weapon.toml`).
4. **Team HUD**: đồng bộ luôn — thanh máu đồng đội giờ cũng dùng `EasedAnimator` (mỗi người 1
   instance riêng qua `Map<UUID, EasedAnimator>`) thay vì smoothing cũ, nhất quán với health bar
   của bản thân.
5. **Còn tiếp tục ở các bản sau**: đây mới là bước đầu của "v3.0 overhaul" — animation/rendering
   cho player hud (head/name/absorption) và các phần còn lại của weapon hud (secondary box, fire
   type flash...) sẽ tiếp tục cải thiện nếu bạn xác nhận muốn đi tiếp theo hướng này.

## v2.10.1 — fix lỗi build TeamHudOverlay (Team vs PlayerTeam) + compat mới với Superb Warfare (SBW)
1. **Fix lỗi build thật từ log bạn gửi**: `Player#getTeam()` trả về `net.minecraft.world.scores.Team`
   (lớp cha), KHÔNG PHẢI `PlayerTeam` — `TeamHudOverlay.java` khai báo sai kiểu ở 2 chỗ gây lỗi
   compile y hệt log. Đã sửa cả 2 chỗ + import sang `Team`, giữ nguyên toàn bộ logic (Team vẫn có
   `getColor()`/`equals()` như đã dùng). Xin lỗi vì để lọt lỗi này — sandbox của mình không có mạng
   tới Forge/CurseForge nên không tự compile thử được, cảm ơn bạn đã build và gửi log lỗi thật.
2. **Compat mới: "[SBW] Superb Warfare"** (github.com/Mercurows/SuperbWarfare, modid `superbwarfare`)
   — đã tải source thật của mod để xác nhận: mọi vehicle (M1A2, Ztz99a, Truck, trực thăng, tàu, pháo...)
   đều kế thừa (trực tiếp hoặc qua base class trung gian như `GeoVehicleEntity`/`ArtilleryEntity`) từ
   1 class gốc duy nhất `com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity` — nên chỉ cần
   1 check `instanceof` là bắt được TẤT CẢ loại xe. Thêm `SuperbWarfareCompat.java` (soft-dependency
   qua reflection, không cần thêm Gradle dependency, giống cách đã làm với TaczPresence).
3. **Toàn bộ HUD của ModernHUD tự ẩn khi đang ngồi trên vehicle SBW** (mọi ghế — lái, xạ thủ, hành
   khách — không chỉ ghế lái, vì HUD của SBW che kín cùng 1 vùng màn hình dù ngồi ghế nào): player hud
   (máu/tên/armor), weapon hud (kể cả chế độ "neutral" khi mang súng không cầm), team hud, và ammo
   alert (RELOAD/NO AMMO/LOW AMMO) — cả 4 overlay đều check `SuperbWarfareCompat.isInVehicle()` ngay
   đầu `render()`. Không cần patch gì thêm nếu không cài SBW — check này trả về `false` ngay lập tức.

## v2.10.0 — Team HUD mới: thanh máu đồng đội theo vanilla scoreboard team, style Warzone
1. **Tính năng mới hoàn toàn**: `TeamHudOverlay.java` — mỗi đồng đội (cùng team scoreboard vanilla,
   `/team join`) hiện 1 dòng gọn: head icon + tên (màu theo màu team vanilla, có fallback riêng) +
   thanh máu mượt (dùng chung cơ chế smoothing `BarRender.smooth()` như HUD của bản thân, nhưng mỗi
   người 1 trạng thái riêng qua `Map<UUID, Float>` — không bị trộn lẫn animation giữa các đồng đội).
   Xếp CHỒNG LÊN TRÊN player hud của mình, chung 1 box nền + viền RIÊNG (tách biệt hoàn toàn khỏi
   panel của player hud chính) — đúng tinh thần "1 box riêng" bạn yêu cầu.
2. **Giới hạn 6 người, sắp xếp ổn định**: `teamHudMaxPlayers` (mặc định 6, chỉnh 1-16) — nhiều hơn thì
   phần dư bị ẩn, sắp xếp theo tên A-Z để việc "ai bị ẩn" luôn nhất quán giữa các frame thay vì đảo
   lộn ngẫu nhiên. Kick 1 người khỏi team hoặc có người rời/hạ số lượng xuống dưới giới hạn thì người
   bị ẩn trước đó sẽ tự hiện lại (không cần restart/reload gì).
3. **Giới hạn kỹ thuật cần biết rõ**: đây dùng đúng hệ thống team vanilla của Minecraft — client chỉ
   đọc được máu/tên của đồng đội khi entity của họ THỰC SỰ được load/track ở phía client (đúng phạm vi
   mà vanilla vốn đã hiện tên/máu người chơi khác). Đồng đội ở xa/khác dimension sẽ đơn giản là KHÔNG
   hiện cho tới khi họ vào lại phạm vi — đây là giới hạn cố hữu của việc không có hệ thống đồng bộ dữ
   liệu phía server riêng, không phải bug.
4. **Config đầy đủ** (`modernhud-hud.toml`, mục `teamHud`): `enabled`, `maxPlayers`, `barWidth`
   (mặc định 64px), `rowGap`, `groupGap` (khoảng cách với player hud chính), `showHead`,
   `useTeamColor` (bật = màu tên theo team vanilla, tắt = dùng `nameColor` cố định), `nameColor`
   (hex, fallback khi team không có màu).
5. **Lưu ý về vị trí căn chỉnh**: team hud căn trái theo player hud chính — khớp hoàn hảo với default
   BOTTOM_LEFT, nhưng nếu bạn đổi player hud sang neo bên phải (TOP_RIGHT/BOTTOM_RIGHT) mà 2 box có độ
   rộng khác nhau thì cạnh phải sẽ không thẳng hàng — chấp nhận đánh đổi này để giữ bản đầu tiên đơn
   giản, có thể cải thiện sau nếu cần.

## v2.9.1 — fix Fire Type flash (X/Y offset, kiểu hiển thị), reserve màu trắng-xám, bỏ drag player hud
1. **Fire Type flash — hoàn thiện phần config đã thêm dở ở bản trước**: bản 2.9.0 đã thêm config
   `fireTypeXOffset`/`fireTypeShowLabel` nhưng code vẽ (`drawFireTypeFlash`) quên chưa dùng tới — giờ
   đã nối đúng: có X offset thật sự, Y offset đổi hướng dấu giống hệt `ammoAlertYOffset` (dương =
   xuống dưới, ÂM ĐƯỢC PHÉP — trước đây range chỉ 0-400 nên không kéo xuống được, giờ -2000 đến 2000),
   default Y đổi từ +46 (đẩy lên) thành -40 theo quy ước dấu mới, scale mặc định giảm 1.3→1.0 (bớt "to
   đùng"). Thêm toggle `fireTypeShowLabel`: bật = "FIRE TYPE: FULL AUTO" như hiện tại, tắt = chỉ
   "FULL AUTO" không có tiền tố.
2. **Reserve ammo color**: đổi default từ vàng gold sang trắng-xám (`0xB8BFC6`) theo đúng yêu cầu.
   Đã rà soát kỹ lại TOÀN BỘ hệ màu (cả `HudConfig.java` lẫn `WeaponConfig.java`) — không tìm thấy bug
   nào trong code, `reserveColor` đã luôn được wire đúng và config được từ trước. Nguyên nhân thật của
   cảm giác "không config được": màn hình kéo-thả trong game (`HudEditorScreen`, phím tắt Controls >
   Modern HUD) CHỈ chỉnh vị trí, không hề có màu — màu chỉ nằm trong file `modernhud-weapon.toml` /
   `modernhud-hud.toml` (mục `colors`), cần sửa trực tiếp trong đó hoặc qua mod xem config dạng GUI
   nếu có cài (vd Configured). Đã làm rõ hơn trong tooltip màn hình editor.
3. **Player HUD: bỏ hẳn kéo-thả, cố định theo config**: xóa handle "Player HUD" khỏi
   `HudEditorScreen` — giờ CHỈ còn Hit/Break Icon kéo-thả được. Player HUD giờ đối xử y hệt weapon
   hud: neo theo góc + offset trong file config (`modernhud-hud.toml`, mục `position`), không còn kéo
   bằng chuột được nữa. Default sẵn đã là `BOTTOM_LEFT` từ trước (không đổi). Xóa luôn hàm
   `HudConfig.setMainPosition()` (dead code sau khi bỏ drag handle).

**Còn nợ lại (chưa làm trong bản này):** tính năng Team HUD (hiển thị thanh máu đồng đội theo vanilla
scoreboard team, xếp chồng lên trên player hud, tối đa 6 người, style Warzone) — đây là tính năng MỚI
khá lớn (cần đọc scoreboard team, track nhiều entity cùng lúc, smoothing riêng từng người...), chưa bắt
đầu code do ưu tiên fix các lỗi cấp bách trước. Sẽ làm ở bản tiếp theo nếu bạn xác nhận vẫn cần.

## v2.9.0 — gộp ammo+name+fire-mode thành 1 khối duy nhất đúng ảnh ref, màu HEX, fire type flash mới
1. **Hệ màu → HEX string thay vì số thập phân**: toàn bộ 16+ config màu (panel, border, ammo, reserve,
   name, fire mode, alert...) giờ nhập bằng mã hex (vd `"E8B84C"` hoặc `"#E8B84C"`) thay vì số thập
   phân khó đọc (`15263308`). File mới `ColorConfig.java` xử lý parse/format (chấp nhận có/không dấu
   `#` hoặc `0x`, sai định dạng thì fallback về màu mặc định, không crash).
2. **Reserve ammo**: thêm lại dấu `|` phân cách (đã bỏ nhầm ở bản trước), reserve giờ nhỏ hơn mag
   theo tỷ lệ chỉnh được (`reserveScaleOffset`, mặc định 0.1) và mờ hơn theo opacity riêng
   (`reserveOpacity`, mặc định 80%) — đồng thời fix luôn lỗi số reserve bị lệch tâm so với mag khi 2
   chữ ở 2 cỡ khác nhau (căn giữa theo chiều cao dòng của mag, không còn "không đều" nữa).
3. **Bố cục lại đúng ảnh MW4 tham chiếu — thay đổi lớn nhất bản này**: gộp mag|reserve + tên súng +
   fire-mode thành MỘT khối duy nhất, chung MỘT nền + MỘT viền bao quanh cả 3 dòng (trước đây là 3 box
   riêng biệt, không khớp ảnh gốc). Tên súng giờ luôn hiển thị (không còn fade/toast tạm thời như
   trước), nằm ngay dưới dòng ammo, fire-mode nằm dưới cùng — đúng thứ tự trong ảnh. Xóa hẳn hệ thống
   "name toast" cũ (`showNameToast`→`showName`, xóa `nameHoldMs`/`nameFadeMs` vì không còn ý nghĩa).
4. **Secondary weapon box đúng ảnh ref**: box giờ RỘNG BẰNG box súng chính (trước đây nhỏ hơn, lệch),
   icon súng lệch về bên phải/trong của box, số đạn reserve chiếm khoảng trống bên trái — 1 box chứa
   cả 2 thứ như trong ảnh (pistol icon + "40"), không phải icon riêng + số ở chỗ khác. Tighten spacing:
   giảm khoảng cách giữa icon chính/phụ (4→2px) và giữa các dòng trong khối ammo (3→2px).
5. **Thêm "FIRE TYPE: FULL AUTO" flash announcement** khi rút súng (yêu cầu #7): chữ hiện giữa màn
   hình, phía trên tâm ngắm, nháy màu vàng → trắng → mờ dần hết hẳn, gạch chân riêng phần tên chế độ
   bắn (không gạch "FIRE TYPE: "). Timing/màu sắc đều chỉnh được qua config (`fireTypeFlashMs`,
   `fireTypeHoldMs`, `fireTypeFadeMs`, `fireTypeFlashColor`, `fireTypeHoldColor`, `fireTypeScale`,
   `fireTypeYOffset`). Dùng chung timer với animation trượt icon nên chỉ chạy đúng 1 lần mỗi lần đổi
   súng, không lặp lại liên tục.
6. Bump `frameWidth` mặc định 148→160px để chừa đủ chỗ cho khối ammo+name+firemode rộng hơn (do gộp
   3 dòng vào 1 box thay vì chia nhỏ như trước).
7. **playerhud bottom-left**: đã xác nhận default sẵn là `BOTTOM_LEFT` từ trước, không cần sửa gì.

## v2.8.4 — corner bracket nhỏ lại + tỷ lệ theo box, thêm 3 kiểu viền chọn được, gộp ammo+reserve
## thành 1 ô đúng ảnh ref, thêm reserve ammo cho súng phụ/neutral
1. **Corner bracket "to quá, vướng mắt" → đã fix tận gốc**: trước đây `cornerBracketLength` là số px
   CỐ ĐỊNH áp dụng như nhau cho MỌI box, kể cả các chip nhỏ (mag/reserve/fire-mode) — nên cùng 1 độ
   dài trông ổn trên icon box to (62x36) nhưng lại "chunky"/vướng mắt trên chip nhỏ. Đã sửa: thêm giới
   hạn tỷ lệ `min(w,h) * 0.3` trong `HudPanel.drawCornerBrackets` — cùng 1 config length giờ tự động
   nhỏ lại trên box nhỏ, đạt full size trên box to. Ngoài ra giảm luôn default: weapon 3→2px, hud
   máu 4→3px.
2. **Thêm 3 kiểu hiển thị viền chọn được** qua config mới `borderStyle` (enum `NONE`/`CORNERS`/`FULL`),
   thay cho toggle bật/tắt cũ:
   - `NONE` — không viền gì cả, chỉ còn nền phẳng (nếu panel cũng đang bật)
   - `CORNERS` — brackets góc như hiện tại (mặc định, giữ nguyên hành vi cũ)
   - `FULL` — viền liền mỏng 1px bao trọn 4 cạnh (hàm mới `HudPanel.drawFullBorder`)
   Áp dụng đồng bộ cho cả HUD máu (`HudConfig.borderStyle`) lẫn HUD súng (`WeaponConfig.borderStyle`)
   qua 1 hàm dispatch chung `HudPanel.drawBorder()`. Đổi tên toàn bộ config liên quan từ
   `cornerBracket*`/`showCornerBrackets` sang `border*` cho đúng ý nghĩa mới (không còn chỉ là corner
   bracket nữa).
3. **Gộp mag + reserve thành 1 ô duy nhất, đúng bố cục ảnh ref** ("030 060"): trước đây là 2 chip xếp
   chồng (mag trên, reserve dưới), giờ nằm CHUNG 1 box, cạnh nhau theo chiều ngang, cùng cỡ chữ, chỉ
   khác màu (mag trắng/đỏ khi ít đạn, reserve vàng gold) để phân biệt — bỏ luôn ký tự "|" phân cách,
   chỉ còn khoảng trắng giữa 2 số như trong ảnh. Fire-mode row vẫn nằm ngay dưới ô này như cũ.
4. **Thêm reserve ammo count cho súng phụ (secondary) VÀ chế độ neutral (mang súng không cầm)**: gộp
   2 hàm vẽ icon trùng lặp (`drawSecondaryWeapon`/`drawNeutralIcon`) thành 1 helper dùng chung
   `drawCarriedIcon()` — vừa vẽ icon vừa vẽ số đạn reserve (không zero-pad) ngay trong CÙNG box với
   icon, đúng như ảnh ref (pistol icon + "40"). Cả secondary lẫn 2 khẩu ở trạng thái neutral giờ đều
   hiện số đạn dự trữ như nhau. Bump kích thước box phụ 40x16 → 44x18 để đủ chỗ cho cả icon lẫn số.

## v2.8.3 — làm lại giao diện weapon hud theo hướng "MW4-style": box riêng từng element, corner
## brackets, reserve gold, chế độ "đang mang súng nhưng không cầm"
1. **Reserve ammo → màu gold**: đổi màu mặc định của số đạn dự trữ (dòng dưới, nhỏ hơn số đạn trong
   băng) sang vàng gold (`0xE8B84C`), tách biệt rõ với số đạn chính màu trắng phía trên.
2. **Mỗi element trên weapon hud giờ có box riêng, KHÔNG còn dồn chung 1 cục**: tách hẳn số đạn trong
   băng, số đạn dự trữ, và hàng fire-mode thành 3 "chip" riêng biệt — mỗi cái tự đo kích thước ôm sát
   nội dung của nó (`CHIP_PAD_X`/`CHIP_PAD_Y`), có khoảng hở (`CHIP_GAP_Y`) giữa các chip để trông
   tách bạch, không dính liền thành 1 khối. Icon súng chính + súng phụ vẫn có box riêng như cũ. Chỉ
   riêng tên súng (name toast) là KHÔNG có box — vẫn là chữ nổi trần như trước, đúng yêu cầu.
3. **Thêm 4 góc bracket nhỏ kiểu MW4 (2026)** — dựa đúng ảnh HUD MW4 bạn gửi: 2 đoạn ngắn mỗi góc tạo
   hình chữ "L" mở, KHÔNG phải viền liền tục bao quanh cả box. Bật/tắt riêng qua config
   `showCornerBrackets` (mặc định BẬT), chỉ vẽ khi panel/fade box cũng đang bật (brackets là lớp phủ
   THÊM lên trên fill, không phải thay thế). Áp dụng cho MỌI chip (icon chính/phụ, số đạn, reserve,
   fire-mode) ở cả HUD máu lẫn HUD súng — dùng chung 1 hàm `HudPanel.drawCornerBrackets()` nên đồng bộ
   khắp nơi. Có thêm config riêng cho màu (`cornerBracketColor`, mặc định trắng ngà `0xE8ECF0`), độ
   đậm (`cornerBracketOpacity`, mặc định 90% — đậm hơn hẳn fill để luôn rõ nét) và độ dài cạnh
   (`cornerBracketLength`, mặc định 3-4px, dày cố định 1px).
4. **Chế độ "đang mang súng nhưng không cầm trên tay"**: trước đây cầm bất kỳ item nào khác ngoài
   súng TACZ là widget biến mất hoàn toàn. Giờ nếu vẫn còn súng trong hotbar (dù không cầm), widget
   vẫn hiện — nhưng KHÔNG có tên súng/số đạn (không có gì để đọc vì không đang cầm khẩu nào cả). Hiện
   tối đa 2 khẩu súng tìm thấy trong hotbar, xếp theo thứ tự slot (khẩu ở slot nhỏ hơn hiện trước), CẢ
   HAI đều dùng chung kiểu thu nhỏ/mờ như ô "secondary" hiện tại — không khẩu nào được highlight làm
   "chính". Lịch sử chọn súng (`currentGunId`/`secondaryGunId`) dùng khi đang thực sự cầm súng được
   giữ nguyên không đụng tới, nên cầm súng thật trở lại thì primary/secondary tiếp tục đúng như trước.
5. **Về yêu cầu bố cục "modern/sleek như MW4"**: mình không tìm được ảnh/tư liệu đáng tin cậy nào về
   giao diện HUD thật của game "Modern Warfare 4" qua tìm kiếm (có thể game chưa phát hành công khai
   hoặc tên khác) — nên phần layout tổng thể (box riêng từng phần tử, khoảng cách gọn, phối màu
   vàng-trắng) mình tự thiết kế theo hướng "tactical HUD hiện đại" nói chung. Bracket góc ở mục 3 thì
   dựa ĐÚNG theo ảnh bạn gửi (không phải suy đoán).

## v2.8.2 — panel width bị dư khúc thừa, armor bar → text-only, tự nhận diện súng thứ 2
1. **Fix "healthbar background box đang bị dư ra một khúc"**: nguyên nhân là `totalWidth` cũ tính
   bằng công thức cố định `barWidth + 44` — con số `44` là chỗ dành sẵn "phòng khi" có chữ HP/Armor
   nằm cuối thanh (END_OF_BAR), NHƯNG được cộng vào bất kể có thật sự vẽ chữ ở đó hay không. Nếu bạn
   để HP/Armor text ở vị trí khác (hoặc tắt hẳn), 44px đó vẫn bị chừa ra trong panel → thành 1 khúc
   nền tô màu (giờ là màu phẳng, không còn fade để "giấu" nó như trước) mà không có gì bên trong —
   đúng là "một khúc nhìn rất xấu". Đã sửa tận gốc: `layoutPass()` giờ tự đo width thật cần dùng ở
   TỪNG dòng (bar, chữ cuối thanh, dòng thông tin chung, chữ "+N" khiên dư) và lấy max — panel giờ
   luôn ôm sát đúng nội dung thực tế đang vẽ, không còn khoảng dư nào cả.
2. **Bỏ hẳn armor bar, chuyển thành text-only** (vd "20/20"): không còn thanh armor (rectangle) nữa,
   chỉ còn 1 dòng chữ — dùng đúng định dạng `armorTextFormat` sẵn có (CLASSIC "12/20" / PERCENT
   "60%"). Gộp `showArmorBar` + `showArmorText` cũ thành 1 toggle duy nhất `showArmor` (bật là có
   chữ, tắt là ẩn hẳn — không còn khái niệm "bar bật, chữ tắt" nữa vì chữ giờ LÀ toàn bộ armor). Vị
   trí `END_OF_BAR`/`BELOW_BAR` (không còn ý nghĩa "cuối thanh" vì hết thanh) giờ đều nghĩa là "dòng
   riêng ngay dưới thanh máu"; `BELOW_NAME_HEAD` vẫn như cũ, nằm chung dòng với HP text phía trên.
3. **Tự động nhận diện súng thứ 2 (primary + secondary) thay vì phải lăn chuột qua lại**: trước đây
   ô súng phụ CHỈ được ghi nhận sau khi bạn đã từng đổi từ súng đó sang súng khác (dựa lịch sử scroll)
   — nếu bạn cầm sẵn 2 súng từ đầu mà chưa từng đổi qua lại, ô phụ để trống dù đang cầm đủ 2 súng. Đã
   sửa: nếu chưa có "lịch sử" (hoặc súng nhớ trong lịch sử không còn trong hotbar nữa), tự động quét
   hotbar tìm khẩu súng KHÁC đang có sẵn và hiện luôn — không cần lăn chuột qua lại trước nữa.
4. Xác nhận lại: healthbar background panel đã dùng opacity phẳng (không fade) từ bản v2.8.1 — bản
   này chỉ còn sửa nốt phần kích thước bị dư khúc ở trên, không có gradient/fade nào còn sót lại
   trong code (đã rà lại toàn bộ `HudPanel`/`PlayerHudOverlay`/`BarRender`, không còn `fillGradient`).

## v2.8.1 — Fix ammo "000" cho súng inv+dummy ammo, HUD đè nhau, animation lem sang text, fade box
1. **Fix ammo hiện "000" cho súng dùng inv ammo (M134 Minigun)**: nguyên nhân thật — `magAmmo()` bị
   mất nhánh check `useDummyAmmo` (có ở v2.7.0 nhưng bị rớt mất lúc build tiếp lên 2.8.0). Đối chiếu
   ngược với source thật của TACZ (`GunHudOverlay.handleCacheCount`): một khẩu có thể VỪA
   `useInventoryAmmo` VỪA `useDummyAmmo` cùng lúc — đạn là số ảo lưu trong NBT (đúng kiểu súng
   minigun gắn xe của bạn: đạn nạp từ kho xe, không phải item thật trong túi đồ người chơi), không hề
   có item đạn thật nào trong inventory để quét. Số "158" màu cyan trong ảnh bạn gửi chính là mã màu
   riêng TACZ dùng cho đúng combo `useInventoryAmmo && useDummyAmmo` này — bằng chứng trực tiếp. Code
   cũ luôn tự quét `player.getInventory()` bất kể `useDummyAmmo`, thấy 0 đạn thật → ra "000". Đã sửa:
   check `useDummyAmmo` TRƯỚC, dùng `getDummyAmmoAmount()` khi đúng — đúng thứ tự ưu tiên TACZ dùng.
2. **Fix "HUD đôi lúc bị đè lên nhau"**: trước chỉ gọi `setVanillaGunHudEnabled(false)` MỘT LẦN lúc
   load config — nếu có gì khác (TACZ tự reload config khi đổi gunpack, addon khác đụng chung cờ
   `RenderConfig.GUN_HUD_ENABLE`, v.v.) bật lại cờ đó, ModernHUD không hề hay biết, HUD gốc TACZ hiện
   trở lại đè lên HUD của mình. Đã sửa: `WeaponHudOverlay` giờ tự re-assert lại cờ này mỗi ~1 giây khi
   đang active, HUD TACZ gốc không thể "giành lại" màn hình quá 1 giây. Thêm cả fallback reflection
   (set field trực tiếp nếu nó là boolean thường thay vì bọc trong ForgeConfigSpec ConfigValue).
3. **Fix animation "push up" lem sang cả ammo/text**: đúng như bạn báo — trước đó CẢ pose (icon, số
   đạn, reserve, fire-mode row, tên súng) bị translate theo animation cùng lúc. Giờ tách riêng: chỉ
   icon súng chính + icon phụ nằm trong pose có animation trượt, còn cột số đạn/reserve/fire-mode/tên
   súng nằm trong pose KHÔNG animation, đứng yên tuyệt đối khi đổi súng.
4. **Bỏ hẳn "fade box" (gradient), thay bằng nền phẳng chỉnh opacity 0-100%**: đúng yêu cầu — không
   còn fade/gradient hướng nào nữa. `HudPanel` giờ chỉ có 1 hàm `draw()` tô 1 màu phẳng ở opacity chỉ
   định. Thêm config mới `panelColor` (0xRRGGBB) + `panelOpacity` (0-100) cho cả HUD máu
   (`modernhud-hud.toml`, mặc định 45%) và HUD súng (`modernhud-weapon.toml`, mặc định 30% — vẫn nhạt
   hơn HUD máu theo đúng tinh thần cũ). Ô súng phụ (secondary) còn nhân thêm `secondaryAlpha` vào
   opacity nền để mờ đồng bộ với icon mờ của nó.

## v2.8.0 — Fix hướng fade health bar, tích hợp 3-way HUD selector với TaczPresence
1. **Fix fade health bar bị ngược**: đúng như bạn báo — hướng fade bị lộn ngược so với ý định (đậm
   bên trái/tên, nhạt dần sang phải mới đúng, code cũ ra ngược lại). Đã đảo lại 2 màu trong
   `HudPanel.drawHorizontal`.
2. **Tích hợp 3-way HUD selector với TaczPresence**: TaczPresence có sẵn `PresenceConfig.GUN_HUD_TYPE`
   (enum `TACZ_HUD`/`TACZ_P_HUD`) để chọn hiển thị HUD súng nào. Không thể tự thêm 1 giá trị vào enum
   của MOD KHÁC (đó là bytecode đã compile sẵn của họ), nên ModernHUD làm bên mình một enum tương ứng
   `WeaponHudSource { TACZ_HUD, TACZ_P_HUD, MODERNHUD }` (config mới, thay cho `enabled` boolean cũ) —
   chọn cái nào thì ModernHUD tự đảm bảo CHỈ đúng 1 HUD đó hiện ra: đảo `RenderConfig.GUN_HUD_ENABLE`
   của TACZ (tắt cả TACZ gốc lẫn TaczPresence — 2 cái đó dùng chung 1 cờ này để quyết định có tự vẽ
   hay không), VÀ nếu chọn TACZ_HUD/TACZ_P_HUD thì còn set luôn `PresenceConfig.GUN_HUD_TYPE` tương
   ứng (khi TaczPresence có cài) để đúng đích danh 1 trong 2 kiểu của họ hiện lên, không phải đoán.
   Toàn bộ đọc/ghi qua reflection (an toàn, không crash nếu TaczPresence đổi API hoặc không cài).
3. **Keybind giờ CYCLE qua đúng 3 lựa chọn** thay vì chỉ toggle on/off như trước: ModernHUD → TACZ
   gốc → TaczPresence (nếu có cài, bỏ qua nếu không) → quay lại ModernHUD.

## v2.7.0 — Fix bug ammo "000", luân phiên với TaczPresence, thiết kế lại fade box, cảnh báo đạn
1. **Fix bug ammo hiện "000" cho súng dùng inventory ammo**: nguyên nhân thật — code cũ tự so sánh
   `ammoId` của viên đạn với `.equals()` thay vì gọi đúng `IAmmo.isAmmoOfGun(gun, ammo)`, method
   compatibility thật sự mà TACZ dùng (không phải lúc nào cũng match kiểu so sánh id đơn giản). Đối
   chiếu lại với `AmmoCacheCounter` của TaczPresence để sửa đúng, đồng thời bỏ luôn việc tự ý giới
   hạn số hiện ra theo `magCapacity` cho nhóm súng này (chúng không có "mag" thật, số hiện ra phải
   đúng bằng tổng đạn rời trong túi, TaczPresence cũng không cắt bớt).
2. **Luân phiên (thay vì đè cứng) với TACZ / TaczPresence**: thêm keybind mới (chưa gán phím mặc
   định) để bật/tắt weapon widget của ModernHUD VÀ tự động đảo ngược `RenderConfig.GUN_HUD_ENABLE`
   của TACZ tương ứng — tạo hiệu ứng luân phiên thật giữa "HUD của ModernHUD" và "HUD TACZ đang hiện
   hành (dù là bản gốc hay của TaczPresence)", thay vì việc ModernHUD chỉ luôn đè cứng HUD kia off.
3. **Thiết kế lại hoàn toàn "fade box"**: bỏ hẳn viền góc bo màu xanh biển (corner accent brackets) —
   không còn viền/khung nào nữa, chỉ còn duy nhất phần fade. HUD máu: fade NGANG trái (nhạt) → phải
   (đậm), có 1 đường line trắng-xám RÕ dọc theo đáy. HUD súng: fade DỌC dưới (đậm, gần số đạn) → trên
   (nhạt), độ đậm nhạt hơn hẳn so với HUD máu — đúng theo ảnh tham khảo gửi kèm. `showPanel` vẫn tắt
   bật được như cũ cho cả 2 HUD (mặc định bật lại, vì giờ nó là fade thuần, không còn viền gây khó
   chịu nữa). Bỏ hẳn config `panelAccentColor` (không còn cần vì hết viền).
4. **Ô súng phụ (secondary) giờ có fade box đúng scale**: trước đó bị thiếu hoàn toàn phần nền cho
   ô phụ, giờ dùng đúng kiểu fade dọc của HUD súng, thu nhỏ theo đúng kích thước ô phụ.
5. **Bỏ ký tự "/" trên reserve ammo → đổi thành "|"** (áp dụng cả cho trường hợp đạn vô hạn "|∞").
6. **Animation "push up" khi đổi súng**: cả khung HUD súng giờ trượt lên từ dưới vào đúng vị trí
   trong ~220ms mỗi khi đổi súng (easing), dùng chung timer với toast tên súng.
7. **Cảnh báo RELOAD / NO AMMO / LOW AMMO kiểu TaczPresence**: overlay mới, chữ nhấp nháy (pulse)
   giữa màn hình khi băng đạn hết (RELOAD nếu còn đạn dự trữ, NO AMMO nếu hết sạch) hoặc dưới 20%
   (LOW AMMO, ngưỡng chỉnh được). Bỏ qua cho súng dùng inventory ammo (không có "mag" để cạn) —
   đúng như cách TaczPresence xử lý.

## v2.6.1 — Fix mất cân xứng weapon HUD, bỏ shadowbox mặc định, absorption full xanh biển, fire-mode icon
Fix trực tiếp theo ảnh chụp thực tế (M4 ADS) — soi kỹ thấy 2 lớp viền accent lồng nhau (khung ngoài +
khung icon) và số đạn dự trữ bị lệch hàng so với số đạn chính.
1. **Bỏ hẳn khung viền lồng nhau ("shadowbox")**: trước đó vẽ 2 lớp panel — 1 lớp bọc toàn bộ widget,
   1 lớp riêng bọc icon — chồng lên nhau nhìn rối và mất cân xứng. Giờ CHỈ còn tối đa 1 panel duy nhất,
   bọc riêng icon súng thôi (không có panel bọc số đạn/tên súng nữa, y hệt HUD gốc CoD — chữ chỉ có
   shadow, không có khung nền). Panel này giờ **tắt theo mặc định** (`showPanel: false` cho cả weapon
   HUD lẫn player HUD) — bật lại được qua config nếu muốn.
2. **Absorption mặc định full xanh biển đậm**: `absorptionColorMode` đổi mặc định từ `TIERED` sang
   `CUSTOM` — ra khỏi hộp là full 1 màu xanh biển đậm (`0x1C7ED6`) luôn, không cần chỉnh gì thêm.
   Xác nhận lại: màu xanh này CHỈ áp dụng cho thanh Absorption trên health bar, không liên quan gì
   đến viền khung panel (2 cái tách biệt hoàn toàn, panel giờ mặc định tắt nên càng rõ ràng).
3. **Fix lệch số đạn chính/dự trữ**: 2 số giờ luôn xếp chồng đúng 1 cột, canh phải cùng 1 mép — số
   chính ở trên, số dự trữ ngay dưới cách 1px, không còn kiểu nửa-cùng-hàng-nửa-lệch-hàng như trước.
4. **Weapon HUD cố định vị trí, chỉ chỉnh qua config**: bỏ hẳn "Weapon HUD" ra khỏi HUD Editor kéo-thả
   — vị trí giờ CHỈ chỉnh được qua `modernhud-weapon.toml` (đúng yêu cầu, vì kéo-thả bằng chuột không
   chính xác giữa các tỉ lệ màn hình khác nhau). HUD Editor có thêm dòng nhắc việc này khi TACZ có mặt.
5. **Tên súng chuyển lên TRÊN icon**: toast tên súng giờ hiện ngay phía trên khung icon (canh phải theo
   mép icon), không còn nằm phía dưới dễ đè lên các phần tử khác nữa.
6. **Fire mode giờ có ICON riêng (dùng icon bạn cung cấp)**: thêm 3 icon `fire_mode_semi/auto/burst.png`
   vào `textures/hud/`. Súng có NHIỀU chế độ bắn → hiện hàng icon, chế độ đang dùng sáng + có gạch chân,
   các chế độ khác mờ đi (y hệt TaczPresence). Súng chỉ có ĐÚNG 1 chế độ → không hiện gì cả, đúng yêu cầu.
7. **Fix bonus**: bộ 3 icon phụ (secondary weapon) giờ nằm hẳn trong 1 dải riêng NGAY DƯỚI khung icon
   chính — tách biệt hoàn toàn khỏi cột chữ (số đạn/tên/fire-mode), không còn khả năng đè lên "AUTO"
   hay bất kỳ chữ nào như trước nữa, bất kể kích thước khung.

## v2.6.0 — Bỏ compass/gunfire, weapon HUD kiểu Call of Duty, primary/secondary weapon, fix bug alpha
1. **Bỏ hẳn Compass + Gunfire detection**: xoá toàn bộ `CompassOverlay`, `CompassConfig`,
   `client/compass/*` (DiamondRender, GunfireBlips, GunfireListener), keybind toggle compass, config
   `modernhud-compass.toml`, và mọi chỗ đăng ký liên quan — vì đã có mod khác lo phần này rồi.
2. **Fix nguyên nhân thật của việc icon súng bị "lệch tệ hại"**: hoá ra TACZ có sẵn HUD súng riêng
   của nó (`GunHudOverlay`), và nó vẫn chạy song song với HUD của ModernHUD — 2 HUD đè lên nhau,
   icon to nhỏ lẫn lộn, đúng như ảnh chụp gửi kèm. Giờ ModernHUD tự tắt HUD súng gốc của TACZ (qua
   `RenderConfig.GUN_HUD_ENABLE`, đọc bằng reflection để an toàn nếu TACZ đổi API) mỗi khi
   `weapon widget` của ModernHUD bật — tắt widget của ModernHUD thì trả lại HUD gốc của TACZ, không
   bao giờ để người chơi không có gì cả.
3. **Fix bug alpha khiến toàn bộ đường viền góc (corner accent) và màu absorption custom bị VÔ
   HÌNH từ trước tới giờ**: màu lấy từ config luôn ở dạng `0xRRGGBB` (không có byte alpha), nhưng
   `GuiGraphics#fill` (khác với `drawString`) KHÔNG tự ép opaque khi alpha=0 — nghĩa là mọi đường
   viền góc accent (cả HUD người chơi lẫn HUD súng) và màu absorption custom trước giờ vẽ ra hoàn
   toàn trong suốt, không hề hiển thị. Đã fix tận gốc trong `HudPanel.draw` và `BarRender.plates`
   bằng cách luôn ép opaque ngay tại chỗ vẽ.
4. **Redesign weapon HUD theo phong cách Call of Duty (Modern Warfare)**: số đạn mag/reserve dạng
   in đậm, zero-pad 3 chữ số kiểu MW ("030"/"060", tắt được qua config), icon súng nằm trong khung
   accent-bracket cố định riêng bên phải, tên súng vẫn là toast hiện lên rồi mờ dần khi đổi súng
   (giữ đúng yêu cầu trước đó — "khung cố định, tên không làm phình khung") nhưng canh vị trí sát
   khu vực số đạn hơn cho giống ảnh tham khảo.
5. **Hệ thống 2 vũ khí (primary/secondary) kiểu COD**: súng đang cầm luôn là primary (khung lớn);
   khẩu súng KHÁC gần nhất từng cầm (còn trong hotbar) hiện thành 1 ô nhỏ, mờ hơn, nép ở góc khung
   icon chính — y hệt HUD 2 súng của Call of Duty. Cầm lại khẩu đó thì nó lại thành primary. Tự động
   ẩn nếu súng đó không còn trong hotbar nữa (không để lại "bóng ma").

## v2.5.1 — Weapon HUD fixed frame + fix icon distortion + fade box direction
Fix theo feedback thực tế từ ảnh chụp màn hình (A545 + FGM-148 Javelin) và tham khảo trực tiếp
source code của TaczPresence (github.com/AlfredoAlessandro/Tacz-Presence).
1. **Fix icon bị "biến dạng"**: nguyên nhân gốc là `TaczCompat.renderHudTexture` cũ đọc thẳng kích
   thước pixel thật của file PNG icon (qua `NativeImage`) rồi tính scale theo đó — nhưng TACZ KHÔNG
   hoạt động vậy. Đối chiếu với chính `ImmersiveGunHudOverlay` của TaczPresence (và cả HUD gốc của
   TACZ) thì icon HUD của TACZ luôn được vẽ từ một canvas cố định **39×13**, bất kể súng nào (súng
   dài như AK hay súng to như Javelin đều dùng chung 1 canvas 39×13 này, phần asset tự canh trong
   đó). Đọc kích thước file thật rồi tự tính UV là sai hoàn toàn, gây méo/lệch UV nặng — nhất là với
   Javelin vì file thật của nó lệch xa tỉ lệ 39:13. Đã bỏ hẳn cơ chế đọc `NativeImage`, giờ luôn blit
   cố định 39×13 nhân với 1 hệ số scale duy nhất (`iconScale`), y hệt cách TaczPresence làm.
2. **HUD giờ có khung cố định (fixed frame), không đổi theo tên súng hay art**: trước đây bề rộng
   panel được tính động theo độ dài tên súng + kích thước icon, nên súng có tên dài
   ("FGM-148 Javelin Anti-Tank Missile") làm cả khung phình to, đè lên crosshair, và tràn ra ngoài
   màn hình (đúng như ảnh chụp — chữ bị cắt "FGM-148 Ja"). Giờ khung dùng kích thước CỐ ĐỊNH
   (`frameWidth` / `frameHeight` trong config), y hệt hướng tiếp cận của TaczPresence: icon + số đạn
   luôn neo ở toạ độ cố định bên trong khung, chữ chỉ được đo để canh phải (right-align), không bao
   giờ dùng để phình khung.
3. **Tên súng tách thành toast riêng, tự ẩn sau 1-2 giây**: tên súng không còn nằm trong khung cố
   định nữa (đó chính là thứ từng làm khung biến dạng) — giờ nó hiện lên như một nhãn nổi riêng phía
   trên khung mỗi khi đổi súng, giữ nguyên ~1.4s rồi mờ dần trong ~0.5s (chỉnh được qua
   `nameHoldMs`/`nameFadeMs`), sau đó biến mất hẳn — khung ammo bên dưới không bao giờ bị ảnh hưởng
   bởi độ dài tên.
4. **Fix fade box bị lộn ngược**: gradient nền (`HudPanel`) trước đó đậm phía trên/nhạt phía dưới —
   ngược với cảm giác "neo" tự nhiên khi HUD nằm sát mép màn hình. Đã đổi lại: đậm ở cạnh neo gần mép
   màn hình, nhạt dần khi đi lên phía trong khung hình — dùng chung cho cả HUD người chơi lẫn HUD
   súng vì cả 2 dùng chung `HudPanel`.

## v2.5.0 — Config sync fixes, custom absorption colour, modern panel, TACZ weapon widget
1. **Config đồng bộ / fix lỗi**: đối chiếu từng field trong `HudConfig` với renderer thực tế và
   phát hiện các config đã khai báo + load nhưng KHÔNG được dùng ở đâu cả: `namePosition`
   (ABOVE/BELOW bị bỏ qua, tên luôn vẽ ở trên), `hpTextPosition` (luôn hardcode END_OF_BAR),
   `showOverflowText` (không có chỗ nào vẽ "+N"), và cả nhóm Armor Bar
   (`showArmorBar/showArmorText/armorTextFormat/armorTextPosition/armorTextColor` — tồn tại từ
   version cũ nhưng chưa từng được wire vào renderer). Đã nối lại toàn bộ các config này vào
   `PlayerHudOverlay`, bao gồm thêm một dòng Armor thật (vanilla armor points, tách biệt với
   Absorption/shield). Đồng thời bổ sung 34 translation key còn thiếu trong `en_us.json` (trước đó
   sẽ hiện ra dạng `modernhud.config.xxx` thô trong màn hình config).
2. **Absorption màu custom**: thêm `absorptionColorMode` (TIERED/CUSTOM) +
   `absorptionCustomColor`, mặc định ocean blue `0x1C7ED6` khi chọn CUSTOM — khoá cả thanh về một
   màu duy nhất thay vì đổi theo tier như trước.
3. **Fix lệch head/name/bar**: đầu người chơi trước đó được căn giữa theo TOÀN BỘ khối (kể cả dòng
   tên), nên nhìn bị lệch so với các thanh bar. Giờ đầu được căn giữa riêng theo khối bar
   (plates+health+armor), độc lập với tên ở ABOVE hay BELOW.
4. **Panel "fade box + line góc"**: `HudPanel` mới — một lớp nền gradient mờ dần (translucent fade)
   + 2 đường viền góc màu accent (corner brackets), dùng chung cho cả HUD người chơi lẫn HUD súng,
   cố tình chỉ 1 lớp phẳng — không có drop-shadow / box đôi phía sau.
5. **Tích hợp TACZ weapon HUD từ CustomHUD**: decompile `TaczCompat`/`AmmoHud` gốc (CFR) để lấy đúng
   logic mag/reserve/fire-mode, port lại vào package riêng của ModernHUD (Mojang mapping, không SRG)
   thành `TaczCompat` + `WeaponConfig` + `WeaponHudOverlay`. Hiển thị icon HUD gốc của TACZ, tên súng,
   fire mode, và đạn mag/reserve dạng SỐ THUẦN — **bỏ hẳn thanh gauge ammo bar**, không shadow box,
   dùng chung panel "modern" ở trên. Soft-dep an toàn: không TACZ thì widget không làm gì cả.

## v2.1.0 — Render-engine HUD + MW compass gunfire (làm lại từ đầu, cẩn thận)
Bản này viết lại từ source gốc v2.0.0, mỗi file whole-rewrite + verify balance, không chồng patch.

### Item 1 — Render-engine migration
Thanh bar không còn PNG texture: vẽ hoàn toàn bằng GuiGraphics fill/gradient (BarRender). Thanh máu
MW2022: mỏng, trắng, sheen nhẹ, smooth. Sắc nét mọi scale.

### Item 2 — HUD player rework
- Đầu người chơi sang CẠNH TRÁI khối bar, căn giữa chiều cao.
- Tên ABOVE/BELOW qua config `namePosition`.
- Absorption MỎNG (config `absorptionHeight`, mặc định 3px), đặt TRÊN thanh giáp, 3 tier màu
  trắng→xanh→tím/đỏ như MW.

### Item 3 — Compass gunfire detection
- Nháy đỏ theo HƯỚNG kẻ địch bắn (chiếu bearing lên bar, cùng công thức tick).
- Detect qua TACZ GunShootEvent client-side (soft-dep, chỉ register khi TACZ loaded).
- shoot_3p 256 block (to/đậm khi gần), silence_3p 32 block (mờ tới khi sát), mũi tên cao/thấp.
- **Whitelist team vanilla**: KHÔNG nháy với chính mình + người cùng team; nháy với khác team +
  mob/player không team.
- Config nhóm "Gunfire Blips": bật/tắt, tầm, màu, kích thước.

TACZ là optional — không có TACZ thì compass vẫn chạy, chỉ mất blip.

## v2.2.0 — Bar smooth & modern kiểu Damage Engine + rework layout + compass off-arc
**Bar (học Damage Engine, hoàn toàn textureless):**
- **Pill bo góc** vẽ bằng stacked fill (radius = height/2) — smooth, modern, không texture.
- **Smooth lerp**: thanh máu TRƯỢT mượt về giá trị thật (mất máu nhanh hơn, hồi máu dịu hơn) thay
  vì nhảy giật — đúng cảm giác Damage Engine.
- Sheen nhẹ nửa trên. Absorption strip 3-tier vẫn dùng pill.
**Layout HUD mới (theo yêu cầu):**
- Đầu player bên trái. Bên phải từ trên xuống: THANH MÁU (pill smooth) + SỐ máu ở cuối bar →
  GIÁP hiển thị bằng ICON khiên vẽ tay + SỐ (KHÔNG còn thanh giáp) → TÊN player dưới cùng.
- Absorption (khi có) là dải mỏng trên cùng.
**Compass off-arc indicator:**
- Tiếng súng NGOÀI cung compass giờ hiện `>>` (bên phải) hoặc `<<` (bên trái) ở mép chỉ hướng,
  fade theo cường độ; xoay vào tầm nhìn thì hiện chấm nháy như thường.

Chỉ còn sound + icon break dùng texture; toàn bộ bar đã render-engine.

## v2.3.0 — HUD MW2022 seamless + ping kim cương Ping-Wheel + chevron rõ
**HUD (theo ảnh ref MW2022/DMZ/Warzone, sửa ảnh 2 xấu):**
- Tên player TRÊN ĐẦU. Đầu bên trái, căn giữa khối.
- Thanh máu HÌNH CHỮ NHẬT mỏng (nửa chiều cao cũ), SEAMLESS — không viền nặng, chỉ hairline 1px
  + sheen nhẹ. Trượt mượt (Damage-Engine glide). Số HP ở cuối bar.
- Armor = PLATE BAR chia ô có KHOẢNG CÁCH (gap) giữa các plate, tô tier trắng→xanh→tím→đỏ. Bỏ
  hẳn armor icon + số giáp.
- Layout ỔN ĐỊNH: hàng plate luôn reserve chỗ nên khi có/mất shield thanh máu KHÔNG nhảy lệch.
**Ping / compass (theo ảnh ref MW + Ping Wheel):**
- Blip giờ là KIM CƯƠNG 2 LỚP (outline ring + filled center) đúng kỹ thuật Ping Wheel: vẽ ô vuông
  rồi xoay 45° bằng matrix, scale mượt (không còn ô vuông 30fps).
- Animation: mỗi phát bắn BOUNCE to (size theo khoảng cách — gần thì to hơn) rồi FADE + NHỎ lại.
- Off-arc: chevron `<<` / `>>` TO ĐẬM vẽ bằng fill (không còn chữ nhỏ khó thấy) chỉ hướng ở mép.

## v2.4.0 — Compass polish + HUD editor kéo thả + config đầy đủ + ổn định
**Compass / shoot indicator:**
- Marker nhỏ lại (~65% mặc định) + CONFIG được: size (%), thời gian fade (ms), độ cao trên bar.
- Marker giờ nằm CAO HƠN thanh tọa độ (vẽ ngoài scissor nên không bị cắt).
- Chevron off-arc `<<`/`>>` giờ ANIMATED (bounce-in + pulse + fade) crisp như gunfire ping, vẽ
  bằng fill (không texture), đẩy RA NGOÀI rìa thanh tọa độ.
**Absorption tier:** đúng thang XÁM → XANH DƯƠNG → TÍM → ĐỎ theo số plate đầy (1 plate = xám,
2 = xanh, 3 = tím, 4+ = đỏ). Sửa lỗi 2 vạch đã tím.
**HUD editor kéo thả (QOL kiểu COD):** keybind mở "HUD Layout Editor" — kéo Player HUD và icon
Hit/Break (gộp chung) đặt vị trí bằng CHUỘT, có lưới căn, lưu khi đóng. Hết cảnh bấm số offset.
**Config:** mỗi feature có nhóm config đồng bộ, đầy đủ; thêm setter runtime persist cho editor.
**Ổn định:** rà client/server — class server không import class client-only; gunfire listener chỉ
đăng ký client + khi TACZ có mặt; balance toàn bộ sạch.

(YACL config + live preview kiểu HaloHUD: chờ bạn gửi HaloHUD source để làm cho chuẩn.)
