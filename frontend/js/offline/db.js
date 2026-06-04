// HoneyBee Offline IndexedDB layer
// Stores:
// - offlineActions: các thao tác nghiệp vụ lưu tạm khi mất kết nối.
// - traceCache: cache thông tin truy xuất QR/product để scanner và product-detail vẫn hiển thị khi offline.
window.HBOfflineDB = (() => {
  const DB_NAME = 'honeybee-offline-db';
  const DB_VERSION = 2;
  const ACTION_STORE = 'offlineActions';
  const TRACE_STORE = 'traceCache';

  function createStores(db) {
    if (!db.objectStoreNames.contains(ACTION_STORE)) {
      const store = db.createObjectStore(ACTION_STORE, { keyPath: 'clientActionId' });
      store.createIndex('status', 'status', { unique: false });
      store.createIndex('createdAt', 'createdAt', { unique: false });
    }
    if (!db.objectStoreNames.contains(TRACE_STORE)) {
      const store = db.createObjectStore(TRACE_STORE, { keyPath: 'token' });
      store.createIndex('productId', 'productId', { unique: false });
      store.createIndex('cachedAt', 'cachedAt', { unique: false });
    }
  }

  function open() {
    return new Promise((resolve, reject) => {
      const req = indexedDB.open(DB_NAME, DB_VERSION);
      req.onupgradeneeded = () => createStores(req.result);
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
      req.onblocked = () => console.warn('[HoneyBee IndexedDB] Upgrade blocked. Close old tabs if cache does not update.');
    });
  }

  async function tx(storeName, mode, fn) {
    const db = await open();
    return new Promise((resolve, reject) => {
      const t = db.transaction(storeName, mode);
      const store = t.objectStore(storeName);
      let result;
      try { result = fn(store); } catch (e) { reject(e); return; }
      t.oncomplete = () => resolve(result);
      t.onerror = () => reject(t.error);
      t.onabort = () => reject(t.error);
    });
  }

  function requestToPromise(req) {
    return new Promise((resolve, reject) => {
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
  }

  async function put(action) { return tx(ACTION_STORE, 'readwrite', store => store.put(action)); }
  async function remove(id) { return tx(ACTION_STORE, 'readwrite', store => store.delete(id)); }
  async function getAll() {
    const db = await open();
    return new Promise((resolve, reject) => {
      const t = db.transaction(ACTION_STORE, 'readonly');
      const req = t.objectStore(ACTION_STORE).getAll();
      req.onsuccess = () => resolve(req.result || []);
      req.onerror = () => reject(req.error);
    });
  }
  async function pending() { return (await getAll()).filter(a => a.status === 'PENDING' || a.status === 'FAILED_RETRY'); }
  async function countPending() { return (await pending()).length; }

  async function putTrace(record) {
    if (!record || !record.token) return null;
    record.token = String(record.token).trim();
    record.cachedAt = record.cachedAt || new Date().toISOString();
    return tx(TRACE_STORE, 'readwrite', store => store.put(record));
  }
  async function getTraceByToken(token) {
    if (!token) return null;
    const db = await open();
    return requestToPromise(db.transaction(TRACE_STORE, 'readonly').objectStore(TRACE_STORE).get(String(token).trim()));
  }
  async function getTraceByProductId(productId) {
    if (productId == null) return null;
    const db = await open();
    const key = Number(productId);
    return new Promise((resolve, reject) => {
      const t = db.transaction(TRACE_STORE, 'readonly');
      const req = t.objectStore(TRACE_STORE).index('productId').get(key);
      req.onsuccess = () => resolve(req.result || null);
      req.onerror = () => reject(req.error);
    });
  }
  async function getAllTrace() {
    const db = await open();
    return requestToPromise(db.transaction(TRACE_STORE, 'readonly').objectStore(TRACE_STORE).getAll()).then(x => x || []);
  }
  async function countTrace() { return (await getAllTrace()).length; }

  return { put, remove, getAll, pending, countPending, putTrace, getTraceByToken, getTraceByProductId, getAllTrace, countTrace };
})();
