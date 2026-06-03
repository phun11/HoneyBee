// ============================================================
// HoneyBee Trace API helper
// ------------------------------------------------------------
// Mục tiêu UX/mobile:
// - Laptop mở http://localhost:5500 -> gọi http://localhost:8080/api.
// - Điện thoại mở http://IP-LAPTOP:5500 -> tự gọi http://IP-LAPTOP:8080/api.
// - Nếu cần server public/ngrok, có thể khai báo window.HB_API_BASE trước file này.
// ============================================================
const API_BASE = window.HB_API_BASE || `${window.location.protocol}//${window.location.hostname}:8080/api`;

/** Gọi API GET và chuẩn hóa lỗi để UI hiển thị dễ hiểu. */
async function apiGet(path) {
  const res = await fetch(`${API_BASE}${path}`);
  const json = await res.json();
  if (!json.success) throw new Error(json.message || 'API error');
  return json.data;
}

/** Gọi API POST dùng chung cho login, cập nhật trạng thái, ghi nhận scan... */
async function apiPost(path, body) {
  const res = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body || {})
  });
  const json = await res.json();
  if (!json.success) throw new Error(json.message || 'API error');
  return json.data;
}

/** Lấy query param từ URL, dùng cho token/id khi quét QR. */
function getParam(name) {
  return new URLSearchParams(location.search).get(name);
}

/** Format ngày giờ ngắn gọn cho mobile. */
function fmtDate(value) {
  if (!value || value === 'null') return 'Chưa có';
  return String(value).replace('T', ' ').substring(0, 19);
}

/** Chống XSS khi render dữ liệu DB ra HTML. */
function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>\"]/g, s => ({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;'}[s]));
}


/** Lấy user đăng nhập demo từ localStorage; nếu chưa login thì trả role GUEST. */
function currentUser() {
  try { return JSON.parse(localStorage.getItem('hb_user') || '{}'); }
  catch { return {}; }
}

/** Chuẩn hóa tên field user vì backend trả key dạng uppercase từ JDBC map. */
function normalizeUser(u) {
  return {
    userId: u.USER_ID || u.userId,
    username: u.USERNAME || u.username,
    fullName: u.FULL_NAME || u.fullName,
    role: u.ROLE || u.role || 'GUEST',
    farmId: u.FARM_ID || u.farmId,
    transporterId: u.TRANSPORTER_ID || u.transporterId,
    storeId: u.STORE_ID || u.storeId
  };
}

/** Trả về ảnh sản phẩm: ưu tiên ảnh Farm upload lưu trong Oracle CLOB, sau đó imageUrl, cuối cùng default. */
function productImageSrc(row) {
  if (!row) return 'assets/default.svg';
  const b64 = row.PRODUCT_IMAGE_B64 || row.productImageBase64 || row.PRODUCT_IMAGE_BASE64;
  const mime = row.PRODUCT_IMAGE_MIME || row.productImageMime || 'image/jpeg';
  if (b64 && String(b64).trim()) return `data:${mime};base64,${String(b64).trim()}`;
  return row.IMAGE_URL || row.imageUrl || 'assets/default.svg';
}
