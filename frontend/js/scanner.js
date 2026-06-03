// ============================================================
// Scanner page
// ------------------------------------------------------------
// Hỗ trợ 3 cách quét:
// 1) Mở camera trực tiếp bằng getUserMedia.
// 2) Chọn/chụp ảnh QR từ máy hoặc điện thoại.
// 3) Nhập token thủ công hoặc dùng token demo.
//
// Ưu tiên jsQR nếu tải được từ CDN. Nếu CDN lỗi, Chrome/Edge mới có thể
// dùng BarcodeDetector để đọc QR trực tiếp. Camera chỉ hoạt động trên
// localhost hoặc HTTPS theo chính sách trình duyệt.
// ============================================================
const fileInput = document.getElementById('qr-file');
const msg = document.getElementById('scan-message');
const video = document.getElementById('qr-video');
const canvas = document.getElementById('qr-canvas');
const cameraBox = document.getElementById('camera-box');
const startCameraBtn = document.getElementById('start-camera');
const stopCameraBtn = document.getElementById('stop-camera');
let cameraStream = null;
let cameraLoopId = null;
let redirected = false;
let barcodeDetector = null;
let cameraBusy = false;

try {
  if ('BarcodeDetector' in window) {
    barcodeDetector = new BarcodeDetector({ formats: ['qr_code'] });
  }
} catch (_) {
  barcodeDetector = null;
}

function setMsg(text) {
  if (msg) msg.textContent = text || '';
}

function isSecureCameraContext() {
  return window.isSecureContext || ['localhost', '127.0.0.1', '::1'].includes(location.hostname);
}

function goToQrValue(value) {
  if (!value || redirected) return;
  redirected = true;
  stopCamera();
  const raw = String(value).trim();
  if (raw.includes('product-detail.html') || /^https?:\/\//i.test(raw)) {
    try {
      const u = new URL(raw, location.href);
      if (u.hostname === 'localhost' || u.hostname === '127.0.0.1') {
        u.hostname = location.hostname;
        u.port = location.port || u.port;
      }
      location.href = u.toString();
    } catch (_) {
      location.href = raw;
    }
  } else {
    location.href = `product-detail.html?token=${encodeURIComponent(raw)}`;
  }
}

function drawToCanvas(source, width, height) {
  const ctx = canvas.getContext('2d', { willReadFrequently: true });
  canvas.width = width;
  canvas.height = height;
  ctx.drawImage(source, 0, 0, width, height);
  return ctx.getImageData(0, 0, width, height);
}

async function decodeFrame(source, width, height) {
  if (!source || !width || !height) return null;

  // jsQR đọc tốt QR trong canvas và không cần quyền đặc biệt.
  if (typeof jsQR === 'function') {
    const imageData = drawToCanvas(source, width, height);
    const code = jsQR(imageData.data, imageData.width, imageData.height, { inversionAttempts: 'attemptBoth' });
    return code?.data || null;
  }

  // Fallback khi CDN jsQR không tải được.
  if (barcodeDetector) {
    try {
      const codes = await barcodeDetector.detect(source);
      return codes?.[0]?.rawValue || null;
    } catch (_) {
      const imageData = drawToCanvas(source, width, height);
      const codes = await barcodeDetector.detect(canvas);
      return codes?.[0]?.rawValue || null;
    }
  }

  setMsg('Không có thư viện đọc QR. Kiểm tra Internet để tải jsQR hoặc dùng Chrome/Edge mới.');
  return null;
}

async function scanCameraFrame() {
  if (!cameraStream || redirected || cameraBusy) return;
  cameraBusy = true;
  try {
    if (video.readyState >= HTMLMediaElement.HAVE_CURRENT_DATA && video.videoWidth > 0) {
      const data = await decodeFrame(video, video.videoWidth, video.videoHeight);
      if (data) {
        setMsg('Đã đọc được QR. Đang mở trang truy xuất...');
        goToQrValue(data);
        return;
      }
    }
  } finally {
    cameraBusy = false;
  }
  if (cameraStream && !redirected) cameraLoopId = requestAnimationFrame(scanCameraFrame);
}

async function startCamera() {
  try {
    redirected = false;
    if (!navigator.mediaDevices?.getUserMedia) {
      setMsg('Trình duyệt không hỗ trợ mở camera. Hãy dùng Chrome/Edge hoặc chọn/chụp ảnh QR.');
      return;
    }
    if (!isSecureCameraContext()) {
      setMsg('Camera bị trình duyệt chặn vì đang chạy qua HTTP IP. Hãy mở bằng http://localhost:8080/scanner.html trên laptop, hoặc dùng HTTPS/ngrok nếu quét bằng điện thoại.');
      return;
    }
    setMsg('Đang xin quyền mở camera...');
    cameraStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } },
      audio: false
    });
    video.srcObject = cameraStream;
    cameraBox.style.display = 'block';
    startCameraBtn.style.display = 'none';
    stopCameraBtn.style.display = 'inline-flex';
    await video.play();
    setMsg('Đưa mã QR vào giữa khung camera. Nếu bị mờ, thử đưa camera ra xa hơn một chút.');
    scanCameraFrame();
  } catch (err) {
    setMsg(`Không mở được camera: ${err.message}. Hãy cấp quyền camera hoặc dùng nút chọn/chụp ảnh QR.`);
    stopCamera();
  }
}

function stopCamera() {
  if (cameraLoopId) cancelAnimationFrame(cameraLoopId);
  cameraLoopId = null;
  cameraBusy = false;
  if (cameraStream) {
    cameraStream.getTracks().forEach(track => track.stop());
    cameraStream = null;
  }
  if (video) video.srcObject = null;
  if (cameraBox) cameraBox.style.display = 'none';
  if (startCameraBtn) startCameraBtn.style.display = 'inline-flex';
  if (stopCameraBtn) stopCameraBtn.style.display = 'none';
}

startCameraBtn?.addEventListener('click', startCamera);
stopCameraBtn?.addEventListener('click', () => {
  stopCamera();
  setMsg('Đã tắt camera.');
});

document.getElementById('mock-safe').onclick = () => {
  location.href = 'product-detail.html?token=HB-QR-RAU-001-SAFE&sig=DEMO_SIGNATURE_SAFE';
};
document.getElementById('mock-warn').onclick = () => {
  location.href = 'product-detail.html?token=HB-QR-XOAI-002-WARN&sig=DEMO_SIGNATURE_WARN';
};
document.getElementById('manual-go').onclick = () => {
  const token = document.getElementById('manual-token').value.trim();
  if (!token) return setMsg('Vui lòng nhập token.');
  goToQrValue(token);
};

fileInput?.addEventListener('change', async (e) => {
  const file = e.target.files?.[0];
  if (!file) return;
  redirected = false;
  setMsg('Đang đọc ảnh QR...');
  const img = new Image();
  img.onload = async () => {
    try {
      const data = await decodeFrame(img, img.naturalWidth || img.width, img.naturalHeight || img.height);
      if (!data) {
        setMsg('Không đọc được QR. Hãy chụp rõ hơn, tránh lóa sáng, hoặc dùng nhập token thủ công.');
        return;
      }
      goToQrValue(data);
    } finally {
      URL.revokeObjectURL(img.src);
    }
  };
  img.onerror = () => setMsg('Không mở được file ảnh QR.');
  img.src = URL.createObjectURL(file);
});

window.addEventListener('beforeunload', stopCamera);
