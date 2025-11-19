document.addEventListener('DOMContentLoaded', () => {

    // --- CONFIG ---
    const API_BASE_URL = 'http://127.0.0.1:5001/api';
    const TOKEN_KEY = 'smart_inventory_token';
    const USER_KEY = 'smart_inventory_user';

    // --- CHECK IF ALREADY LOGGED IN ---
    if (localStorage.getItem(TOKEN_KEY)) {
        window.location.href = 'dashboard.html';
    }

    // --- TAB/VIEW SELECTORS ---
    const showLoginTab = document.getElementById('show-login-tab');
    const showRegisterTab = document.getElementById('show-register-tab');
    const loginView = document.getElementById('login-view');
    const registerView = document.getElementById('register-view');

    // --- LOGIN FORM SELECTORS ---
    const loginForm = document.getElementById('login-form');
    const loginSubmitBtn = document.getElementById('login-submit-btn');
    const loginMessage = document.getElementById('login-message');
    const loginUsernameInput = document.getElementById('login-username');
    const loginPasswordInput = document.getElementById('login-password');
    
    // --- REGISTER FORM SELECTORS ---
    const registerForm = document.getElementById('register-form');
    const registerSubmitBtn = document.getElementById('register-submit-btn');
    const registerMessage = document.getElementById('register-message');
    const regFullnameInput = document.getElementById('reg-fullname');
    const regUsernameInput = document.getElementById('reg-username');
    const regEmailInput = document.getElementById('reg-email');
    const regPasswordInput = document.getElementById('reg-password');

    // --- EVENT LISTENERS ---

    // Tab Switching
    showLoginTab.addEventListener('click', () => {
        loginView.style.display = 'block';
        registerView.style.display = 'none';
        showLoginTab.classList.add('active');
        showRegisterTab.classList.remove('active');
    });

    showRegisterTab.addEventListener('click', () => {
        loginView.style.display = 'none';
        registerView.style.display = 'block';
        showLoginTab.classList.remove('active');
        showRegisterTab.classList.add('active');
    });

    // Form Handlers
    loginForm.addEventListener('submit', handleLogin);
    registerForm.addEventListener('submit', handleRegister);


    // --- LOGIN FUNCTION ---
    async function handleLogin(e) {
        e.preventDefault(); // <-- STOPS THE REFRESH
        loginMessage.style.display = 'none';
        loginSubmitBtn.disabled = true;
        loginSubmitBtn.textContent = 'Signing in...';

        const data = {
            username: loginUsernameInput.value,
            password: loginPasswordInput.value
        };
        
        try {
            const response = await fetch(`${API_BASE_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data),
            });

            const result = await response.json();

            if (response.ok) {
                // SUCCESS
                localStorage.setItem(TOKEN_KEY, result.access_token);
                localStorage.setItem(USER_KEY, JSON.stringify(result.user));
                window.location.href = 'dashboard.html';
            } else {
                // FAIL
                showApiMessage(loginMessage, result.message || 'Invalid username or password.', 'error');
            }
        } catch (error) {
            showApiMessage(loginMessage, `Network error: ${error.message}`, 'error');
        } finally {
            loginSubmitBtn.disabled = false;
            loginSubmitBtn.textContent = 'Sign In';
        }
    }

    // --- REGISTER FUNCTION ---
    async function handleRegister(e) {
        e.preventDefault(); // <-- STOPS THE REFRESH
        registerMessage.style.display = 'none';
        registerSubmitBtn.disabled = true;
        registerSubmitBtn.textContent = 'Creating Account...';

        const data = {
            full_name: regFullnameInput.value,
            username: regUsernameInput.value,
            email: regEmailInput.value,
            password: regPasswordInput.value
        };
        
        try {
            const response = await fetch(`${API_BASE_URL}/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data),
            });

            const result = await response.json();

            if (response.ok) {
                // SUCCESS
                showApiMessage(registerMessage, 'Success! Please log in.', 'success');
                // Switch back to login tab
                showLoginTab.click();
                // Clear register form
                registerForm.reset();
            } else {
                // FAIL
                showApiMessage(registerMessage, result.message || 'An error occurred.', 'error');
            }
        } catch (error) {
            showApiMessage(registerMessage, `Network error: ${error.message}`, 'error');
        } finally {
            registerSubmitBtn.disabled = false;
            registerSubmitBtn.textContent = 'Create Account';
        }
    }

    // --- HELPER FUNCTION ---
    function showApiMessage(element, message, type) {
        element.textContent = message;
        element.className = type === 'success' ? 'alert alert-success' : 'alert alert-error';
        element.style.display = 'block';
    }
});