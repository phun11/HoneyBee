// HoneyBee Offline status/toast UI
window.HBOfflineUI = (() => {
  function ensure() {
    let bar = document.getElementById('hb-offline-bar');
    if (!bar) {
      bar = document.createElement('div');
      bar.id = 'hb-offline-bar';
      bar.style.cssText = 'position:fixed;left:16px;right:16px;bottom:16px;z-index:9999;padding:12px 16px;border-radius:16px;box-shadow:0 10px 28px rgba(0,0,0,.18);font-weight:700;background:#fff8e1;color:#684200;border:1px solid #f4c542;display:none;';
      document.body.appendChild(bar);
    }
    return bar;
  }
  function show(message, type='info', timeout=4500) {
    const bar = ensure();
    const palette = {
      info: ['#fff8e1','#684200','#f4c542'],
      ok: ['#eafaf0','#176b35','#7ad99a'],
      warn: ['#fff4e5','#92400e','#f59e0b'],
      error: ['#fee2e2','#991b1b','#ef4444']
    }[type] || ['#fff8e1','#684200','#f4c542'];
    bar.style.background = palette[0]; bar.style.color = palette[1]; bar.style.borderColor = palette[2];
    bar.textContent = message;
    bar.style.display = 'block';
    if (timeout) setTimeout(() => { if (bar.textContent === message) bar.style.display = 'none'; }, timeout);
  }
  async function updatePendingBadge() {
    try {
      const n = await HBOfflineDB.countPending();
      let badge = document.getElementById('hb-sync-badge');
      if (!badge) {
        badge = document.createElement('button');
        badge.id = 'hb-sync-badge';
        badge.type = 'button';
        badge.style.cssText = 'position:fixed;right:18px;top:82px;z-index:9998;border:0;border-radius:999px;padding:10px 14px;background:#1f2937;color:white;font-weight:700;box-shadow:0 8px 22px rgba(0,0,0,.16);cursor:pointer;';
        badge.onclick = () => HBOfflineSync.syncNow(true);
        document.body.appendChild(badge);
      }
      badge.textContent = navigator.onLine ? `Online · chờ sync: ${n}` : `Offline · đã lưu tạm: ${n}`;
      badge.style.background = n > 0 ? (navigator.onLine ? '#1d4ed8' : '#92400e') : '#15803d';
    } catch(e) { console.warn(e); }
  }
  return { show, updatePendingBadge };
})();
