# Báº£n hiá»‡n táº¡i: V008

Xem [bÃ¡o cÃ¡o bÃ n giao vÃ  hÆ°á»›ng dáº«n migration/khÃ´i phá»¥c](V008_HANDOVER.md) vÃ  [mÃ´ hÃ¬nh kho hiá»‡n táº¡i](backend_java/INVENTORY_COST.md). V008 Ä‘Ã£ thay viá»‡c Ä‘áº·t Ä‘Æ¡n trá»« kho báº±ng giá»¯ hÃ ng riÃªng, ADMIN duyá»‡t xuáº¥t giao vÃ  giÃ¡ vá»‘n theo SKU. CÃ¡c ghi chÃ©p nghiá»‡m thu V001â€“V007 bÃªn dÆ°á»›i Ä‘Æ°á»£c giá»¯ lÃ m lá»‹ch sá»­, khÃ´ng dÃ¹ng pháº§n mÃ´ hÃ¬nh kho cÅ© Ä‘á»ƒ váº­n hÃ nh V008.

---

# Quáº£n trá»‹ Clothing Shop

## Cáº¥u hÃ¬nh Gmail SMTP báº±ng lá»‡nh

Trong PowerShell táº¡i thÆ° má»¥c dá»± Ã¡n, cháº¡y ` .\shop.cmd -Mode ConfigureSmtp `.
Lá»‡nh há»i Ä‘á»‹a chá»‰ Gmail vÃ  **máº­t kháº©u á»©ng dá»¥ng Google 16 kÃ½ tá»±**, nháº­p á»Ÿ dáº¡ng áº©n; khÃ´ng dÃ¹ng máº­t kháº©u Ä‘Äƒng nháº­p Gmail, khÃ´ng gá»­i máº­t kháº©u qua chat. Táº¡o táº¡i https://myaccount.google.com/apppasswords sau khi báº­t xÃ¡c minh hai bÆ°á»›c. Cáº¥u hÃ¬nh dÃ¹ng `smtp.gmail.com:587`, STARTTLS báº¯t buá»™c vÃ  kiá»ƒm tra danh tÃ­nh chá»©ng chá»‰ mÃ¡y chá»§.

Lá»‡nh kiá»ƒm tra Ä‘Äƒng nháº­p SMTP **khÃ´ng gá»­i email**, chá»‰ lÆ°u khi xÃ¡c thá»±c thÃ nh cÃ´ng. Cáº¥u hÃ¬nh lÆ°u trong `.vscode/oracle-local.env` ngoÃ i Git, giá»¯ nguyÃªn cÃ¡c biáº¿n Oracle vÃ  Ä‘áº·t quyá»n tá»‡p cho ngÆ°á»i dÃ¹ng hiá»‡n táº¡i/SYSTEM. Náº¿u kiá»ƒm tra tháº¥t báº¡i thÃ¬ khÃ´ng ghi SMTP má»›i. Sau khi thÃ nh cÃ´ng, lá»‡nh tá»± khá»Ÿi Ä‘á»™ng láº¡i backend JAR Ä‘ang cháº¡y Ä‘á»ƒ náº¡p cáº¥u hÃ¬nh; náº¿u backend cháº¡y tá»« IDE thÃ¬ tá»± restart trong IDE. Cuá»‘i cÃ¹ng má»Ÿ `/forgot-password` vÃ  gá»­i yÃªu cáº§u Ä‘áº¿n tÃ i khoáº£n quáº£n trá»‹ cá»§a báº¡n Ä‘á»ƒ kiá»ƒm tra thÆ° Ä‘áº¿n.

CÃ³ thá»ƒ kiá»ƒm tra láº¡i káº¿t ná»‘i báº±ng `.\shop.cmd -Mode TestSmtp`. MÃ¡y hiá»‡n chÆ°a Ä‘Æ°á»£c cung cáº¥p máº­t kháº©u á»©ng dá»¥ng nÃªn SMTP tháº­t chá»‰ báº­t sau khi ngÆ°á»i dÃ¹ng hoÃ n thÃ nh lá»‡nh cáº¥u hÃ¬nh. ÄÃ£ kiá»ƒm tra cá»•ng Gmail 587 cÃ³ thá»ƒ káº¿t ná»‘i, kiá»ƒm thá»­ xÃ¡c thá»±c thÃ nh cÃ´ng/tháº¥t báº¡i báº±ng mock vÃ  kiá»ƒm thá»­ ghi cáº¥u hÃ¬nh/ACL báº±ng tá»‡p giáº£; chÆ°a xÃ¡c thá»±c tÃ i khoáº£n Gmail tháº­t.

## Thá»‘ng kÃª, phiáº¿u kho vÃ  khÃ´i phá»¥c máº­t kháº©u (báº£n cáº­p nháº­t 08/11/2026)

