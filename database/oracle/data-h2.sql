-- ============================================================
-- HoneyBee Trace - sample data
-- ============================================================
-- Dữ liệu demo cố tình có cả trường hợp SAFE và WARNING để trình bày QR security.
-- ============================================================

INSERT INTO FARMS(FARM_ID, FARM_NAME, ADDRESS, OWNER_NAME, CERTIFICATION, CONTACT_PHONE, CONTACT_EMAIL) VALUES
(1, 'Trang trại Mật Vàng Đà Lạt', 'Đà Lạt, Lâm Đồng', 'Đặng Thu Hà', 'VietGAP 2026', '0901000001', 'dalat@honeybee.demo'),
(2, 'Vườn Xoài Cái Bè Organic', 'Cái Bè, Tiền Giang', 'Võ Thanh Tâm', 'Organic Demo', '0901000002', 'caibe@honeybee.demo'),
(3, 'Nông trại Mộc Châu Xanh', 'Mộc Châu, Sơn La', 'Bùi Quốc Việt', 'GlobalGAP Demo', '0901000003', 'mocchau@honeybee.demo');

INSERT INTO TRANSPORTERS(TRANSPORTER_ID, TRANSPORTER_NAME, PHONE, ADDRESS, CONTACT_PERSON) VALUES
(1, 'BeeTruck Logistics', '0912000001', 'TP.HCM', 'Nguyễn Văn Tài'),
(2, 'Mekong Fresh Transport', '0912000002', 'Tiền Giang', 'Trần Minh Quân');

INSERT INTO STORES(STORE_ID, STORE_NAME, STORE_TYPE, ADDRESS, CITY, PHONE, CONTACT_PERSON) VALUES
(1, 'Honey Mart Quận 1', 'CLEAN_FOOD_STORE', 'Quận 1, TP.HCM', 'TP.HCM', '0923000001', 'Lê Thu Ngân'),
(2, 'Nhà hàng Green Table', 'RESTAURANT', 'Quận 3, TP.HCM', 'TP.HCM', '0923000002', 'Phạm Hoàng Anh');

INSERT INTO USERS(USER_ID, USERNAME, PASSWORD, FULL_NAME, EMAIL, ROLE, FARM_ID, TRANSPORTER_ID, STORE_ID) VALUES
(1, 'admin', 'admin123', 'Quản trị viên HoneyBee', 'admin@honeybee.demo', 'ADMIN', NULL, NULL, NULL),
(2, 'farm1', '123456', 'Nông trại Mật Vàng', 'farm1@honeybee.demo', 'FARMER', 1, NULL, NULL),
(3, 'transport1', '123456', 'BeeTruck Logistics', 'transport1@honeybee.demo', 'TRANSPORTER', NULL, 1, NULL),
(4, 'store1', '123456', 'Honey Mart Quận 1', 'store1@honeybee.demo', 'STORE', NULL, NULL, 1);

-- Luồng trạng thái hợp lệ. Procedure Oracle dùng bảng này để kiểm tra nhảy trạng thái.
INSERT INTO STATUS_TRANSITIONS(FROM_STATUS, TO_STATUS, ALLOWED_ROLE, DESCRIPTION) VALUES
('CREATED','FARM_CONFIRMED','FARMER','Nông trại xác nhận nguồn gốc.'),
('FARM_CONFIRMED','READY_FOR_TRANSPORT','FARMER','Nông trại xác nhận lô sẵn sàng bàn giao vận chuyển.'),
('FARM_CONFIRMED','CERTIFIED','ADMIN','Admin xác nhận chứng chỉ.'),
('CERTIFIED','READY_FOR_TRANSPORT','FARMER','Nông trại chuyển lô đã chứng nhận sang chờ vận chuyển.'),
('READY_FOR_TRANSPORT','IN_TRANSIT','TRANSPORTER','Đơn vị vận chuyển nhận hàng.'),
('IN_TRANSIT','DELIVERED_TO_STORE','TRANSPORTER','Đã giao tới cửa hàng.'),
('DELIVERED_TO_STORE','STORE_RECEIVED','STORE','Cửa hàng xác nhận nhận hàng.'),
('STORE_RECEIVED','AVAILABLE_FOR_SALE','STORE','Cửa hàng đưa lên kệ.'),
('AVAILABLE_FOR_SALE','SOLD_OUT','STORE','Cửa hàng đánh dấu bán hết.'),
('CREATED','CANCELLED','ADMIN','Hủy lô lỗi.'),
('AVAILABLE_FOR_SALE','REVOKED','ADMIN','Thu hồi lô có rủi ro.'),
('SOLD_OUT','REVOKED','ADMIN','Thu hồi sau bán do phát hiện lỗi.');

