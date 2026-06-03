// Oracle version: disable previous PWA cache/service-worker during development
(async function(){
  if ('serviceWorker' in navigator) {
    try {
      const regs = await navigator.serviceWorker.getRegistrations();
      for (const r of regs) await r.unregister();
      if (window.caches) {
        const keys = await caches.keys();
        await Promise.all(keys.map(k => caches.delete(k)));
      }
    } catch(e) { console.warn('PWA cleanup skipped', e); }
  }
})();