- **Thá»‘ng kÃª doanh thu:** chá»n tá»«ng ngÃ y/thÃ¡ng/quÃ½/nÄƒm, lá»c nÄƒm, thÃ¡ng, quÃ½ hoáº·c khoáº£ng ngÃ y; lá»c tÃªn/mÃ£ sáº£n pháº©m. CÃ³ báº£ng Ä‘á»§ cÃ¡c ká»³ ká»ƒ cáº£ ká»³ báº±ng 0, biá»ƒu Ä‘á»“, CSV vÃ  chi tiáº¿t doanh thu theo sáº£n pháº©m/mÃ u/size. NgÃ y hoÃ n thÃ nh Ä‘Æ¡n lÃ  ngÃ y ghi doanh thu; ngÃ y xÃ¡c nháº­n phiáº¿u hoÃ n lÃ  ngÃ y ghi giÃ¡ trá»‹ hÃ ng hoÃ n. Doanh thu rÃ²ng = tiá»n hÃ ng âˆ’ giáº£m giÃ¡ âˆ’ hÃ ng hoÃ n, khÃ´ng gá»“m phÃ­ giao. Giáº£m giÃ¡ Ä‘Æ°á»£c phÃ¢n bá»• vá» dÃ²ng hÃ ng, xá»­ lÃ½ pháº§n lÃ m trÃ²n Ä‘á»ƒ hoÃ n Ä‘á»§ khÃ´ng dÆ°/thiáº¿u. ÄÃ¢y lÃ  giÃ¡ trá»‹ doanh thu/hÃ ng hoÃ n, khÃ´ng tá»± chuyá»ƒn tiá»n hoÃ n qua ngÃ¢n hÃ ng.
- **Phiáº¿u nháº­p / xuáº¥t / hoÃ n:** STAFF hoáº·c ADMIN láº­p/sá»­a/há»§y phiáº¿u nhÃ¡p; chá»‰ ADMIN xÃ¡c nháº­n ghi tá»“n hoáº·c láº­p phiáº¿u Ä‘áº£o. Phiáº¿u Ä‘Ã£ xÃ¡c nháº­n khÃ´ng sá»­a/xÃ³a, duyá»‡t láº·p bá»‹ tá»« chá»‘i. CÃ³ lÃ½ do, ngÆ°á»i láº­p/duyá»‡t, thá»i gian, tá»“n trÆ°á»›c/sau tá»«ng biáº¿n thá»ƒ. Sá»­a phiáº¿u nhÃ¡p cá»§a ngÆ°á»i khÃ¡c chá»‰ dÃ nh cho ADMIN.
- **Tá»“n má»›i báº±ng 0:** khÃ´ng cho nháº­p tá»“n ngay trong form sáº£n pháº©m hoáº·c gá»i API Ä‘iá»u chá»‰nh trá»±c tiáº¿p. Nháº­p hÃ ng báº±ng phiáº¿u RECEIPT; xuáº¥t khÃ¡c báº±ng ISSUE, khÃ´ng xuáº¥t vÆ°á»£t tá»“n. COUNT kiá»ƒm kÃª sá»‘ sáºµn bÃ¡n thá»±c táº¿ (loáº¡i trá»« hÃ ng Ä‘Ã£ giá»¯ cho Ä‘Æ¡n); náº¿u tá»“n Ä‘Ã£ Ä‘á»•i tá»« lÃºc láº­p phiáº¿u thÃ¬ pháº£i sá»­a/Ä‘áº¿m láº¡i trÆ°á»›c khi duyá»‡t. REVERSAL Ä‘áº£o Ä‘Ãºng biáº¿n Ä‘á»™ng cá»§a phiáº¿u RECEIPT/ISSUE/COUNT; khÃ´ng Ä‘Æ°á»£c Ä‘áº£o láº·p.
- **BÃ¡n / há»§y:** há»‡ thá»‘ng tá»± táº¡o chá»©ng tá»« SALE khi giá»¯/trá»« hÃ ng cho Ä‘Æ¡n; CANCEL khi há»§y trÆ°á»›c giao/thanh toÃ¡n tháº¥t báº¡i vÃ  hoÃ n giá»¯ tá»“n Ä‘Ãºng má»™t láº§n. Trang tá»“n tÃ¡ch tá»“n sáºµn bÃ¡n, sá»‘ Ä‘ang giá»¯ cho Ä‘Æ¡n chÆ°a giao vÃ  sá»‘ cÃ²n táº¡i kho. BÃ¡o cÃ¡o nháº­p xuáº¥t tá»“n ghi rÃµ Ä‘áº§u ká»³, nháº­p, xuáº¥t khÃ¡c, xuáº¥t/giá»¯ bÃ¡n, há»§y Ä‘Æ¡n, hoÃ n nháº­p, kiá»ƒm kÃª/Ä‘áº£o vÃ  cuá»‘i ká»³. Sá»‘ dÆ° cÅ© giá»¯ nguyÃªn; khÃ´ng bá»‹a chá»©ng tá»« quÃ¡ khá»©.
- **Nháº­n hÃ ng hoÃ n:** má»Ÿ Ä‘Æ¡n hoÃ n thÃ nh hoáº·c Ä‘Æ¡n Ä‘ang yÃªu cáº§u tráº£ â†’ Láº­p phiáº¿u nháº­n hÃ ng hoÃ n. Chá»n Ä‘Ãºng biáº¿n thá»ƒ, sá»‘ lÆ°á»£ng vÃ  tÃ¬nh tráº¡ng; bá» â€œNháº­p láº¡i hÃ ng bÃ¡nâ€ vá»›i hÃ ng lá»—i. ADMIN duyá»‡t má»›i ghi nháº­n hÃ ng hoÃ n. KhÃ´ng cho nháº­n vÆ°á»£t sá»‘ Ä‘Ã£ bÃ¡n chÆ°a hoÃ n; há»— trá»£ hoÃ n má»™t pháº§n/toÃ n bá»™ vÃ  cháº·n hai phiáº¿u Ä‘á»“ng thá»i hoÃ n vÆ°á»£t. HÃ ng lá»—i váº«n tÃ­nh giÃ¡ trá»‹ hÃ ng hoÃ n nhÆ°ng khÃ´ng tÄƒng tá»“n bÃ¡n. KhÃ´ng cho Ä‘áº£o phiáº¿u nháº­n hoÃ n báº±ng chá»©c nÄƒng Ä‘áº£o phiáº¿u thá»§ cÃ´ng; pháº£i kiá»ƒm tra ká»¹ trÆ°á»›c khi duyá»‡t.
- **QuÃªn máº­t kháº©u quáº£n trá»‹:** `/forgot-password`; liÃªn káº¿t dÃ¹ng má»™t láº§n, háº¿t háº¡n 15 phÃºt, lÆ°u SHA-256 trong Oracle, khÃ´ng tráº£ token qua API/log. Äá»•i máº­t kháº©u thu há»“i cÃ¡c phiÃªn cÅ© vÃ  khÃ´ng tá»± má»Ÿ khÃ³a tÃ i khoáº£n. NÃºt Hiá»‡n/áº¨n cÃ³ trÃªn Ä‘Äƒng nháº­p, Ä‘áº·t láº¡i máº­t kháº©u vÃ  cÃ¡c Ã´ máº­t kháº©u quáº£n trá»‹ má»›i.

