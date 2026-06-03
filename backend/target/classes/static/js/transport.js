const user = normalizeUser(currentUser());
let selected = null;
function toast(msg, ok=true){ const el=document.getElementById('route-msg'); if(el){ el.textContent=msg; el.style.color=ok?'#1b7f3a':'#b42318'; } }
function statusGroup(s){
  if (s==='WAITING_FOR_PICKUP') return 'Chờ lấy hàng';
  if (['PICKED_UP','IN_TRANSIT','ARRIVED_AT_HUB','OUT_FOR_DELIVERY'].includes(s)) return 'Đang vận chuyển';
  if (s==='DELIVERED') return 'Đã giao - chờ Store';
  return 'Có sự cố/hoàn tất';
}
function selectShipment(p){
  selected = p;
  const f=document.getElementById('route-form');
  f.productId.value=p.PRODUCT_ID; f.batchLabel.value=`${p.BATCH_CODE||p.PRODUCT_ID} - ${p.PRODUCT_NAME}`;
  f.fromLocation.value=p.CURRENT_LOCATION || p.PICKUP_LOCATION || p.FARM_ADDRESS || '';
  f.toLocation.value=p.DELIVERY_LOCATION || p.STORE_ADDRESS || '';
  f.currentLocation.value=''; f.storageTemperature.value=p.REQUIRED_TEMP_MIN || ''; f.humidity.value=p.REQUIRED_HUMIDITY_MIN || ''; f.note.value=''; f.issueNote.value='';
  toast('Đã chọn lô ' + (p.BATCH_CODE||p.PRODUCT_ID));
}
window.selectShipment=selectShipment;
async function pickup(id){
  const p = allShipments.find(x=>Number(x.PRODUCT_ID)===Number(id));
  try{ await apiPost(`/system/transport/shipments/${id}/pickup`, {transporterId:user.transporterId||p.TRANSPORTER_ID||1, userId:user.userId, fromLocation:p.PICKUP_LOCATION||p.FARM_ADDRESS, toLocation:p.DELIVERY_LOCATION||p.STORE_ADDRESS, currentLocation:p.PICKUP_LOCATION||p.FARM_ADDRESS, storageTemperature:p.REQUIRED_TEMP_MIN, humidity:p.REQUIRED_HUMIDITY_MIN, sealStatus:'INTACT', note:'Đã nhận hàng tại điểm lấy.'}); toast('Nhận hàng thành công.'); await load(); }catch(e){ toast(e.message,false); }
}
window.pickup=pickup;
async function deliver(id){
  const p = allShipments.find(x=>Number(x.PRODUCT_ID)===Number(id));
  try{ await apiPost(`/system/transport/shipments/${id}/deliver`, {transporterId:user.transporterId||p.TRANSPORTER_ID||1, userId:user.userId, fromLocation:p.CURRENT_LOCATION||p.PICKUP_LOCATION, toLocation:p.DELIVERY_LOCATION||p.STORE_ADDRESS, currentLocation:p.DELIVERY_LOCATION||p.STORE_ADDRESS, storageTemperature:p.REQUIRED_TEMP_MIN, humidity:p.REQUIRED_HUMIDITY_MIN, sealStatus:'INTACT', note:'Đã giao đến Store.'}); toast('Đã giao hàng. Store có thể xác nhận nhận hàng.'); await load(); }catch(e){ toast(e.message,false); }
}
window.deliver=deliver;
let allShipments=[];
function shipmentCard(p){
  return `<article class="hb-batch-card">
    <div class="hb-batch-image-wrap"><img class="hb-batch-image" src="${escapeHtml(productImageSrc(p))}" alt="product" onerror="this.src='assets/default.svg'"></div>
    <div class="hb-batch-body">
      <h3 class="hb-batch-title">${escapeHtml(p.BATCH_CODE||p.PRODUCT_ID)} - ${escapeHtml(p.PRODUCT_NAME)}</h3>
      <p class="hb-batch-meta"><b>${escapeHtml(p.FARM_NAME)}</b> → <b>${escapeHtml(p.STORE_NAME||'Store')}</b></p>
      <div class="hb-batch-route"><div class="hb-route-box"><span>Lấy hàng</span>${escapeHtml(p.PICKUP_LOCATION||p.FARM_ADDRESS||'')}</div><div class="hb-route-box"><span>Giao hàng</span>${escapeHtml(p.DELIVERY_LOCATION||p.STORE_ADDRESS||'')}</div></div>
      <p class="hb-batch-meta">Yêu cầu: <b>${p.REQUIRED_TEMP_MIN??''}-${p.REQUIRED_TEMP_MAX??''}°C</b> · <b>${p.REQUIRED_HUMIDITY_MIN??''}-${p.REQUIRED_HUMIDITY_MAX??''}%</b> · Trạng thái: <b>${escapeHtml(p.STATUS)}</b></p>
      <p class="hb-batch-meta">Ghi chú: ${escapeHtml(p.TRANSPORT_NOTE||'Không có')}</p>
      <p class="hb-batch-meta">QR: ${escapeHtml(p.QR_TOKEN||'')}</p>
      <div class="hb-batch-actions"><button class="hb-btn ghost" onclick="selectShipment(${JSON.stringify(p).replace(/"/g,'&quot;')})">Chọn</button>${p.STATUS==='WAITING_FOR_PICKUP'?`<button class="hb-btn" onclick="pickup(${p.PRODUCT_ID})">📦 Nhận hàng</button>`:''}${['PICKED_UP','IN_TRANSIT','ARRIVED_AT_HUB','OUT_FOR_DELIVERY'].includes(p.STATUS)?`<button class="hb-btn" onclick="deliver(${p.PRODUCT_ID})">✅ Đã giao</button>`:''}<a class="hb-btn ghost" href="product-detail.html?token=${encodeURIComponent(p.QR_TOKEN||'')}&sig=${encodeURIComponent(p.QR_SIGNATURE||'')}">Trace</a></div>
    </div>
  </article>`;
}
async function load(){
  const tid=user.transporterId||'';
  allShipments=await apiGet(`/system/transport/shipments/pending${tid?`?transporterId=${tid}`:''}`);
  const grouped={}; allShipments.forEach(p=>{ (grouped[statusGroup(p.STATUS)] ||= []).push(p); });
  document.getElementById('shipments').classList.add('hb-enterprise-list');
  document.getElementById('shipments').innerHTML=Object.entries(grouped).map(([g,rows])=>`<h3 style="margin:18px 0 6px;color:var(--hb-brown)">${g}</h3>`+rows.map(shipmentCard).join('')).join('') || '<p class="hb-empty">Không có lô cần xử lý.</p>';
  const hist=await apiGet(`/system/transport/shipments/history${tid?`?transporterId=${tid}`:''}`);
  document.getElementById('transport-history').innerHTML=hist.slice(0,30).map(h=>`<div class="hb-history-row"><b>${escapeHtml(h.BATCH_CODE||h.PRODUCT_ID)} - ${escapeHtml(h.STATUS)}</b><p>${escapeHtml(h.FROM_LOCATION||'')} → ${escapeHtml(h.TO_LOCATION||'')}</p><p>Vị trí: ${escapeHtml(h.CURRENT_LOCATION||'')} | ${h.STORAGE_TEMPERATURE??''}°C | ${h.HUMIDITY??''}% | Seal: ${escapeHtml(h.SEAL_STATUS||'')}</p><small>${fmtDate(h.TRANSPORT_TIME)} - ${escapeHtml(h.NOTE||h.ISSUE_NOTE||'')}</small></div>`).join('') || '<p class="hb-note">Chưa có lịch sử.</p>';
}

