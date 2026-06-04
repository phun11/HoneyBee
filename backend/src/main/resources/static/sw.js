// HoneyBee Offline Sync Service Worker
// - Cache UI shell so transport.html can still open after backend is stopped.
// - Do NOT cache /api calls; business data is handled by IndexedDB offline queue.
const CACHE_NAME = 'honeybee-offline-qr-cache-v3';
const CORE_ASSETS = [
  '/',
  '/index.html',
  '/login.html',
  '/admin.html',
  '/farm-management.html',
  '/transport.html',
  '/store.html',
  '/products.html',
  '/scanner.html',
  '/product-detail.html',
  '/offline.html',
  '/css/honeybee.css',
  '/css/style.css',
  '/js/api.js',
  '/js/login.js',
  '/js/transport.js',
  '/js/store.js',
  '/js/farms.js',
  '/js/products.js',
  '/js/scanner.js',
  '/js/detail.js',
  '/js/admin.js',
  '/js/pwa.js',
  '/js/offline/db.js',
  '/js/offline/offline-ui.js',
  '/js/offline/offline-queue.js',
  '/js/offline/sync-manager.js',
  '/js/offline/trace-cache.js',
  '/manifest.json',
  '/assets/default.svg',
  '/assets/CaiXanh.jpg',
  '/assets/XoaiCatHoaLoc.jpg',
  '/assets/CaiBeXanh.jpg',
  '/assets/CaChua.jpg',
  '/assets/ManHongSocTrang.jpg',
  '/assets/BongCaiXanh.jpg',
  '/assets/TaoDo.jpg',
  '/assets/ThanhLong.jpg',
  '/assets/ManHauSonLa.png',
  '/assets/icon-192.png',
  '/assets/icon-512.png'
];

async function cacheCoreAssets() {
  const cache = await caches.open(CACHE_NAME);
  // Do not let one missing asset fail the entire service worker installation.
  await Promise.allSettled(CORE_ASSETS.map(async (asset) => {
    try {
      const res = await fetch(asset, { cache: 'reload' });
      if (res && res.ok) await cache.put(asset, res.clone());
    } catch (e) {
      console.warn('[HoneyBee SW] Skip cache asset:', asset, e.message);
    }
  }));
}

self.addEventListener('install', (event) => {
  event.waitUntil(cacheCoreAssets().then(() => self.skipWaiting()));
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
      .then(async () => {
        const clients = await self.clients.matchAll({ includeUncontrolled: true, type: 'window' });
        clients.forEach(c => c.postMessage({ type: 'HB_SW_READY', cacheName: CACHE_NAME }));
      })
  );
});

function normalizePath(pathname) {
  if (pathname === '/') return '/index.html';
  return pathname;
}

async function networkThenCache(request) {
  const cache = await caches.open(CACHE_NAME);
  const response = await fetch(request);
  if (response && response.ok && request.method === 'GET') {
    cache.put(request, response.clone()).catch(() => {});
  }
  return response;
}

async function cachedPageFallback(request) {
  const url = new URL(request.url);
  const cache = await caches.open(CACHE_NAME);
  const path = normalizePath(url.pathname);
  return (await cache.match(path))
    || (await cache.match('/transport.html'))
    || (await cache.match('/offline.html'))
    || new Response('HoneyBee offline cache is not ready. Start backend once and open the page again.', {
      status: 503,
      headers: { 'Content-Type': 'text/plain; charset=utf-8' }
    });
}

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);

  // API calls must go to network. If they fail, frontend JS catches and stores actions in IndexedDB.
  if (url.pathname.startsWith('/api/')) return;

  // HTML navigation: network first, cached page fallback when backend is stopped.
  if (event.request.mode === 'navigate' || event.request.headers.get('accept')?.includes('text/html')) {
    event.respondWith(networkThenCache(event.request).catch(() => cachedPageFallback(event.request)));
    return;
  }

  // Static assets: cache first, then network, then default image fallback.
  event.respondWith(
    caches.match(event.request).then(cached => {
      if (cached) return cached;
      return networkThenCache(event.request).catch(async () => {
        if (url.pathname.match(/\.(png|jpg|jpeg|gif|webp|svg)$/i)) {
          return (await caches.match('/assets/default.svg')) || Response.error();
        }
        return Response.error();
      });
    })
  );
});