INSERT INTO PRODUCTS(PRODUCT_ID, FARM_ID, BATCH_CODE, PRODUCT_NAME, CATEGORY, DESCRIPTION, QUALITY_SUMMARY, PRICE, QUANTITY, UNIT, STATUS, VERSION_NO, IMAGE_URL, CREATED_BY) VALUES
(1, 1, 'HB-DL-RAU-001', 'Rau cải xanh HoneyBee - Lô Đà Lạt 001', 'Rau xanh', 'Lô rau cải xanh trồng tại nhà kính Đà Lạt.', 'Canh tác an toàn, thu hoạch trong ngày, bảo quản lạnh 6-10°C.', 32000, 120, 'kg', 'AVAILABLE_FOR_SALE', 4, 'assets/veg.svg', 2),
(2, 2, 'HB-CB-XOAI-002', 'Xoài Cát Hòa Lộc - Lô Cái Bè 002', 'Trái cây', 'Lô xoài tuyển chọn từ Cái Bè, Tiền Giang.', 'Bao trái tự nhiên, kiểm tra độ ngọt, đóng gói tại kho BlueFood.', 85000, 80, 'kg', 'READY_FOR_TRANSPORT', 2, 'assets/mango.svg', 2),
(3, 3, 'HB-MC-RAU-003', 'Rau cải Mộc Châu - Lô vận chuyển 003', 'Rau xanh', 'Rau cải trồng theo quy trình kiểm soát phân bón.', 'Đang vận chuyển, nhiệt độ bảo quản được ghi nhận theo từng chặng.', 30000, 200, 'kg', 'IN_TRANSIT', 1, 'assets/veg.svg', 2);

INSERT INTO PRODUCT_ORIGIN(PRODUCT_ID, CULTIVATION_PLACE, SOWING_DATE, HARVEST_DATE, EXPIRED_DATE, PRODUCTION_PROCESS) VALUES
(1, 'Nhà kính A1 - Đà Lạt', DATE '2026-04-10', DATE '2026-05-15', DATE '2026-05-20', 'Gieo hạt, chăm sóc hữu cơ, kiểm tra dư lượng, thu hoạch và đóng gói.'),
(2, 'Vườn xoài Cái Bè', DATE '2025-12-15', DATE '2026-05-08', DATE '2026-05-25', 'Bao trái, kiểm tra độ ngọt, phân loại kích thước, đóng thùng.'),
(3, 'Khu trồng rau Mộc Châu', DATE '2026-04-01', DATE '2026-05-12', DATE '2026-05-18', 'Canh tác an toàn, kiểm tra nguồn nước, sơ chế và đóng gói lạnh.');

INSERT INTO CERTIFICATES(CERTIFICATE_ID, FARM_ID, PRODUCT_ID, CERTIFICATE_NAME, ISSUED_BY, ISSUE_DATE, EXPIRED_DATE, FILE_URL, STATUS) VALUES
(1, 1, 1, 'VietGAP rau quả 2026', 'Trung tâm kiểm định Demo', DATE '2026-01-01', DATE '2026-12-31', 'docs/cert-vietgap-dalat.pdf', 'VALID'),
(2, 2, 2, 'Organic Demo 2026', 'HoneyBee QC', DATE '2026-01-15', DATE '2026-12-31', 'docs/cert-organic-caibe.pdf', 'VALID'),
(3, 3, 3, 'GlobalGAP Demo', 'HoneyBee QC', DATE '2026-02-01', DATE '2026-11-30', 'docs/cert-globalgap-mocchau.pdf', 'VALID');