MÃ¡y hiá»‡n táº¡i dÃ¹ng **thÆ° khÃ´i phá»¥c cá»¥c bá»™**: sau khi gá»­i yÃªu cáº§u, má»Ÿ tá»‡p `reset-*.txt` má»›i nháº¥t trong `%USERPROFILE%\clothing-shop-private\password-reset`, rá»“i má»Ÿ liÃªn káº¿t trong tá»‡p. ThÆ° má»¥c á»Ÿ ngoÃ i Git, tá»‡p chá»‰ cáº¥p quyá»n cho chá»§ sá»Ÿ há»¯u. LiÃªn káº¿t chá»©a token trong fragment Ä‘á»ƒ trÃ¡nh Ä‘Æ°a token vÃ o access log. KhÃ´ng gá»­i liÃªn káº¿t/máº­t kháº©u qua chat. ChÆ°a cáº¥u hÃ¬nh SMTP nÃªn chÆ°a gá»­i email tháº­t.

Muá»‘n gá»­i email tháº­t, Ä‘áº·t cÃ¡c biáº¿n dÆ°á»›i Ä‘Ã¢y trong mÃ´i trÆ°á»ng cá»¥c bá»™ hoáº·c `.vscode/oracle-local.env` (tá»‡p Ä‘Ã£ bá»‹ bá» qua bá»Ÿi Git), rá»“i khá»Ÿi Ä‘á»™ng láº¡i. KhÃ´ng ghi máº­t kháº©u SMTP vÃ o mÃ£ nguá»“n:

```text
SHOP_RESET_DELIVERY=smtp
SPRING_MAIL_HOST=<mÃ¡y chá»§ SMTP>
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=<tÃ i khoáº£n gá»­i>
SPRING_MAIL_PASSWORD=<máº­t kháº©u á»©ng dá»¥ng SMTP>
SHOP_RESET_FROM=<Ä‘á»‹a chá»‰ gá»­i>
SHOP_SMTP_AUTH=true
SHOP_SMTP_STARTTLS=true
SHOP_SMTP_SSL=false
SHOP_RESET_BASE_URL=http://localhost:8080
```

ÄÃ£ kiá»ƒm thá»­ Oracle tháº­t: phiáº¿u nhÃ¡p khÃ´ng Ä‘á»•i tá»“n; STAFF khÃ´ng Ä‘Æ°á»£c duyá»‡t; duyá»‡t láº·p/xuáº¥t vÆ°á»£t tá»“n bá»‹ tá»« chá»‘i; sá»­a/há»§y/Ä‘áº£o phiáº¿u; kiá»ƒm kÃª phÃ¡t hiá»‡n sá»‘ tá»“n Ä‘Ã£ Ä‘á»•i; hoÃ n má»™t pháº§n, hÃ ng lá»—i khÃ´ng nháº­p bÃ¡n, hai phiáº¿u hoÃ n Ä‘á»“ng thá»i; bÃ¡o cÃ¡o ngÃ y/thÃ¡ng/quÃ½/nÄƒm vÃ  cÃ¢n Ä‘á»‘i tá»“n; token háº¿t háº¡n/dÃ¹ng láº¡i/tÃ i khoáº£n khÃ³a bá»‹ tá»« chá»‘i, Ä‘á»•i máº­t kháº©u thu há»“i phiÃªn. ChÃ­n kiá»ƒm thá»­ DOM kiá»ƒm tra cÃ¡c form, bá»™ lá»c bÃ¡o cÃ¡o, duyá»‡t phiáº¿u cÃ³ bÆ°á»›c xÃ¡c nháº­n vÃ  nÃºt hiá»‡n máº­t kháº©u. SMTP tháº­t chÆ°a kiá»ƒm thá»­ do chÆ°a cÃ³ cáº¥u hÃ¬nh; gá»­i thÆ° cá»¥c bá»™ Ä‘Ã£ kiá»ƒm thá»­. Kiá»ƒm tra hiá»ƒn thá»‹ báº±ng Chrome tháº­t váº«n chÆ°a thá»±c hiá»‡n vÃ¬ quyá»n Ä‘iá»u khiá»ƒn Chrome trÆ°á»›c Ä‘Ã³ bá»‹ tá»« chá»‘i.

MÃ£ sáº£n pháº©m má»›i tá»± tÄƒng theo sequence Oracle, dáº¡ng SP004, SP005â€¦; mÃ£ tháº­t Ä‘Æ°á»£c cáº¥p khi lÆ°u. SKU biáº¿n thá»ƒ má»›i tá»± sinh tá»« mÃ£ sáº£n pháº©m + tÃªn mÃ u khÃ´ng dáº¥u + size, vÃ­ dá»¥ SP004-DEN-M. Hai Ã´ nÃ y chá»‰ Ä‘á»c; SKU xem trÆ°á»›c Ä‘á»•i khi chá»n mÃ u/nháº­p size. Backend bá» qua mÃ£/SKU do trÃ¬nh duyá»‡t gá»­i vÃ  giá»¯ mÃ£/SKU cÅ© khi sá»­a Ä‘á»ƒ khÃ´ng thay Ä‘á»•i lá»‹ch sá»­. Khi cÃ³ tÃªn mÃ u/size trÃ¹ng dáº¡ng chuáº©n hÃ³a, backend thÃªm háº­u tá»‘ sá»‘ Ä‘á»ƒ SKU duy nháº¥t. Sequence cÃ³ thá»ƒ nháº£y sá»‘ sau rollback/kiá»ƒm thá»­, khÃ´ng tÃ¡i sá»­ dá»¥ng sá»‘ Ä‘Ã£ cáº¥p.

