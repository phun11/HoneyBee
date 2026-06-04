const CACHE_NAME = 'honeybee-offline-sync-v1';
const APP_SHELL = [
  '/', '/login.html', '/admin.html', '/farm-management.html', '/transport.html', '/store.html', '/products.html', '/scanner.html', '/product-detail.html', '/offline.html',
  '/css/honeybee.css', '/css/style.css',
  '/js/api.js', '/js/login.js', '/js/transport.js', '/js/store.js', '/js/farms.js', '/js/products.js', '/js/scanner.js', '/js/detail.js', '/js/admin.js', '/js/pwa.js',
  '/js/offline/db.js', '/js/offline/offline-ui.js', '/js/offline/offline-queue.js', '/js/offline/sync-manager.js',
  '/assets/default.svg', '/assets/icon-192.png', '/assets/icon-512.png'
];
self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(APP_SHELL)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);
  if (url.pathname.startsWith('/api/')) return;
  event.respondWith(fetch(event.request).catch(() => caches.match(event.request).then(r => r || caches.match('/offline.html'))));
});
