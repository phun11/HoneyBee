# HoneyBee Oracle Version

Bản này chạy với Oracle schema `HONEYBEE_WEB`, không dùng H2. Hướng dẫn chi tiết nằm trong:

```text
README_ORACLE_TERMINAL_RUN.md
```

Chạy nhanh trong VS Code Terminal:

```powershell
.\tools\windows\reset-db-oracle.bat
.\tools\windows\run-web-oracle.bat
```

Sau đó mở:

```text
http://localhost:8080/api/system/health
http://localhost:8080/login.html
```

Tính năng mới: Farm upload ảnh sản phẩm, ảnh được lưu vào Oracle `PRODUCTS.PRODUCT_IMAGE_B64` và hiển thị ở Products, Farm, Transport, Store, Trace/QR.