Form sáº£n pháº©m hiá»‡n cho nháº­p thÆ°Æ¡ng hiá»‡u/cháº¥t liá»‡u trá»±c tiáº¿p (cÃ³ gá»£i Ã½ dá»¯ liá»‡u cÅ©), khÃ´ng chá»n sáºµn khi thÃªm má»›i. TÃªn má»›i Ä‘Æ°á»£c táº¡o cÃ¹ng giao dá»‹ch lÆ°u sáº£n pháº©m; tÃªn Ä‘Ã£ cÃ³ Ä‘Æ°á»£c dÃ¹ng láº¡i. CÃ³ thá»ƒ chá»n nhiá»u áº£nh, xem trÆ°á»›c, gÃ¡n mÃ u/thá»© tá»± vÃ  chá»n áº£nh Ä‘áº¡i diá»‡n ngay trong form thÃªm/sá»­a. Khi lÆ°u kÃ¨m áº£nh, backend lÆ°u toÃ n bá»™ sáº£n pháº©m, biáº¿n thá»ƒ, áº£nh vÃ  thÆ°Æ¡ng hiá»‡u/cháº¥t liá»‡u má»›i trong cÃ¹ng transaction; áº£nh lá»—i sáº½ rollback dá»¯ liá»‡u vÃ  dá»n file vá»«a táº¡o. KhÃ´ng cáº§n lÆ°u nhÃ¡p rá»“i má»Ÿ há»™p áº£nh riÃªng.

## Cháº¡y trÃªn mÃ¡y hiá»‡n táº¡i

Tá»« thÆ° má»¥c `Project-Java`, dÃ¹ng terminal tÆ°Æ¡ng tÃ¡c:

TrÆ°á»›c khi build láº¡i, dá»«ng backend Ä‘ang cháº¡y Ä‘á»ƒ Windows khÃ´ng khÃ³a tá»‡p JAR. Báº£n hiá»‡n táº¡i Ä‘Ã£ build vÃ  Ä‘ang cháº¡y; cÃ³ thá»ƒ bá» qua Build/Run vÃ  chá»‰ cháº¡y CreateAdmin á»Ÿ terminal thá»© hai.

```bat
./shop.cmd -Mode Build
./shop.cmd -Mode CreateAdmin
./shop.cmd -Mode Run
```

`CreateAdmin` há»i username, email vÃ  máº­t kháº©u 12â€“72 kÃ½ tá»± trá»±c tiáº¿p trong terminal; máº­t kháº©u khÃ´ng hiá»‡n trÃªn mÃ n hÃ¬nh, khÃ´ng truyá»n qua command line vÃ  khÃ´ng ghi vÃ o Git. Chá»‰ khá»Ÿi táº¡o khi chÆ°a tá»“n táº¡i tÃ i khoáº£n vai trÃ² ADMIN, ká»ƒ cáº£ admin Ä‘Ã£ khÃ³a. Náº¿u backend Ä‘ang cháº¡y, váº«n cÃ³ thá»ƒ cháº¡y lá»‡nh khá»Ÿi táº¡o á»Ÿ terminal thá»© hai vÃ¬ lá»‡nh nÃ y khÃ´ng má»Ÿ cá»•ng HTTP. KhÃ´ng cÃ³ máº­t kháº©u máº·c Ä‘á»‹nh.

Script đọc thông tin Oracle từ `.vscode/oracle-local.env`, chỉ nạp các biến datasource/upload được cho phép. Không sửa tệp này. Ứng dụng yêu cầu JDK 25 LTS; đặt `JAVA_HOME` trỏ tới JDK 25 trước khi chạy/build. Ưu tiên `mvn` trong PATH; nếu chưa có thì dùng Maven 3.9.9 đã tải tại `.local/apache-maven-3.9.9`. Thư mục `.local` không vào Git. Máy khác cần cài JDK 25 và Maven trong PATH hoặc đặt `JAVA_HOME`.

CÃ¡c Ä‘á»‹a chá»‰, cÃ¹ng má»™t backend/cá»•ng nÃªn khÃ´ng cáº§n cháº¡y server frontend riÃªng:

- ÄÄƒng nháº­p quáº£n trá»‹: http://localhost:8080/login
- Quáº£n trá»‹: http://localhost:8080/admin
- Giao diá»‡n bÃ¡n hÃ ng gá»‘c: http://localhost:8080/store/index.html
- Danh sÃ¡ch sáº£n pháº©m tá»« Oracle: http://localhost:8080/shop/index.html
- TÃ i khoáº£n khÃ¡ch hÃ ng: http://localhost:8080/shop/index.html?view=account
- Giá» hÃ ng tháº­t: http://localhost:8080/store/cart.html?live=1
- Thanh toÃ¡n tháº­t: http://localhost:8080/store/checkout.html?live=1

Giao diá»‡n máº«u gá»‘c váº«n giá»¯ bá»‘ cá»¥c/ná»™i dung. NÃºt **Mua hÃ ng Â· Sáº£n pháº©m thá»±c táº¿** má»Ÿ luá»“ng láº¥y dá»¯ liá»‡u Oracle. Chi tiáº¿t sáº£n pháº©m cÃ³ `?live=1&id=...` vÃ  cÃ¡c trang giá» hÃ ng/thanh toÃ¡n tháº­t dÃ¹ng pháº§n ná»™i dung Ä‘á»™ng, giá»¯ header/footer cá»§a máº«u. PhiÃªn khÃ¡ch hÃ ng riÃªng vá»›i phiÃªn quáº£n trá»‹; Ä‘Äƒng kÃ½ tÃ i khoáº£n khÃ¡ch Ä‘á»ƒ thá»­ mua hÃ ng. TÃ i khoáº£n `khach1` trong dá»¯ liá»‡u máº«u cÅ© dÃ¹ng hash giáº£ nÃªn khÃ´ng thá»ƒ dÃ¹ng Ä‘á»ƒ Ä‘Äƒng nháº­p tháº­t.

Náº¿u cháº¡y tá»« VS Code, tiáº¿p tá»¥c dÃ¹ng cáº¥u hÃ¬nh **Clothing Shop (Oracle)** Ä‘Ã£ cÃ³. Náº¿u cháº¡y JAR thá»§ cÃ´ng, Ä‘áº·t thÆ° má»¥c lÃ m viá»‡c lÃ  `backend_java` hoáº·c Ä‘áº·t `shop.storefront` Ä‘áº¿n thÆ° má»¥c `giao dien`.

## Migration vÃ  dá»¯ liá»‡u

ÄÃ£ Ã¡p dá»¥ng trÃªn Oracle `FREEPDB1`, schema `CLOTHING_SHOP` ngÃ y 07/11/2026:

