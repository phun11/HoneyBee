// HoneyBee Offline action queue
window.HBOfflineQueue = (() => {
  function deviceId() {
    let id = localStorage.getItem('hb_device_id');
    if (!id) { id = 'device-' + crypto.randomUUID(); localStorage.setItem('hb_device_id', id); }
    return id;
  }
  async function add(action) {
    const user = normalizeUser(currentUser ? currentUser() : {});
    const record = {
      clientActionId: action.clientActionId || crypto.randomUUID(),
      actionType: action.actionType,
      entityType: action.entityType || 'PRODUCT',
      entityId: action.entityId == null ? null : String(action.entityId),
      username: user.username || 'offline-user',
      roleName: user.role || 'UNKNOWN',
      offlineCreatedAt: action.offlineCreatedAt || new Date().toISOString().substring(0,19),
      payload: action.payload || {},
      status: 'PENDING',
      createdAt: new Date().toISOString()
    };
    await HBOfflineDB.put(record);
    await HBOfflineUI.updatePendingBadge();
    return record;
  }
  return { add, deviceId };
})();
