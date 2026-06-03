# API Document

Base URL: `http://localhost:8080/api`

## Products
- `GET /products`: lấy danh sách sản phẩm.
- `GET /products/{id}`: lấy chi tiết sản phẩm.
- `POST /products`: thêm sản phẩm, backend gọi procedure `PRC_ADD_PRODUCT`, sau đó sinh QR.
- `PUT /products/{id}`: cập nhật sản phẩm.
- `DELETE /products/{id}`: xóa sản phẩm.

Body mẫu POST:
```json
{
  "farmId": 1,
  "productName": "Dưa leo hữu cơ",
  "category": "Rau củ",
  "description": "Dưa leo sạch",
  "price": 25000,
  "imageUrl": "assets/default.svg",
  "cultivationPlace": "Nhà kính A2",
  "sowingDate": "2026-04-01",
  "harvestDate": "2026-05-10",
  "productionProcess": "Gieo hạt, chăm sóc hữu cơ, thu hoạch và đóng gói."
}
```

## Farms
- `GET /farms`
- `GET /farms/{id}`
- `POST /farms`
- `PUT /farms/{id}`
- `DELETE /farms/{id}`

## Traceability
- `GET /trace/product/{id}`: lấy thông tin truy xuất, farm, origin, transport, updates.
- `GET /trace/token/{token}`: resolve QR token sang productId và ghi scan log.

## QR
- `GET /qr/product/{id}/image`: trả về ảnh QR PNG.
- `GET /qr/text?value=...`: sinh QR PNG từ text bất kỳ.
- `POST /qr/product/{id}`: tạo QR record mới cho sản phẩm.