INSERT INTO TRANSPORT_HISTORY(TRANSPORT_ID, PRODUCT_ID, TRANSPORTER_ID, TRANSPORT_COMPANY, FROM_LOCATION, TO_LOCATION, TRANSPORT_TIME, STORAGE_TEMPERATURE, STATUS, NOTE, CREATED_BY) VALUES
(1, 1, 1, 'BeeTruck Logistics', 'Đà Lạt, Lâm Đồng', 'Kho HoneyBee TP.HCM', TIMESTAMP '2026-05-15 08:00:00', 8.5, 'DELIVERED', 'Xe lạnh, seal nguyên vẹn.', 3),
(2, 1, 1, 'BeeTruck Logistics', 'Kho HoneyBee TP.HCM', 'Honey Mart Quận 1', TIMESTAMP '2026-05-16 09:30:00', 9.0, 'DELIVERED', 'Giao trong ngày.', 3),
(3, 2, 2, 'Mekong Fresh Transport', 'Cái Bè, Tiền Giang', 'Honey Mart Quận 1', TIMESTAMP '2026-05-10 07:00:00', 12.0, 'DELIVERED', 'Đóng thùng xốp.', 3),
(4, 3, 1, 'BeeTruck Logistics', 'Mộc Châu, Sơn La', 'Kho HoneyBee Hà Nội', TIMESTAMP '2026-05-15 06:00:00', 7.0, 'IN_TRANSIT', 'Đang vận chuyển.', 3);

INSERT INTO DISTRIBUTION_HISTORY(DISTRIBUTION_ID, PRODUCT_ID, STORE_ID, RECEIVED_AT, RECEIVED_BY, QUANTITY_RECEIVED, STATUS, NOTE) VALUES
(1, 1, 1, TIMESTAMP '2026-05-16 15:00:00', 4, 120, 'AVAILABLE_FOR_SALE', 'Cửa hàng xác nhận đủ số lượng.'),
(2, 2, 1, TIMESTAMP '2026-05-11 11:00:00', 4, 80, 'RECEIVED', 'Hàng đạt yêu cầu.' );

