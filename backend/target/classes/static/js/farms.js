const user = normalizeUser(currentUser());
let excelRows = [];
let stores = [];
let transporters = [];

function val(row, ...names) {
  for (const n of names) if (row[n] !== undefined && row[n] !== null && String(row[n]).trim() !== '') return row[n];
  return '';
}
function n(v) { return v === '' || v == null ? null : Number(v); }
function isoDate(v) {
  if (!v) return null;
  if (typeof v === 'number' && window.XLSX) return XLSX.SSF.format('yyyy-mm-dd', v);
  return String(v).substring(0, 10);
}
function formObj(form) {
  const fd = new FormData(form);
  const o = Object.fromEntries(fd.entries());
  ['farmId','storeId','transporterId'].forEach(k => o[k] = n(o[k]));
  ['price','quantity','requiredTempMin','requiredTempMax','requiredHumidityMin','requiredHumidityMax'].forEach(k => o[k] = n(o[k]));
  ['sowingDate','harvestDate','expiredDate','certificateIssueDate','certificateExpiredDate'].forEach(k => o[k] = o[k] || null);
  return o;
}
function toast(msg, ok=true){ const el=document.getElementById('farm-msg'); el.textContent=msg; el.style.color=ok?'#1b7f3a':'#b42318'; }

async function imageUrlToBase64(url){
  if(!url || String(url).startsWith('data:')) return null;
  try{
    const res=await fetch(url);
    if(!res.ok) return null;
    const blob=await res.blob();
    if(!blob.type.startsWith('image/')) return null;
    if(blob.size > 900*1024) return null;
    const dataUrl=await new Promise((resolve,reject)=>{ const r=new FileReader(); r.onload=()=>resolve(r.result); r.onerror=reject; r.readAsDataURL(blob); });
    const parts=String(dataUrl).split(',');
    return {base64:parts[1]||'', mime:blob.type||'image/png'};
  }catch{ return null; }
}
async function enrichImageFromExcel(req){
  if(req.productImageBase64) return req;
  const img = await imageUrlToBase64(req.imageUrl);
  if(img){ req.productImageBase64=img.base64; req.productImageMime=img.mime; }
  return req;
}

function bindProductImageUpload(){
  const input=document.getElementById('product-image-file');
  const b64=document.getElementById('productImageBase64');
  const mime=document.getElementById('productImageMime');
  const preview=document.getElementById('product-image-preview');
  if(!input) return;
  input.addEventListener('change', async ()=>{
    const file=input.files && input.files[0];
    if(!file){ b64.value=''; mime.value=''; preview.textContent='Chưa chọn ảnh.'; return; }
    if(!file.type.startsWith('image/')){ toast('File ảnh không hợp lệ.', false); input.value=''; return; }
    if(file.size > 900*1024){ toast('Ảnh quá lớn. Hãy chọn ảnh dưới 900KB để lưu DB ổn định.', false); input.value=''; return; }
    const dataUrl=await new Promise((resolve,reject)=>{ const r=new FileReader(); r.onload=()=>resolve(r.result); r.onerror=reject; r.readAsDataURL(file); });
    const parts=String(dataUrl).split(',');
    b64.value=parts[1]||''; mime.value=file.type||'image/jpeg';
    preview.innerHTML=`<img src="${dataUrl}" alt="preview" style="width:150px;height:120px;object-fit:cover;border-radius:18px;border:1px solid var(--hb-border);vertical-align:middle;margin-right:10px"> Đã chọn: ${escapeHtml(file.name)} (${Math.round(file.size/1024)}KB)`;
  });
}

