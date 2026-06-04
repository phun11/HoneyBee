const user = normalizeUser(currentUser());
function msg(t, ok=true){ const e=document.getElementById('sale-message'); if(e){e.textContent=t; e.style.color=ok?'#1b7f3a':'#b42318';}}
async function receive(id, status='STORE_RECEIVED'){
  const note = status==='REJECTED' ? prompt('Nhập lý do từ chối/trả hàng:', 'Hàng không đạt điều kiện nhận.') : 'Store xác nhận nhận hàng.';
  if (status==='REJECTED' && !note) return;
  try{ await apiPost(status==='REJECTED'?'/system/store/reject':'/system/store/receive', {productId:id, storeId:user.storeId||1, userId:user.userId, quantityReceived:null, status, note, rejectReason: status==='REJECTED'?note:null}); msg('Đã cập nhật trạng thái lô.'); await loadStore(); }catch(e){ msg(e.message,false); }
}
window.receive=receive;
function fillQr(token){ document.querySelector('[name="qrToken"]').value=token; }
window.fillQr=fillQr;
function storeCard(p){
  return `<article class="hb-batch-card">
    <div class="hb-batch-image-wrap"><img class="hb-batch-image" src="${escapeHtml(productImageSrc(p))}" alt="product" onerror="this.src='assets/default.svg'"></div>
    <div class="hb-batch-body">
      <h3 class="hb-batch-title">${escapeHtml(p.BATCH_CODE||p.PRODUCT_ID)} - ${escapeHtml(p.PRODUCT_NAME)}</h3>
      <p class="hb-batch-meta">Farm: <b>${escapeHtml(p.FARM_NAME||'')}</b> → Store: <b>${escapeHtml(p.STORE_NAME||'')}</b></p>
      <p class="hb-batch-meta">Trạng thái lô: <b>${escapeHtml(p.PRODUCT_STATUS)}</b> · QR sale: <b>${escapeHtml(p.SALE_STATUS||'')}</b> · ${p.TRANSPORT_STEP_COUNT||0} chặng vận chuyển</p>
      <div class="hb-batch-route"><div class="hb-route-box"><span>Điểm lấy</span>${escapeHtml(p.PICKUP_LOCATION||p.FARM_ADDRESS||'')}</div><div class="hb-route-box"><span>Điểm giao</span>${escapeHtml(p.DELIVERY_LOCATION||p.STORE_ADDRESS||'')}</div></div>
      <p class="hb-batch-meta">QR: ${escapeHtml(p.QR_TOKEN||'')}</p>
      <div class="hb-batch-actions">${p.PRODUCT_STATUS==='DELIVERED'?`<button class="hb-btn" onclick="receive(${p.PRODUCT_ID}, 'STORE_RECEIVED')">✅ Xác nhận nhận</button>`:''}${['DELIVERED','STORE_RECEIVED'].includes(p.PRODUCT_STATUS)?`<button class="hb-btn ghost" onclick="receive(${p.PRODUCT_ID}, 'AVAILABLE_FOR_SALE')">🛒 Sẵn sàng bán</button><button class="hb-btn ghost" onclick="receive(${p.PRODUCT_ID}, 'REJECTED')">⚠️ Từ chối/Trả hàng</button>`:''}<button class="hb-btn ghost" onclick="fillQr('${escapeHtml(p.QR_TOKEN||'')}')">Điền QR</button><a class="hb-btn ghost" href="product-detail.html?token=${encodeURIComponent(p.QR_TOKEN||'')}">Trace</a></div>
    </div>
  </article>`;
}
async function loadStore(){
  const sid=user.storeId||''; const rows=await apiGet(`/system/store/products${sid?`?storeId=${sid}`:''}`);
  const box=document.getElementById('store-list');
  box.classList.add('hb-enterprise-list');
  if(!rows.length){ box.innerHTML='<p class="hb-empty">Chưa có lô hàng giao đến store.</p>'; return; }
  box.innerHTML=rows.map(storeCard).join('');
}
document.getElementById('sale-form')?.addEventListener('submit', async e=>{ e.preventDefault(); const fd=new FormData(e.target); const body=Object.fromEntries(fd.entries()); body.storeId=user.storeId||Number(body.storeId)||1; body.userId=user.userId||Number(body.userId)||4; try{ await apiPost('/system/store/sale-status', body); msg('Đã lưu trạng thái QR/POS.'); await loadStore(); }catch(err){ msg(err.message,false); }});
document.getElementById('refresh-btn')?.addEventListener('click', loadStore);
const sf=document.getElementById('sale-form'); if(sf){ sf.storeId.value=user.storeId||1; sf.userId.value=user.userId||4; }
loadStore().catch(e=>msg(e.message,false));