document.getElementById('reload-transport')?.addEventListener('click', load);
document.getElementById('route-form')?.addEventListener('submit', async e=>{
  e.preventDefault(); const f=e.target; if(!f.productId.value) return toast('Chưa chọn lô.',false);
  const body={transporterId:user.transporterId||1,userId:user.userId,fromLocation:f.fromLocation.value,toLocation:f.toLocation.value,currentLocation:f.currentLocation.value,storageTemperature:f.storageTemperature.value?Number(f.storageTemperature.value):null,humidity:f.humidity.value?Number(f.humidity.value):null,sealStatus:f.sealStatus.value,status:f.status.value,note:f.note.value,issueNote:f.issueNote.value};
  try{ await apiPost(`/system/transport/shipments/${f.productId.value}/route`, body); toast('Đã lưu chặng mới.'); await load(); }catch(err){ toast(err.message,false); }
});
document.getElementById('issue-btn')?.addEventListener('click', async ()=>{
  const f=document.getElementById('route-form'); if(!f.productId.value) return toast('Chưa chọn lô.',false);
  try{ await apiPost(`/system/transport/shipments/${f.productId.value}/issue`, {transporterId:user.transporterId||1,userId:user.userId,fromLocation:f.fromLocation.value,toLocation:f.toLocation.value,currentLocation:f.currentLocation.value,storageTemperature:f.storageTemperature.value?Number(f.storageTemperature.value):null,humidity:f.humidity.value?Number(f.humidity.value):null,sealStatus:f.sealStatus.value,note:f.note.value,issueNote:f.issueNote.value||'Transport báo sự cố/trả hàng.'}); toast('Đã báo sự cố.'); await load(); }catch(e){ toast(e.message,false); }
});
load().catch(e=>toast(e.message,false));
