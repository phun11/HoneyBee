// ============================================================
// Admin dashboard
// ------------------------------------------------------------
// Tập trung vào các nút demo thực tế: xem Oracle, lấy QR, quét QR,
// cập nhật đã bán/chưa bán và xem audit/security alert.
// ============================================================

async function initAdmin() {
  const statsEl = document.getElementById('stats');
  const alertsEl = document.getElementById('alerts');
  const auditsEl = document.getElementById('audits');
  const actionsEl = document.getElementById('quick-actions');
  const qrDemoEl = document.getElementById('qr-demo');

  actionsEl.innerHTML = [
    ['📦', 'Lô hàng & mã QR', 'Xem danh sách lô, token, ảnh QR để demo quét.', 'products.html'],
    ['🌱', 'Nông trại/Farm', 'Tạo lô, import Excel, xem trước và xác nhận sinh QR.', 'farm-management.html'],
    ['🚚', 'Vận chuyển', 'Nhận lô, cập nhật trạng thái và hành trình vận chuyển.', 'transport.html'],
    ['📷', 'Quét QR', 'Mở màn hình scanner bằng camera hoặc ảnh QR.', 'scanner.html'],
    ['🏪', 'Cửa hàng/POS', 'Đánh dấu QR đã bán/chưa bán để chống copy tem.', 'store.html'],
    ['🗄️', 'Oracle Health', 'Kiểm tra kết nối Oracle, schema và dữ liệu nền.', '/api/system/health']
  ].map(a => `
    <a class="hb-card hb-col-4" href="${a[3]}" ${a[3].startsWith('/') ? 'target="_blank"' : ''} style="display:block;box-shadow:none;background:#fffdf7">
      <span style="font-size:28px">${a[0]}</span>
      <h3 style="margin:8px 0 4px">${a[1]}</h3>
      <p style="margin:0;color:var(--hb-muted);line-height:1.45">${a[2]}</p>
    </a>`).join('');

  const d = await apiGet('/system/dashboard');
  try {
    const h = await apiGet('/system/health');
    const db = document.getElementById('db-status');
    if (db) db.innerHTML = `<div><span>User</span><span>${escapeHtml(h.currentUser)}</span></div><div><span>Schema version</span><span>${escapeHtml(h.schemaVersion)}</span></div><div><span>Required tables</span><span>${h.missingTables && h.missingTables.length ? 'Thiếu: '+escapeHtml(h.missingTables.join(', ')) : 'OK'}</span></div><div><span>Required columns</span><span>${h.missingColumns && h.missingColumns.length ? 'Thiếu: '+escapeHtml(h.missingColumns.join(', ')) : 'OK'}</span></div>`;
  } catch(e) {
    const db = document.getElementById('db-status');
    if (db) db.innerHTML = `<div><span>Oracle</span><span style="color:#b42318">${escapeHtml(e.message)}</span></div>`;
  }
  const cards = [
    ['📦', 'Lô hàng', d.products], ['🌱', 'Nông trại', d.farms], ['🚚', 'Vận chuyển', d.transporters],
    ['🏪', 'Cửa hàng', d.stores], ['📷', 'QR scans', d.scanLogs], ['🚨', 'Cảnh báo mở', d.securityAlerts]
  ];
  statsEl.innerHTML = cards.map(c => `<article class="hb-card hb-col-4 hb-stat"><div><span>${c[0]}</span><p>${c[1]}</p></div><strong>${c[2] ?? 0}</strong></article>`).join('');

  const qrRows = await apiGet('/system/qr-codes');
  qrDemoEl.innerHTML = qrRows.slice(0, 3).map(q => {
    const qrImg = `/api/qr/product/${encodeURIComponent(q.PRODUCT_ID)}/image`;
    const trace = `product-detail.html?token=${encodeURIComponent(q.QR_TOKEN)}&sig=${encodeURIComponent(q.QR_SIGNATURE || '')}`;
    return `<div style="display:flex;gap:12px;align-items:center;border-bottom:1px dashed var(--hb-border);padding:10px 0">
      <img src="${qrImg}" alt="QR" style="width:82px;height:82px;border-radius:12px;background:white;padding:4px">
      <div style="min-width:0">
        <b>${escapeHtml(q.PRODUCT_NAME)}</b><br>
        <small style="word-break:break-all;color:var(--hb-muted)">${escapeHtml(q.QR_TOKEN)}</small><br>
        <a class="hb-btn ghost" href="${trace}" style="margin-top:8px;padding:8px 12px">Xem trace</a>
        <a class="hb-btn ghost" href="${qrImg}" target="_blank" style="margin-top:8px;padding:8px 12px">Mở QR</a>
      </div>
    </div>`;
  }).join('');

  const alerts = await apiGet('/system/security-alerts');
  alertsEl.innerHTML = alerts.length ? alerts.slice(0,8).map(a => `<p><span class="hb-badge warning">${escapeHtml(a.ALERT_LEVEL)}</span> ${escapeHtml(a.ALERT_MESSAGE)}</p>`).join('') : '<p>Không có cảnh báo.</p>';

  const audits = await apiGet('/system/audit-logs');
  auditsEl.innerHTML = audits.slice(0,8).map(a => `<p><span class="hb-badge">${escapeHtml(a.ACTION_TYPE)}</span> ${escapeHtml(a.TABLE_NAME)} #${escapeHtml(a.RECORD_ID)}<br><small>${fmtDate(a.CREATED_AT)}</small></p>`).join('');
}

initAdmin().catch(e => alert(e.message));