| Migration | Ná»™i dung | Giá» Viá»‡t Nam |
|---|---|---|
| V001__admin.sql | CATEGORY, PRODUCT_IMAGE, VOUCHER, INVENTORY_HISTORY, ADMIN_AUDIT, SHOP_MIGRATION; liÃªn káº¿t danh má»¥c; thÃ´ng tin váº­n chuyá»ƒn/voucher/giá»¯ tá»“n; UNIQUE mÃ uâ€“size | 22:30 |
| V002__session_revocation.sql | AUTH_VERSION cho MANAGER vÃ  USERS Ä‘á»ƒ thu há»“i phiÃªn | 22:45 |
| V003__automatic_product_codes.sql | Sequence mÃ£ sáº£n pháº©m, báº¯t Ä‘áº§u sau mÃ£ SP lá»›n nháº¥t; khÃ´ng sá»­a mÃ£/SKU cÅ© | 08/11/2026 13:18 |
| V004__inventory_reports_recovery.sql | Phiáº¿u kho/dÃ²ng phiáº¿u, liÃªn káº¿t lá»‹ch sá»­, ngÃ y hoÃ n thÃ nh vÃ  hÃ ng hoÃ n, token khÃ´i phá»¥c máº­t kháº©u | 08/11/2026 |

Xem tráº¡ng thÃ¡i báº±ng `SELECT VERSION, APPLIED_AT FROM SHOP_MIGRATION ORDER BY VERSION`.

KhÃ´ng cháº¡y láº¡i `schema.sql`, `sample_data.sql` hay migration Ä‘Ã£ Ã¡p dá»¥ng. Migration Oracle cÃ³ DDL tá»± commit; pháº£i sao lÆ°u vÃ  Ã¡p dá»¥ng má»™t láº§n theo thá»© tá»±. Náº¿u triá»ƒn khai sang database khÃ¡c, Ä‘á»‘i chiáº¿u cáº¥u trÃºc vÃ  kiá»ƒm tra trÃ¹ng biáº¿n thá»ƒ trÆ°á»›c:

```sql
SELECT PRODUCT_ID, COLOR_ID, SIZE_NAME, COUNT(*)
FROM PRODUCT_DETAIL
GROUP BY PRODUCT_ID, COLOR_ID, SIZE_NAME
HAVING COUNT(*) > 1;
```

Náº¿u cÃ³ trÃ¹ng, xá»­ lÃ½ dá»¯ liá»‡u theo nghiá»‡p vá»¥ trÆ°á»›c; migration sáº½ dá»«ng thay vÃ¬ xÃ³a dá»¯ liá»‡u. Náº¿u DDL gáº·p lá»—i giá»¯a chá»«ng, kiá»ƒm tra object Ä‘Ã£ táº¡o trÆ°á»›c khi tiáº¿p tá»¥c, khÃ´ng cháº¡y láº¡i toÃ n bá»™ mÃ¹ quÃ¡ng. Báº£n ghi SHOP_MIGRATION chá»‰ Ä‘Æ°á»£c thÃªm sau khi migration hoÃ n táº¥t.

Hai sáº£n pháº©m vÃ  ba biáº¿n thá»ƒ cÅ© Ä‘Æ°á»£c giá»¯ nguyÃªn; sau nghiá»‡m thu váº«n cÃ³ 0 BILL vÃ  0 MANAGER. Dá»¯ liá»‡u kiá»ƒm thá»­ cÃ³ tiá»n tá»‘ ngáº«u nhiÃªn vÃ  Ä‘Ã£ Ä‘Æ°á»£c dá»n, khÃ´ng xÃ³a dá»¯ liá»‡u shop. Sáº£n pháº©m cÅ© chÆ°a cÃ³ áº£nh váº«n Ä‘Æ°á»£c giá»¯ nguyÃªn; khi lÆ°u báº±ng form quáº£n trá»‹, muá»‘n báº­t bÃ¡n pháº£i chá»n áº£nh Ä‘áº¡i diá»‡n vÃ  cÃ³ biáº¿n thá»ƒ Ä‘ang bÃ¡n. Danh má»¥c cá»§a sáº£n pháº©m cÅ© cÃ³ thá»ƒ Ä‘á»ƒ trá»‘ng.

## Chá»©c nÄƒng

