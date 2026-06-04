// Login demo: backend trả role, frontend điều hướng đúng dashboard.
document.getElementById('login-btn').onclick = async () => {
  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value;
  const msg = document.getElementById('login-msg');
  try {
    const user = await apiPost('/auth/login', { username, password });
    localStorage.setItem('hb_user', JSON.stringify(user));
    const role = user.ROLE || user.role;
    if (role === 'FARMER') location.href = 'farm-management.html';
    else if (role === 'TRANSPORTER') location.href = 'transport.html';
    else if (role === 'STORE') location.href = 'store.html';
    else location.href = 'admin.html';
  } catch (e) {
    msg.textContent = e.message;
  }
};
