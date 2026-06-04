// HoneyBee Offline sync manager. Sends pending IndexedDB actions to Oracle through backend.
window.HBOfflineSync = (() => {
  let syncing = false;

  async function serverReady() {
    try {
      const res = await fetch(`${API_BASE}/system/health`, { cache: 'no-store' });
      if (!res.ok) return false;
      const json = await res.json();
      return !!(json.success && json.data && json.data.ready);
    } catch { return false; }
  }

  async function syncNow(manual=false) {
    if (syncing) return;
    const pending = await HBOfflineDB.pending();
    await HBOfflineUI.updatePendingBadge();
    if (!pending.length) {
      if (manual) HBOfflineUI.show('Không có thao tác offline nào cần đồng bộ.', 'ok');
      return;
    }
    if (!(await serverReady())) {
      if (manual) HBOfflineUI.show('Backend/Oracle chưa sẵn sàng. Vẫn giữ thao tác trong bộ nhớ local.', 'warn');
      return;
    }
    syncing = true;
    HBOfflineUI.show(`Đang đồng bộ ${pending.length} thao tác offline lên Oracle...`, 'info', 0);
    try {
      const user = normalizeUser(currentUser ? currentUser() : {});
      const res = await fetch(`${API_BASE}/offline-sync/batch`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ deviceId: HBOfflineQueue.deviceId(), username: user.username, roleName: user.role, actions: pending })
      });
      const json = await res.json();
      if (!json.success) throw new Error(json.message || 'Sync API error');
      const results = (json.data && json.data.results) || [];
      let ok = 0, fail = 0;
      for (const r of results) {
        if (r.status === 'SUCCESS' || r.status === 'DUPLICATE') { await HBOfflineDB.remove(r.clientActionId); ok++; }
        else {
          const old = pending.find(x => x.clientActionId === r.clientActionId);
          if (old) { old.status = 'FAILED_RETRY'; old.lastError = r.message; old.updatedAt = new Date().toISOString(); await HBOfflineDB.put(old); }
          fail++;
        }
      }
      await HBOfflineUI.updatePendingBadge();
      if (fail) HBOfflineUI.show(`Đồng bộ hoàn tất: ${ok} thành công, ${fail} thất bại. Bấm huy hiệu sync để thử lại.`, 'warn', 8000);
      else HBOfflineUI.show(`Đồng bộ thành công ${ok}/${pending.length} thao tác. Audit log đã được ghi vào Oracle.`, 'ok', 7000);
      if (typeof load === 'function') setTimeout(() => load().catch(()=>{}), 500);
    } catch (e) {
      HBOfflineUI.show('Đồng bộ lỗi: ' + e.message + '. Dữ liệu vẫn được giữ local.', 'error', 8000);
    } finally {
      syncing = false;
    }
  }

  window.addEventListener('online', () => { HBOfflineUI.show('Kết nối mạng đã khôi phục. Đang kiểm tra backend/Oracle...', 'info'); setTimeout(() => syncNow(false), 1200); });
  window.addEventListener('offline', () => { HBOfflineUI.show('Bạn đang offline. Thao tác mới sẽ được lưu tạm trên thiết bị.', 'warn', 0); HBOfflineUI.updatePendingBadge(); });
  document.addEventListener('DOMContentLoaded', () => { HBOfflineUI.updatePendingBadge(); setTimeout(() => syncNow(false), 1500); setInterval(() => syncNow(false), 12000); });
  return { syncNow, serverReady };
})();