- ADMIN: quáº£n lÃ½ khÃ¡ch hÃ ng, nhÃ¢n viÃªn/vai trÃ², danh má»¥c, sáº£n pháº©m/biáº¿n thá»ƒ/áº£nh, voucher, Ä‘Æ¡n hÃ ng, tá»“n kho, bÃ¡o cÃ¡o vÃ  nháº­t kÃ½. STAFF: danh má»¥c, sáº£n pháº©m, voucher, Ä‘Æ¡n, tá»“n, bÃ¡o cÃ¡o; khÃ´ng Ä‘Æ°á»£c quáº£n lÃ½ tÃ i khoáº£n hoáº·c Ä‘á»c nháº­t kÃ½ quáº£n trá»‹.
- BCrypt cost 12; CSRF cho má»i thao tÃ¡c ghi; Ä‘Äƒng xuáº¥t báº±ng POST; phiÃªn háº¿t háº¡n sau 30 phÃºt khÃ´ng hoáº¡t Ä‘á»™ng; giá»›i háº¡n 5 láº§n Ä‘Äƒng nháº­p tháº¥t báº¡i trong 10 phÃºt theo IP. KhÃ³a/thay quyá»n tÄƒng phiÃªn báº£n quyá»n; phiÃªn cÅ© máº¥t hiá»‡u lá»±c cáº£ khi sau Ä‘Ã³ má»Ÿ khÃ³a. Kiá»ƒm tra vai trÃ²/tráº¡ng thÃ¡i á»Ÿ backend trÃªn má»—i request quáº£n trá»‹. KhÃ´ng tá»± khÃ³a/tá»± háº¡ quyá»n, khÃ´ng vÃ´ hiá»‡u hÃ³a admin hoáº¡t Ä‘á»™ng cuá»‘i cÃ¹ng.
- API ghi cÅ© Ä‘Æ°á»£c báº£o vá»‡; API hÃ³a Ä‘Æ¡n chá»©a thÃ´ng tin cÃ¡ nhÃ¢n chá»‰ dÃ nh cho quáº£n trá»‹. `/api/dev/**` luÃ´n bá»‹ tá»« chá»‘i, khÃ´ng thá»ƒ Ä‘á»c SQL tÃ¹y Ã½. API tÃ i khoáº£n dÃ¹ng danh sÃ¡ch cá»™t an toÃ n, khÃ´ng tráº£ hash. Lá»—i database khÃ´ng tráº£ SQL hoáº·c ná»™i dung Oracle chi tiáº¿t. Táº¯t log tham sá»‘ JDBC Ä‘á»ƒ trÃ¡nh ghi máº­t kháº©u/hash.
- KhÃ¡ch hÃ ng cÃ³ tÃ¬m kiáº¿m tÃªn/email/Ä‘iá»‡n thoáº¡i, chi tiáº¿t vÃ  lá»‹ch sá»­ Ä‘Æ¡n; khÃ³a/má»Ÿ khÃ³a báº±ng tráº¡ng thÃ¡i, giá»¯ liÃªn káº¿t cÅ©. Táº¡o nhÃ¢n viÃªn, Ä‘á»•i ADMIN/STAFF vÃ  khÃ³a/má»Ÿ khÃ³a.
- Danh má»¥c cha/con, áº©n/khÃ´i phá»¥c; cháº·n tá»± tham chiáº¿u vÃ  vÃ²ng láº·p. Form sáº£n pháº©m chá»n danh má»¥c, thÆ°Æ¡ng hiá»‡u, cháº¥t liá»‡u. TÃ¬m kiáº¿m, lá»c danh má»¥c/tráº¡ng thÃ¡i vÃ  phÃ¢n trang sáº£n pháº©m.
- LÆ°u sáº£n pháº©m vÃ  cÃ¡c biáº¿n thá»ƒ cÃ¹ng transaction. Báº¯t buá»™c Ã­t nháº¥t má»™t biáº¿n thá»ƒ; SKU chuáº©n hÃ³a chá»¯ hoa vÃ  duy nháº¥t; mÃ uâ€“size duy nháº¥t trong sáº£n pháº©m; khÃ´ng cho giÃ¡/tá»“n Ã¢m. Giá»¯ biáº¿n thá»ƒ cÅ©, ngá»«ng bÃ¡n thay vÃ¬ xÃ³a. KhÃ´ng Ä‘á»•i mÃ uâ€“size cá»§a biáº¿n thá»ƒ Ä‘Ã£ táº¡o Ä‘á»ƒ báº£o toÃ n lá»‹ch sá»­ Ä‘Æ¡n.
- Tá»“n chá»‰ thay Ä‘á»•i qua phiáº¿u kho Ä‘Æ°á»£c duyá»‡t hoáº·c chá»©ng tá»« bÃ¡n/há»§y tá»± Ä‘á»™ng, cÃ³ lÃ½ do vÃ  lá»‹ch sá»­. Sáº£n pháº©m má»›i tá»“n 0; khÃ´ng sá»­a tá»“n trá»±c tiáº¿p trong form sáº£n pháº©m. GiÃ¡, quyá»n, tÃ i khoáº£n, voucher, tráº¡ng thÃ¡i Ä‘Æ¡n vÃ  áº£nh cÃ³ nháº­t kÃ½ trong transaction.
- Upload nhiá»u áº£nh cÃ³ preview, thá»© tá»±, áº£nh Ä‘áº¡i diá»‡n, mÃ u vÃ  gá»¡ áº£nh. Kiá»ƒm tra chá»¯ kÃ½ vÃ  giáº£i mÃ£ JPEG/PNG/WebP; tá»‘i Ä‘a 5 MB/áº£nh, 40 megapixel; tÃªn UUID do server táº¡o. áº¢nh theo mÃ u dÃ¹ng chung giá»¯a cÃ¡c size; fallback áº£nh chung/Ä‘áº¡i diá»‡n.
- Voucher pháº§n trÄƒm/cá»‘ Ä‘á»‹nh, ngÃ y hiá»‡u lá»±c theo giá» Ä‘á»‹a phÆ°Æ¡ng cá»§a Oracle, Ä‘Æ¡n tá»‘i thiá»ƒu, tráº§n giáº£m pháº§n trÄƒm, giá»›i háº¡n lÆ°á»£t, báº­t/táº¯t. Oracle khÃ³a/cáº­p nháº­t cÃ³ Ä‘iá»u kiá»‡n, tÃ­nh giáº£m vÃ  ghi lÆ°á»£t cÃ¹ng transaction Ä‘áº·t Ä‘Æ¡n. LÆ°á»£t Ä‘Ã£ dÃ¹ng khÃ´ng Ä‘Æ°á»£c tráº£ láº¡i khi há»§y, trÃ¡nh tÃ¡i sá»­ dá»¥ng ngoÃ i giá»›i háº¡n.
- ÄÆ¡n cÃ³ tÃ¬m kiáº¿m, lá»c tráº¡ng thÃ¡i/ngÃ y, phÃ¢n trang; chi tiáº¿t khÃ¡ch, Ä‘á»‹a chá»‰ snapshot, dÃ²ng hÃ ng/mÃ u/size, giÃ¡ táº¡i lÃºc mua, voucher, giáº£m, phÃ­ giao vÃ  tá»•ng. Ghi nháº­n COD hoáº·c chuyá»ƒn khoáº£n thá»§ cÃ´ng, tráº¡ng thÃ¡i Ä‘Ã£ nháº­n tiá»n, Ä‘Æ¡n vá»‹ váº­n chuyá»ƒn/mÃ£ theo dÃµi. KhÃ´ng tÃ­ch há»£p cá»•ng thanh toÃ¡n bÃªn ngoÃ i.
- Tá»•ng quan: doanh thu rÃ²ng theo thÃ¡ng ghi nháº­n trong nÄƒm hiá»‡n táº¡i, Ä‘Æ¡n theo tráº¡ng thÃ¡i, bÃ¡n cháº¡y sau hÃ ng hoÃ n, cáº£nh bÃ¡o tá»“n. BÃ¡o cÃ¡o chi tiáº¿t cÃ³ ngÃ y/thÃ¡ng/quÃ½/nÄƒm. Báº£ng rá»—ng, Ä‘ang táº£i vÃ  thÃ´ng bÃ¡o lá»—i/thÃ nh cÃ´ng Ä‘Ã£ cÃ³.