INSERT INTO BATCH_STATUS_HISTORY(PRODUCT_ID, OLD_STATUS, NEW_STATUS, CHANGED_BY, CHANGED_AT, NOTE) VALUES
(1, NULL, 'CREATED', 2, TIMESTAMP '2026-05-14 08:00:00', 'Nông trại tạo lô hàng.'),
(1, 'CREATED', 'FARM_CONFIRMED', 2, TIMESTAMP '2026-05-14 09:00:00', 'Xác nhận nguồn gốc.'),
(1, 'FARM_CONFIRMED', 'CERTIFIED', 1, TIMESTAMP '2026-05-14 10:00:00', 'HoneyBee xác nhận chứng chỉ.'),
(1, 'CERTIFIED', 'READY_FOR_TRANSPORT', 2, TIMESTAMP '2026-05-14 14:00:00', 'Farm bàn giao lô cho vận chuyển.'),
(1, 'READY_FOR_TRANSPORT', 'IN_TRANSIT', 3, TIMESTAMP '2026-05-15 08:00:00', 'Đơn vị vận chuyển nhận hàng.'),
(1, 'IN_TRANSIT', 'DELIVERED_TO_STORE', 3, TIMESTAMP '2026-05-16 09:30:00', 'Đơn vị vận chuyển giao tới cửa hàng.'),
(1, 'DELIVERED_TO_STORE', 'STORE_RECEIVED', 4, TIMESTAMP '2026-05-16 15:00:00', 'Cửa hàng xác nhận nhận hàng.'),
(1, 'STORE_RECEIVED', 'AVAILABLE_FOR_SALE', 4, TIMESTAMP '2026-05-16 15:30:00', 'Lô hàng đã sẵn sàng bán.'),
(2, NULL, 'CREATED', 2, TIMESTAMP '2026-05-08 08:00:00', 'Nông trại tạo lô hàng.'),
(2, 'CREATED', 'FARM_CONFIRMED', 2, TIMESTAMP '2026-05-08 09:00:00', 'Farm xác nhận nguồn gốc.'),
(2, 'FARM_CONFIRMED', 'READY_FOR_TRANSPORT', 2, TIMESTAMP '2026-05-09 08:00:00', 'Farm chuyển lô sang chờ vận chuyển.'),
(3, NULL, 'CREATED', 2, TIMESTAMP '2026-05-12 08:00:00', 'Nông trại tạo lô hàng.'),
(3, 'CREATED', 'FARM_CONFIRMED', 2, TIMESTAMP '2026-05-12 09:00:00', 'Farm xác nhận nguồn gốc.'),
(3, 'FARM_CONFIRMED', 'READY_FOR_TRANSPORT', 2, TIMESTAMP '2026-05-14 15:00:00', 'Farm chuyển sang chờ vận chuyển.'),
(3, 'READY_FOR_TRANSPORT', 'IN_TRANSIT', 3, TIMESTAMP '2026-05-15 06:00:00', 'Đang vận chuyển.');

INSERT INTO PRODUCT_UPDATES(PRODUCT_ID, UPDATE_TITLE, UPDATE_CONTENT, UPDATED_BY, UPDATED_AT) VALUES
(1, 'Thu hoạch', 'Rau được thu hoạch tại nhà kính A1 Đà Lạt.', 'Nông trại', TIMESTAMP '2026-05-15 06:30:00'),
(1, 'Kiểm tra chất lượng', 'Không phát hiện dư lượng vượt ngưỡng demo.', 'HoneyBee QC', TIMESTAMP '2026-05-15 10:00:00'),
(1, 'Đưa lên kệ', 'Lô rau đã được Honey Mart Quận 1 xác nhận.', 'Cửa hàng', TIMESTAMP '2026-05-16 15:30:00'),
(2, 'Bao trái', 'Hoàn tất bao trái bảo vệ tự nhiên.', 'Nông trại', TIMESTAMP '2026-05-08 09:00:00'),
(3, 'Sơ chế', 'Rửa, phân loại và đóng gói lạnh.', 'Nông trại', TIMESTAMP '2026-05-12 10:00:00');

-- Signature demo được backend tạo lại khi tạo QR mới. Dữ liệu mẫu dùng chuỗi cố định để test nhanh.
INSERT INTO QR_CODES(PRODUCT_ID, QR_TOKEN, QR_SIGNATURE, QR_URL, QR_IMAGE_URL, STATUS, SALE_STATUS) VALUES
(1, 'HB-QR-RAU-001-SAFE', 'DEMO_SIGNATURE_SAFE', 'http://localhost:8080/product-detail.html?token=HB-QR-RAU-001-SAFE&sig=DEMO_SIGNATURE_SAFE', 'http://localhost:8080/api/qr/product/1/image', 'ACTIVE', 'NOT_SOLD'),
(2, 'HB-QR-XOAI-002-WARN', 'DEMO_SIGNATURE_WARN', 'http://localhost:8080/product-detail.html?token=HB-QR-XOAI-002-WARN&sig=DEMO_SIGNATURE_WARN', 'http://localhost:8080/api/qr/product/2/image', 'SUSPICIOUS', 'NOT_SOLD'),
(3, 'HB-QR-RAU-003-ACTIVE', 'DEMO_SIGNATURE_ACTIVE', 'http://localhost:8080/product-detail.html?token=HB-QR-RAU-003-ACTIVE&sig=DEMO_SIGNATURE_ACTIVE', 'http://localhost:8080/api/qr/product/3/image', 'ACTIVE', 'NOT_SOLD');