async function loadMasters(){
  stores = await apiGet('/system/stores');
  transporters = await apiGet('/system/transporters');
  const storeSel = document.getElementById('store-select');
  const transSel = document.getElementById('transporter-select');
  storeSel.innerHTML = stores.map(s=>`<option value="${s.STORE_ID}">${escapeHtml(s.STORE_NAME)} - ${escapeHtml(s.ADDRESS)}</option>`).join('');
  transSel.innerHTML = '<option value="">Chưa phân công</option>' + transporters.map(t=>`<option value="${t.TRANSPORTER_ID}">${escapeHtml(t.TRANSPORTER_NAME)}</option>`).join('');
}
function batchCard(p){
  const canConfirm = ['CREATED','FARM_CONFIRMED'].includes(p.STATUS);
  return `<article class="hb-batch-card">
    <div class="hb-batch-image-wrap"><img class="hb-batch-image" src="${escapeHtml(productImageSrc(p))}" alt="product" onerror="this.src='assets/default.svg'"></div>
    <div class="hb-batch-body">
      <h3 class="hb-batch-title">${escapeHtml(p.BATCH_CODE || p.PRODUCT_ID)} - ${escapeHtml(p.PRODUCT_NAME)}</h3>
      <p class="hb-batch-meta"><b>${escapeHtml(p.CATEGORY)}</b> · ${p.QUANTITY||0} ${escapeHtml(p.UNIT||'')} · Store: <b>${escapeHtml(p.STORE_NAME||'Chưa chọn')}</b></p>
      <p class="hb-batch-meta">Trạng thái: <b>${escapeHtml(p.STATUS)}</b> · QR: ${escapeHtml(p.QR_TOKEN||'')}</p>
      <div class="hb-batch-route"><div class="hb-route-box"><span>Điểm lấy</span>${escapeHtml(p.PICKUP_LOCATION||p.FARM_ADDRESS_MASTER||'Chưa có')}</div><div class="hb-route-box"><span>Điểm giao</span>${escapeHtml(p.DELIVERY_LOCATION||p.STORE_ADDRESS||'Chưa có')}</div></div>
      <div class="hb-batch-actions">${canConfirm?`<button class="hb-btn" onclick="confirmBatch(${p.PRODUCT_ID})">✅ Xác nhận chờ lấy hàng</button>`:''}<a class="hb-btn ghost" href="product-detail.html?token=${encodeURIComponent(p.QR_TOKEN||'')}&sig=${encodeURIComponent(p.QR_SIGNATURE||'')}">Trace</a></div>
    </div>
  </article>`;
}
async function loadProducts(){
  const farmId = user.farmId || document.getElementById('farmId').value || 1;
  const rows = await apiGet(`/system/farm/products?farmId=${farmId}`);
  const box = document.getElementById('farm-products');
  box.classList.add('hb-enterprise-list');
  if (!rows.length) { box.innerHTML = '<p class="hb-empty">Chưa có lô hàng.</p>'; return; }
  box.innerHTML = rows.map(batchCard).join('');
}
async function confirmBatch(id){
  try { await apiPost(`/system/farm/products/${id}/ready-for-transport`, {userId:user.userId, note:'Farm xác nhận đủ thông tin và chờ lấy hàng.'}); toast('Đã chuyển lô sang WAITING_FOR_PICKUP.'); await loadProducts(); }
  catch(e){ toast(e.message,false); }
}
window.confirmBatch = confirmBatch;