## Quy táº¯c Ä‘Æ¡n hÃ ng vÃ  tá»“n kho

Giá» lÆ°u ID `PRODUCT_DETAIL`. Checkout láº¥y khÃ¡ch tá»« phiÃªn, khÃ´ng nháº­n customer ID tÃ¹y Ã½. Backend Ä‘á»c giÃ¡ Oracle, kiá»ƒm tra tráº¡ng thÃ¡i khÃ¡ch/sáº£n pháº©m/biáº¿n thá»ƒ; sá»‘ lÆ°á»£ng pháº£i lÃ  sá»‘ nguyÃªn dÆ°Æ¡ng. PhÃ­ giao cá»‘ Ä‘á»‹nh 30.000 â‚« do shop quy Ä‘á»‹nh; bá» qua phÃ­/tá»•ng/giÃ¡ tá»« trÃ¬nh duyá»‡t. Tiá»n giáº£m tÃ­nh láº¡i tá»« voucher trÃªn server.

Khi Ä‘áº·t Ä‘Æ¡n, khÃ³a biáº¿n thá»ƒ theo thá»© tá»± ID vÃ  trá»« tá»“n báº±ng UPDATE cÃ³ Ä‘iá»u kiá»‡n. CÃ¹ng transaction ghi Ä‘Æ¡n, dÃ²ng hÃ ng, lá»‹ch sá»­, chuyá»ƒn Ä‘á»™ng tá»“n vÃ  lÆ°á»£t voucher; lá»—i á»Ÿ bÆ°á»›c nÃ o rollback toÃ n bá»™. Thá»© tá»± khÃ³a nháº¥t quÃ¡n giÃºp giáº£m deadlock khi cÃ³ nhiá»u dÃ²ng hÃ ng.

CÃ¡c chuyá»ƒn tráº¡ng thÃ¡i há»£p lá»‡:

```text
Chá» xÃ¡c nháº­n (0) â†’ ÄÃ£ xÃ¡c nháº­n (1) â†’ Äang giao (2) â†’ HoÃ n thÃ nh (3) â†’ Tráº£ hÃ ng (5)
          0 hoáº·c 1 â†’ ÄÃ£ há»§y (4)
```

Há»§y hoáº·c ghi nháº­n thanh toÃ¡n tháº¥t báº¡i trÆ°á»›c khi giao dÃ¹ng cÃ¹ng luá»“ng há»§y. KhÃ³a dÃ²ng BILL vÃ  Ä‘á»•i STOCK_RESERVED tá»« 1 sang 0, rá»“i hoÃ n Ä‘Ãºng cÃ¡c biáº¿n thá»ƒ má»™t láº§n. YÃªu cáº§u há»§y láº·p bá»‹ tá»« chá»‘i. ÄÆ¡n cÅ© trÆ°á»›c migration khÃ´ng Ä‘Æ°á»£c tá»± Ä‘Ã¡nh dáº¥u Ä‘Ã£ giá»¯ tá»“n, trÃ¡nh bá»‹a lá»‹ch sá»­/hoÃ n tá»“n sai.

HoÃ n thÃ nh COD tá»± ghi nháº­n Ä‘Ã£ thanh toÃ¡n; chuyá»ƒn khoáº£n pháº£i ghi nháº­n nháº­n tiá»n trÆ°á»›c khi hoÃ n thÃ nh. Tráº¡ng thÃ¡i yÃªu cáº§u tráº£ khÃ´ng tá»± tÄƒng tá»“n: nháº­n hÃ ng báº±ng phiáº¿u RETURN gáº¯n Ä‘Æ¡n gá»‘c vÃ  ADMIN xÃ¡c nháº­n. KhÃ´ng tá»± chuyá»ƒn tiá»n hoÃ n hoáº·c nháº­p bÃ¡n láº¡i hÃ ng chÆ°a kiá»ƒm tra. ÄÆ¡n Ä‘Ã£ káº¿t thÃºc khÃ´ng sá»­a thÃ´ng tin giao/thu tiá»n; khÃ´ng Ä‘á»•i Ä‘Ã£ thu tiá»n vá» chÆ°a thu tiá»n.

## áº¢nh vÃ  báº£o máº­t cáº¥u hÃ¬nh

Máº·c Ä‘á»‹nh áº£nh lÆ°u táº¡i `%USERPROFILE%/clothing-shop-uploads`, ngoÃ i mÃ£ nguá»“n. CÃ³ thá»ƒ Ä‘áº·t `SHOP_UPLOAD_DIR` trong cáº¥u hÃ¬nh mÃ´i trÆ°á»ng cá»¥c bá»™. Oracle chá»‰ lÆ°u URL/thá»© tá»±/mÃ u. KhÃ´ng dÃ¹ng tÃªn file khÃ¡ch cung cáº¥p; URL file chá»‰ nháº­n tÃªn UUID vÃ  Ä‘uÃ´i áº£nh há»£p lá»‡.

Gá»¡ áº£nh chá»‰ xÃ³a liÃªn káº¿t Oracle. Cháº¡y `./shop.cmd -Mode CleanImages` Ä‘á»ƒ dá»n file server táº¡o Ä‘Ã£ quÃ¡ 24 giá» vÃ  khÃ´ng cÃ²n Ä‘Æ°á»£c tham chiáº¿u tá»« PRODUCT_IMAGE, PRODUCT hoáº·c PRODUCT_DETAIL; file cÃ²n Ä‘Æ°á»£c sáº£n pháº©m khÃ¡c dÃ¹ng sáº½ Ä‘Æ°á»£c giá»¯. KhÃ´ng tá»± xÃ³a áº£nh cÅ© bÃªn ngoÃ i thÆ° má»¥c upload.

