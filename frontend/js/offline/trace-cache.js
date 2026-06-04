// HoneyBee offline trace cache
// Mục tiêu: khi mất Wi-Fi/server, scanner vẫn mở được product-detail.html và hiển thị thông tin sản phẩm đã cache.
window.HBTraceCache = (() => {
  let warming = false;

  function tokenOf(data, fallback) {
    const info = data?.info || data?.INFO || data || {};
    return String(fallback || info.QR_TOKEN || info.qrToken || '').trim();
  }

  function productIdOf(data, fallback) {
    const info = data?.info || data?.INFO || data || {};
    const v = fallback ?? info.PRODUCT_ID ?? info.productId;
    return v == null || v === '' ? null : Number(v);
  }

  function signatureOf(data, fallback) {
    const info = data?.info || data?.INFO || data || {};
    return String(fallback || info.QR_SIGNATURE || info.qrSignature || '').trim();
  }

  async function cacheTrace(data, meta = {}) {
    if (!window.HBOfflineDB || !data) return null;
    const token = tokenOf(data, meta.token);
    const productId = productIdOf(data, meta.productId);
    if (!token) return null;
    const info = data.info || {};
    const record = {
      token,
      productId,
      signature: signatureOf(data, meta.signature),
      data,
      productName: info.PRODUCT_NAME || info.productName || '',
      batchCode: info.BATCH_CODE || info.batchCode || '',
      imageUrl: info.IMAGE_URL || info.imageUrl || '',
      source: meta.source || 'ONLINE_TRACE',
      cachedAt: new Date().toISOString()
    };
    await HBOfflineDB.putTrace(record);
    return record;
  }

  async function getByToken(token) {
    if (!window.HBOfflineDB || !token) return null;
    return HBOfflineDB.getTraceByToken(String(token).trim());
  }

  async function getByProductId(productId) {
    if (!window.HBOfflineDB || productId == null) return null;
    return HBOfflineDB.getTraceByProductId(Number(productId));
  }

  async function serverReady() {
    try {
      const res = await fetch(`${API_BASE}/system/health`, { cache: 'no-store' });
      if (!res.ok) return false;
      const json = await res.json();
      return !!(json.success && json.data && json.data.ready);
    } catch { return false; }
  }

  async function warmTraceCache(options = {}) {
    if (warming || !window.HBOfflineDB) return { skipped: true };
    if (!(await serverReady())) return { skipped: true, reason: 'server-not-ready' };
    warming = true;
    try {
      const res = await fetch(`${API_BASE}/system/offline-trace-cache`, { cache: 'no-store' });
      const json = await res.json();
      if (!json.success) throw new Error(json.message || 'offline trace cache API error');
      const rows = Array.isArray(json.data) ? json.data : [];
      let ok = 0;
      for (const row of rows) {
        const trace = row.trace || row.TRACE;
        const token = row.qrToken || row.QR_TOKEN;
        const productId = row.productId || row.PRODUCT_ID;
        const signature = row.qrSignature || row.QR_SIGNATURE;
        if (trace && token) {
          await cacheTrace(trace, { token, productId, signature, source: 'PREWARM_API' });
          ok++;
        }
      }
      if (!options.silent && window.HBOfflineUI) {
        HBOfflineUI.show(`Đã cache ${ok} hồ sơ QR để quét offline.`, 'ok', 5000);
      }
      return { success: true, count: ok };
    } catch (e) {
      console.warn('[HoneyBee] Warm trace cache failed', e);
      if (!options.silent && window.HBOfflineUI) HBOfflineUI.show('Không cache được dữ liệu QR offline: ' + e.message, 'warn', 6500);
      return { success: false, message: e.message };
    } finally {
      warming = false;
    }
  }

  async function cacheSummaryText() {
    try {
      const n = await HBOfflineDB.countTrace();
      return `QR offline cache: ${n} sản phẩm`;
    } catch { return 'QR offline cache chưa sẵn sàng'; }
  }

  return { cacheTrace, getByToken, getByProductId, warmTraceCache, cacheSummaryText };
})();