function toRequest(row){
  const storeId = n(val(row,'storeId','STORE_ID','Store ID','Mã store')) || (stores[0] && stores[0].STORE_ID);
  const transporterId = n(val(row,'transporterId','TRANSPORTER_ID','Transporter ID','Mã vận chuyển')) || null;
  const imageUrl = val(row,'imageUrl','IMAGE_URL','Ảnh','Ảnh sản phẩm','productImageUrl') || '';
  let rawB64 = val(row,'productImageBase64','PRODUCT_IMAGE_B64','imageBase64','Ảnh base64') || '';
  let imageMime = val(row,'productImageMime','PRODUCT_IMAGE_MIME','imageMime','Kiểu ảnh') || '';
  if(String(rawB64).startsWith('data:')){
    const m=String(rawB64).match(/^data:([^;]+);base64,(.*)$/);
    if(m){ imageMime=m[1]; rawB64=m[2]; }
  }
  return {
    farmId: user.farmId || 1,
    productName: String(val(row,'productName','Tên sản phẩm','Tên lô hàng')).trim(),
    category: String(val(row,'category','Loại','Loại sản phẩm')).trim(),
    price: n(val(row,'price','Giá')), quantity: n(val(row,'quantity','Số lượng')) || 100, unit: val(row,'unit','Đơn vị') || 'kg',
    description: val(row,'description','Mô tả'), imageUrl, productImageBase64:String(rawB64||'').trim(), productImageMime:String(imageMime||'').trim(),
    cultivationPlace: val(row,'cultivationPlace','Nơi trồng'), farmAddress: val(row,'farmAddress','Địa chỉ farm'),
    sowingDate: isoDate(val(row,'sowingDate','Ngày gieo trồng')), harvestDate: isoDate(val(row,'harvestDate','Ngày thu hoạch')), expiredDate: isoDate(val(row,'expiredDate','Hạn sử dụng')),
    productionProcess: val(row,'productionProcess','Quy trình sản xuất'),
    storeId, transporterId, pickupLocation: val(row,'pickupLocation','Điểm lấy hàng'), deliveryLocation: val(row,'deliveryLocation','Điểm giao hàng'),
    requiredTempMin: n(val(row,'requiredTempMin','Nhiệt độ min')) || 6, requiredTempMax: n(val(row,'requiredTempMax','Nhiệt độ max')) || 10,
    requiredHumidityMin: n(val(row,'requiredHumidityMin','Độ ẩm min')) || 70, requiredHumidityMax: n(val(row,'requiredHumidityMax','Độ ẩm max')) || 90,
    transportNote: val(row,'transportNote','Ghi chú vận chuyển'), certificateName: val(row,'certificateName','Chứng chỉ'), certificateIssuer: val(row,'certificateIssuer','Đơn vị cấp'), certificateIssueDate: isoDate(val(row,'certificateIssueDate','Ngày cấp chứng chỉ')), certificateExpiredDate: isoDate(val(row,'certificateExpiredDate','Ngày hết hạn chứng chỉ')), certificateFileUrl: val(row,'certificateFileUrl','File chứng chỉ')
  };
}
function excelImgSrc(r){ if(r.productImageBase64) return `data:${r.productImageMime||'image/jpeg'};base64,${r.productImageBase64}`; return r.imageUrl || 'assets/default.svg'; }
function isValidDateString(v){ return !v || /^\d{4}-\d{2}-\d{2}$/.test(String(v)); }
function validateReq(r){
  const errors=[];
  if(!r.productName || String(r.productName).trim().length < 3) errors.push('Tên lô tối thiểu 3 ký tự');
  if(!r.category || String(r.category).trim().length < 2) errors.push('Thiếu loại sản phẩm');
  if(!r.storeId || Number(r.storeId) <= 0) errors.push('Thiếu storeId hợp lệ');
  if(r.transporterId != null && Number(r.transporterId) <= 0) errors.push('transporterId không hợp lệ');
  if(r.price != null && Number(r.price) < 0) errors.push('Giá không được âm');
  if(r.quantity == null || Number(r.quantity) <= 0) errors.push('Số lượng phải > 0');
  if(!r.unit || String(r.unit).trim().length === 0) errors.push('Thiếu đơn vị tính');
  if(!r.pickupLocation) errors.push('Thiếu điểm lấy hàng');
  if(!r.deliveryLocation) errors.push('Thiếu điểm giao hàng');
  if(!isValidDateString(r.sowingDate)) errors.push('Ngày gieo trồng sai định dạng yyyy-mm-dd');
  if(!isValidDateString(r.harvestDate)) errors.push('Ngày thu hoạch sai định dạng yyyy-mm-dd');
  if(!isValidDateString(r.expiredDate)) errors.push('Hạn sử dụng sai định dạng yyyy-mm-dd');
  if(r.sowingDate && r.harvestDate && r.harvestDate < r.sowingDate) errors.push('Ngày thu hoạch phải sau ngày gieo trồng');
  if(r.harvestDate && r.expiredDate && r.expiredDate < r.harvestDate) errors.push('Hạn sử dụng phải sau ngày thu hoạch');
  if(r.requiredTempMin != null && r.requiredTempMax != null && Number(r.requiredTempMin) > Number(r.requiredTempMax)) errors.push('Nhiệt độ min > max');
  if(r.requiredHumidityMin != null && r.requiredHumidityMax != null && Number(r.requiredHumidityMin) > Number(r.requiredHumidityMax)) errors.push('Độ ẩm min > max');
  if(r.certificateIssueDate && r.certificateExpiredDate && r.certificateExpiredDate < r.certificateIssueDate) errors.push('Ngày hết hạn chứng chỉ phải sau ngày cấp');
  if(r.productImageBase64 && String(r.productImageBase64).length > 1200000) errors.push('Ảnh base64 quá lớn, nên dưới 900KB');
  return errors;
}
function importStatusBadge(errors){ return errors.length ? `<span class="hb-badge danger">Lỗi</span>` : `<span class="hb-badge safe">Hợp lệ</span>`; }
function renderExcelPreview(){
  const box=document.getElementById('excel-preview-box');
  const valid=excelRows.filter(r=>!validateReq(r).length).length;
  const invalid=excelRows.length-valid;
  box.style.display='block';
  document.getElementById('excel-summary').innerHTML=`Đọc <b>${excelRows.length}</b> dòng. Hợp lệ: <b style="color:#1b7f3a">${valid}</b>. Lỗi: <b style="color:#b42318">${invalid}</b>. Ảnh Excel hỗ trợ cột <b>imageUrl</b> hoặc <b>productImageBase64</b>; khi xác nhận, ảnh imageUrl sẽ được đọc và lưu base64 vào Oracle nếu truy cập được.`;
  document.getElementById('excel-preview').innerHTML=`
    <div style="overflow:auto;max-height:440px;border:1px solid var(--hb-border);border-radius:18px">
      <table class="hb-table">
        <tr><th>#</th><th>Ảnh</th><th>Trạng thái</th><th>Tên lô</th><th>Loại</th><th>SL</th><th>Đơn vị</th><th>Store</th><th>Transport</th><th>Thu hoạch</th><th>HSD</th><th>Lỗi/ràng buộc</th></tr>
        ${excelRows.map((r,i)=>{ const errs=validateReq(r); return `<tr><td>${i+1}</td><td><img class="hb-import-thumb" src="${escapeHtml(excelImgSrc(r))}" onerror="this.src='assets/default.svg'"></td><td>${importStatusBadge(errs)}</td><td>${escapeHtml(r.productName)}</td><td>${escapeHtml(r.category)}</td><td>${r.quantity??''}</td><td>${escapeHtml(r.unit||'')}</td><td>${r.storeId??''}</td><td>${r.transporterId??''}</td><td>${escapeHtml(r.harvestDate||'')}</td><td>${escapeHtml(r.expiredDate||'')}</td><td>${errs.length?escapeHtml(errs.join('; ')):'OK'}</td></tr>`}).join('')}
      </table>
    </div>`;
}
function renderImportResult(successes, failures){
  const ids = successes.map(x => `#${x.productId || x.PRODUCT_ID || ''}${x.batchCode ? ' - '+x.batchCode : ''}`).join(', ');
  const failHtml = failures.length ? `<details style="margin-top:10px"><summary>Chi tiết lỗi (${failures.length})</summary><ul>${failures.map(f=>`<li>Dòng ${f.row}: ${escapeHtml(f.error)}</li>`).join('')}</ul></details>` : '';
  document.getElementById('excel-import-result').innerHTML = `<div class="hb-card" style="padding:16px;margin-top:14px;background:#fffdf6"><b>Kết quả import</b><p>Thành công: <b style="color:#1b7f3a">${successes.length}</b> | Thất bại: <b style="color:#b42318">${failures.length}</b></p><p><b>ID lô mới tạo:</b> ${ids || 'Không có'}</p>${failHtml}</div>`;
}