## Káº¿t quáº£ nghiá»‡m thu

ÄÃ£ cháº¡y Maven vÃ  á»©ng dá»¥ng trÃªn Java 17.0.19, Spring Boot 3.3.5, Oracle tháº­t. Bá»™ `OracleAcceptanceTest` khá»Ÿi Ä‘á»™ng HTTP server tháº­t trÃªn cá»•ng ngáº«u nhiÃªn, gá»i HTTP qua cookie/CSRF vÃ  Ä‘á»‘i chiáº¿u SQL, sau Ä‘Ã³ dá»n chÃ­nh cÃ¡c fixture cá»§a nÃ³. KhÃ´ng mock database.

ÄÃ£ qua: truy cáº­p chÆ°a Ä‘Äƒng nháº­p; Ä‘Äƒng nháº­p Ä‘Ãºng/sai; CSRF; STAFF khÃ´ng Ä‘Æ°á»£c quáº£n lÃ½ tÃ i khoáº£n; khÃ³a máº¥t quyá»n phiÃªn; khÃ´ng tá»± khÃ³a admin; API khÃ´ng lá»™ hash; khÃ³a/má»Ÿ khÃ¡ch vÃ  buá»™c Ä‘Äƒng nháº­p láº¡i; vÃ²ng láº·p danh má»¥c; táº¡o ba biáº¿n thá»ƒ; trÃ¹ng SKU vÃ  mÃ uâ€“size; rollback sáº£n pháº©m lá»—i; upload PNG vÃ  tá»« chá»‘i áº£nh giáº£; chá»n áº£nh Ä‘áº¡i diá»‡n/gÃ¡n mÃ u/gá»¡ cover khi Ä‘ang bÃ¡n bá»‹ tá»« chá»‘i; giÃ¡/phÃ­ do server tÃ­nh; voucher há»£p lá»‡/háº¿t háº¡n/chÆ°a hiá»‡u lá»±c/chÆ°a Ä‘áº¡t tá»‘i thiá»ƒu/háº¿t lÆ°á»£t; hai Ä‘Æ¡n cáº¡nh tranh tá»“n; hai Ä‘Æ¡n cáº¡nh tranh lÆ°á»£t voucher; tráº¡ng thÃ¡i Ä‘Ãºng/sai; há»§y láº·p khÃ´ng hoÃ n tá»“n hai láº§n; tá»“n biáº¿n thá»ƒ khÃ¡c giá»¯ nguyÃªn; Ä‘iá»u chá»‰nh Ã¢m vÆ°á»£t tá»“n bá»‹ tá»« chá»‘i; checkout láº¥y Ä‘Ãºng customer tá»« phiÃªn; Ä‘á»‘i chiáº¿u sá»‘ Ä‘Æ¡n dashboard vá»›i SQL.

Cháº¡y láº¡i:

```bat
./shop.cmd -Mode Test
```

Máº·c Ä‘á»‹nh `mvn test` bá» qua bá»™ Oracle Ä‘á»ƒ trÃ¡nh tá»± sá»­a database náº¿u chÆ°a chá»§ Ä‘á»™ng báº­t `RUN_ORACLE_TESTS=true`. Script Test báº­t biáº¿n nÃ y vÃ  dÃ¹ng upload test trong `.local`. Káº¿t quáº£ nghiá»‡m thu Ä‘Æ°á»£c ghi táº¡i `.local/java17-test.log`; build táº¡i `.local/build.log`.

Hai test DOM dÃ¹ng mÃ£ JavaScript tháº­t Ä‘Ã£ qua: form admin gá»­i ba biáº¿n thá»ƒ; gallery Ä‘á»•i áº£nh theo mÃ u, giá»¯ áº£nh khi Ä‘á»•i size, fallback áº£nh chung vÃ  giá» lÆ°u ID biáº¿n thá»ƒ. CÃ i `jsdom@26.1.0` báº±ng `npm install --prefix .local/dom-tests jsdom@26.1.0 --no-audit --no-fund`, rá»“i `node --test tools/frontend.test.cjs`. KhÃ´ng cáº§n Node Ä‘á»ƒ cháº¡y cá»­a hÃ ng.

HTTP trÃªn backend cá»•ng 8080 tráº£ 200 cho login, storefront gá»‘c, shop tháº­t, catalog vÃ  CSRF. **ChÆ°a xÃ¡c minh bá»‘ cá»¥c vÃ  thao tÃ¡c báº±ng Chrome tháº­t:** bá»™ phÃª duyá»‡t tá»± Ä‘á»™ng tá»« chá»‘i Ä‘iá»u khiá»ƒn Google Chrome. Test DOM khÃ´ng thay tháº¿ xÃ¡c nháº­n hiá»ƒn thá»‹ trÃªn trÃ¬nh duyá»‡t. Cáº§n tá»± má»Ÿ URL, táº¡o admin báº±ng lá»‡nh trÃªn rá»“i thá»­ cÃ¡c trang; khÃ´ng cáº§n gá»­i máº­t kháº©u qua chat.

## Giá»¯ cÃ´ng viá»‡c cÃ³ sáºµn

Giá»¯ nhÃ¡nh `anh-hieu`, khÃ´ng commit/push. Giá»¯ thay Ä‘á»•i ban Ä‘áº§u cá»§a `pom.xml` (ojdbc11 23.7.0.25.01), `SqlHelper.java` (khÃ´ng sá»­a thÃªm) vÃ  cáº¥u hÃ¬nh FREEPDB1/máº­t kháº©u tá»« biáº¿n mÃ´i trÆ°á»ng trong `application.properties`. CÃ¡c pháº§n báº£o máº­t Ä‘Æ°á»£c thÃªm vÃ o cáº¥u hÃ¬nh hiá»‡n cÃ³. KhÃ´ng sá»­a `.vscode/oracle-local.env` hay `.vscode/launch.json`; `.vscode`, upload vÃ  `.local` Ä‘Æ°á»£c bá» qua bá»Ÿi Git. Giao diá»‡n máº«u chá»‰ thÃªm liÃªn káº¿t vÃ  náº¡p script cho luá»“ng dá»¯ liá»‡u tháº­t.
