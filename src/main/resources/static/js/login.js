document.addEventListener('DOMContentLoaded', () => {
    if (getLoggedInUser()) {
        window.location.href = getDefaultPageForRole(getLoggedInUser().role);
        return;
    }

    document.getElementById('loginForm').addEventListener('submit', handleLogin);
    document.querySelectorAll('.sample-pill').forEach(pill => {
        pill.addEventListener('click', () => {
            document.getElementById('loginEmail').value = pill.dataset.email;
            document.getElementById('loginPassword').value = pill.dataset.password;
        });
    });

    renderGuestQr();
});

async function handleLogin(event) {
    event.preventDefault();
    const payload = {
        email: document.getElementById('loginEmail').value.trim(),
        password: document.getElementById('loginPassword').value.trim()
    };

    try {
        const data = await apiRequest('/api/auth/login', {
            method: 'POST',
            headers: buildHeaders(),
            body: JSON.stringify(payload),
            loadingText: 'Signing you in...'
        });

        setLoggedInUser({ ...data.user, token: data.token });
        showMessage(data.message, 'success', 'loginMessage');
        setTimeout(() => {
            window.location.href = getDefaultPageForRole(data.user.role);
        }, 800);
    } catch (error) {
        showMessage(error.message, 'danger', 'loginMessage');
    }
}

function renderGuestQr() {
    const host = document.getElementById('guestQrCode');
    const linkLabel = document.getElementById('guestQrLink');
    if (!host || !window.QRCode) {
        return;
    }

    const qrUrl = `${window.location.origin}/guest-menu?table=T-04`;
    linkLabel.textContent = qrUrl;
    host.innerHTML = '';
    new QRCode(host, {
        text: qrUrl,
        width: 164,
        height: 164,
        colorDark: '#0d0d18',
        colorLight: '#f5ecd6',
        correctLevel: QRCode.CorrectLevel.H
    });
}