async function init(){
  bindProductImageUpload();
  document.getElementById('farmId').value = user.farmId || 1;
  await loadMasters(); await loadProducts();
  document.getElementById('batch-form').addEventListener('submit', async e => { e.preventDefault(); try { await apiPost('/system/farm/products', formObj(e.target)); toast('Tạo lô thành công. Có thể xác nhận chờ lấy hàng.'); e.target.reset(); document.getElementById('farmId').value=user.farmId||1; document.getElementById('productImageBase64').value=''; document.getElementById('productImageMime').value=''; document.getElementById('product-image-preview').textContent='Chưa chọn ảnh.'; await loadProducts(); } catch(err){ toast(err.message,false); }});
  document.getElementById('download-excel-template').onclick=()=>{
    const headers=['productName','category','price','quantity','unit','description','imageUrl','productImageBase64','productImageMime','cultivationPlace','farmAddress','sowingDate','harvestDate','expiredDate','productionProcess','storeId','transporterId','pickupLocation','deliveryLocation','requiredTempMin','requiredTempMax','requiredHumidityMin','requiredHumidityMax','transportNote','certificateName','certificateIssuer','certificateIssueDate','certificateExpiredDate','certificateFileUrl'];
    const rows=[
      ['Excel Demo Green Mustard','Leafy Vegetable',30000,100,'kg','Batch imported from Excel','assets/veg.svg','','','Greenhouse A1','Da Lat Farm','2026-06-01','2026-06-03','2026-06-10','Organic care and cold packing',1,1,'Da Lat Farm - Dock A','Honey Mart Quan 1',6,10,70,90,'Keep cold 6-10C','VietGAP Demo','HoneyBee QC','2026-01-01','2026-12-31','docs/cert-demo.pdf'],
      ['Excel Demo Mango','Fruit',55000,80,'kg','Mango imported from Excel','assets/mango.svg','','','Mango Zone B','Cai Be Farm','2026-02-20','2026-06-03','2026-06-13','Bagged fruit and cold sorting',1,1,'Cai Be Farm - Packing','Honey Mart Quan 1',10,15,65,85,'Do not stack over 5 boxes','GlobalGAP Mango','HoneyBee QC','2026-01-15','2026-12-31','docs/cert-mango.pdf']
    ];
    const ws=XLSX.utils.aoa_to_sheet([headers,...rows]);
    const wb=XLSX.utils.book_new(); XLSX.utils.book_append_sheet(wb, ws, 'FarmImport');
    XLSX.writeFile(wb, 'honeybee_farm_import_template_with_images.xlsx');
  };
  document.getElementById('excel-file').onchange=async e=>{
    const file=e.target.files[0];
    if(!file) return;
    try{
      const data=await file.arrayBuffer();
      const wb=XLSX.read(data,{type:'array'});
      const sheet=wb.Sheets[wb.SheetNames[0]];
      const raw=XLSX.utils.sheet_to_json(sheet,{defval:''});
      excelRows=raw.map(toRequest);
      renderExcelPreview();
      document.getElementById('excel-import-result').innerHTML='';
    }catch(err){ toast('Không đọc được Excel: '+err.message,false); }
  };
  document.getElementById('confirm-excel-import').onclick=async()=>{
    const successes=[]; const failures=[];
    for(let i=0;i<excelRows.length;i++){
      const r={...excelRows[i]};
      const errs=validateReq(r);
      if(errs.length){ failures.push({row:i+1,error:errs.join('; ')}); continue; }
      try{
        await enrichImageFromExcel(r);
        const created=await apiPost('/system/farm/products', r);
        successes.push({row:i+1, productId:created.productId, batchCode:r.batchCode || ('HB-BATCH-'+created.productId)});
      }catch(err){ failures.push({row:i+1,error:err.message || 'API error'}); }
    }
    renderImportResult(successes, failures);
    toast(`Import xong: ${successes.length} sản phẩm thành công, ${failures.length} thất bại.`, failures.length===0);
    await loadProducts();
  };
}
init().catch(e=>toast(e.message,false));