INSERT INTO QR_SCAN_LOGS(QR_TOKEN, PRODUCT_ID, IP_ADDRESS, USER_AGENT, RESULT_STATUS, WARNING_LEVEL, WARNING_MESSAGE, SCANNED_AT) VALUES
('HB-QR-RAU-001-SAFE', 1, '127.0.0.1', 'Demo browser', 'SUCCESS', 'NONE', 'Không phát hiện bất thường.', TIMESTAMP '2026-05-16 16:00:00'),
('HB-QR-XOAI-002-WARN', 2, '127.0.0.1', 'Demo browser', 'SUCCESS', 'MEDIUM', 'QR có số lượt quét cao hơn bình thường trong demo.', TIMESTAMP '2026-05-16 16:05:00');

INSERT INTO QR_SECURITY_ALERTS(QR_TOKEN, PRODUCT_ID, ALERT_TYPE, ALERT_LEVEL, ALERT_MESSAGE) VALUES
('HB-QR-XOAI-002-WARN', 2, 'HIGH_SCAN_FREQUENCY', 'MEDIUM', 'Mã QR có dấu hiệu bị quét nhiều lần bất thường. Cần kiểm tra nguy cơ sao chép tem QR.');

INSERT INTO AUDIT_LOGS(TABLE_NAME, RECORD_ID, ACTION_TYPE, NEW_DATA, PERFORMED_BY) VALUES
('PRODUCTS', '1', 'INSERT', 'Tạo lô rau cải xanh HoneyBee', 'farm1'),
('PRODUCTS', '1', 'STATUS_CHANGE', 'AVAILABLE_FOR_SALE', 'store1'),
('QR_CODES', 'HB-QR-RAU-001-SAFE', 'INSERT', 'Tạo QR truy xuất an toàn cho lô hàng 1', 'system'),
('QR_SECURITY_ALERTS', '2', 'QR_ALERT', 'Cảnh báo QR nghi ngờ bị copy', 'system');

ALTER TABLE FARMS ALTER COLUMN FARM_ID RESTART WITH 100;
ALTER TABLE TRANSPORTERS ALTER COLUMN TRANSPORTER_ID RESTART WITH 100;
ALTER TABLE STORES ALTER COLUMN STORE_ID RESTART WITH 100;
ALTER TABLE USERS ALTER COLUMN USER_ID RESTART WITH 100;
ALTER TABLE PRODUCTS ALTER COLUMN PRODUCT_ID RESTART WITH 100;
ALTER TABLE PRODUCT_ORIGIN ALTER COLUMN ORIGIN_ID RESTART WITH 100;
ALTER TABLE TRANSPORT_HISTORY ALTER COLUMN TRANSPORT_ID RESTART WITH 100;
ALTER TABLE CERTIFICATES ALTER COLUMN CERTIFICATE_ID RESTART WITH 100;
ALTER TABLE DISTRIBUTION_HISTORY ALTER COLUMN DISTRIBUTION_ID RESTART WITH 100;
ALTER TABLE BATCH_STATUS_HISTORY ALTER COLUMN STATUS_HISTORY_ID RESTART WITH 100;
ALTER TABLE PRODUCT_UPDATES ALTER COLUMN UPDATE_ID RESTART WITH 100;
ALTER TABLE QR_CODES ALTER COLUMN QR_ID RESTART WITH 100;
ALTER TABLE QR_SCAN_LOGS ALTER COLUMN SCAN_ID RESTART WITH 100;
ALTER TABLE QR_SECURITY_ALERTS ALTER COLUMN ALERT_ID RESTART WITH 100;
ALTER TABLE AUDIT_LOGS ALTER COLUMN AUDIT_ID RESTART WITH 100;
