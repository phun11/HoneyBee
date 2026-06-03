// ============================================================
// Products / QR list page
// ------------------------------------------------------------
// Trang này giải quyết nhu cầu demo quan trọng:
// - Xem lô hàng trong Oracle database.
// - Lấy token QR mẫu để quét/nhập thủ công.
// - Xem/tải ảnh QR do backend sinh từ QR_URL trong DB.
// ============================================================

function saleBadge(status) {
  const s = String(status || 'NOT_SOLD');
  if (s === 'SOLD') return '<span class="hb-badge danger">🏷️ Đã bán</span>';
  if (s === 'RETURNED') return '<span class="hb-badge warning">↩️ Hoàn trả</span>';
  return '<span class="hb-badge safe">🛒 Chưa bán</span>';
}

function qrBadge(status) {
  const s = String(status || 'ACTIVE');
  if (s === 'ACTIVE') return '<span class="hb-badge safe">✅ QR ACTIVE</span>';
  if (s === 'SUSPICIOUS') return '<span class="hb-badge warning">⚠️ QR SUSPICIOUS</span>';
  return `<span class="hb-badge danger">🚨 QR ${escapeHtml(s)}</span>`;
}

async function loadProducts() {
  const el = document.getElementById('products');
  el.innerHTML = '<article class="hb-card hb-col-12">Đang tải danh sách QR...</article>';
  const rows = await apiGet('/system/qr-codes');

  if (!rows.length) {
    el.innerHTML = '<article class="hb-card hb-col-12">Chưa có QR nào trong database.</article>';
    return;
  }

  el.innerHTML = rows.map(r => {
    const qrImg = `/api/qr/product/${encodeURIComponent(r.PRODUCT_ID)}/image`;
    const traceUrl = `product-detail.html?token=${encodeURIComponent(r.QR_TOKEN)}&sig=${encodeURIComponent(r.QR_SIGNATURE || '')}`;
    return `
      <article class="hb-card hb-col-4" style="display:flex;flex-direction:column;gap:12px">
        <div style="display:flex;justify-content:space-between;gap:10px;align-items:flex-start;flex-wrap:wrap">
          <div>${qrBadge(r.QR_STATUS)} ${saleBadge(r.SALE_STATUS)}</div>
        </div>
        <div style="display:flex;gap:14px;align-items:center">
          <img src="${escapeHtml(productImageSrc(r))}" alt="product" style="width:74px;height:74px;border-radius:18px;background:#fff3c4;padding:10px" onerror="this.src='assets/default.svg'">
          <div>
            <h3 style="margin:0 0 4px">${escapeHtml(r.PRODUCT_NAME)}</h3>
            <p style="margin:0;color:var(--hb-muted)">${escapeHtml(r.CATEGORY)} · ${escapeHtml(r.BATCH_CODE)}</p>
          </div>
        </div>
        <div style="text-align:center;background:#fffdf7;border:1px dashed var(--hb-border);border-radius:18px;padding:12px">
          <img src="${qrImg}" alt="QR code" style="width:180px;max-width:100%;border-radius:14px;background:white;padding:8px">
          <p style="font-size:.86rem;word-break:break-all;color:var(--hb-muted)"><b>Token:</b> ${escapeHtml(r.QR_TOKEN)}</p>
        </div>
        <div class="hb-kv">
          <div><span>Trạng thái lô</span><span>${escapeHtml(r.PRODUCT_STATUS)}</span></div>
          <div><span>Giá</span><span>${escapeHtml(r.PRICE)} VND</span></div>
          <div><span>Đã bán lúc</span><span>${fmtDate(r.SOLD_AT)}</span></div>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:auto">
          <a class="hb-btn" href="${traceUrl}">Xem trace</a>
          <a class="hb-btn ghost" href="${qrImg}" target="_blank">Mở QR</a>
        </div>
      </article>`;
  }).join('');
}

loadProducts().catch(e => {
  document.getElementById('products').innerHTML = `<article class="hb-card hb-col-12 hb-trust danger"><h2>Lỗi tải lô hàng</h2><p>${escapeHtml(e.message)}</p></article>`;
});
