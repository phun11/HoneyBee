// HoneyBee PWA registration for offline UI cache + offline QR trace cache.
(function(){
  function notify(message, type = 'info', timeout = 5000) {
    if (window.HBOfflineUI && typeof HBOfflineUI.show === 'function') {
      HBOfflineUI.show(message, type, timeout);
      return;
    }
    console.log('[HoneyBee PWA]', message);
  }

  function loadScriptOnce(src) {
    return new Promise((resolve, reject) => {
      if ([...document.scripts].some(s => (s.getAttribute('src') || '').split('?')[0] === src)) return resolve();
      const s = document.createElement('script');
      s.src = src;
      s.onload = () => resolve();
      s.onerror = () => reject(new Error('Cannot load ' + src));
      document.head.appendChild(s);
    });
  }

  async function ensureOfflineModules() {
    try {
      if (!window.HBOfflineDB) await loadScriptOnce('/js/offline/db.js');
      if (!window.HBOfflineUI) await loadScriptOnce('/js/offline/offline-ui.js');
      if (!window.HBOfflineQueue) await loadScriptOnce('/js/offline/offline-queue.js');
      if (!window.HBOfflineSync) await loadScriptOnce('/js/offline/sync-manager.js');
      if (!window.HBTraceCache) await loadScriptOnce('/js/offline/trace-cache.js');
      await HBOfflineUI.updatePendingBadge?.();
    } catch (e) {
      console.warn('[HoneyBee] Cannot load offline modules', e);
    }
  }

  async function registerServiceWorker() {
    await ensureOfflineModules();
    if (!('serviceWorker' in navigator)) {
      notify('Trình duyệt không hỗ trợ Service Worker. Chỉ lưu offline trong tab đang mở.', 'warn', 6500);
      return;
    }
    try {
      const reg = await navigator.serviceWorker.register('/sw.js?v=offline-qr-cache-v3', { scope: '/' });
      console.log('[HoneyBee] Service worker registered', reg.scope);

      await navigator.serviceWorker.ready;
      notify('Offline cache đã sẵn sàng. Trang QR/Farm/Store/Admin có thể mở lại từ cache sau khi mất kết nối.', 'ok', 6500);

      // Preload all trace records while online. This is the key for QR scan detail offline.
      setTimeout(() => window.HBTraceCache?.warmTraceCache({ silent: false }), 1000);
      setInterval(() => window.HBTraceCache?.warmTraceCache({ silent: true }), 60000);

      if (!navigator.serviceWorker.controller && !sessionStorage.getItem('hb_sw_first_reload_done')) {
        sessionStorage.setItem('hb_sw_first_reload_done', '1');
        setTimeout(() => location.reload(), 500);
      }
    } catch (e) {
      console.warn('[HoneyBee] Service worker registration failed', e);
      notify('Không đăng ký được offline cache: ' + e.message, 'error', 8000);
    }
  }

  navigator.serviceWorker?.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'HB_SW_READY') {
      notify('Offline cache đã cập nhật. Dữ liệu QR sẽ được lưu local để quét khi mất Wi-Fi.', 'ok', 6500);
      setTimeout(() => window.HBTraceCache?.warmTraceCache({ silent: false }), 500);
    }
  });

  window.addEventListener('online', () => {
    setTimeout(() => window.HBTraceCache?.warmTraceCache({ silent: false }), 1500);
  });

  window.addEventListener('load', registerServiceWorker);
})();
