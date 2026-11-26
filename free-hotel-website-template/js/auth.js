/**
 * ============================================
 *  AUTH.JS — Harbor Lights Hotel
 *  UI helpers only.
 *
 *  Authentication (login / register / forgot-password) and the navbar
 *  logged-in state are now handled server-side by Spring MVC. This file no
 *  longer performs any AJAX or form interception; it only enhances the forms
 *  with password visibility toggles, a strength meter, numeric code input and
 *  the user-menu dropdown.
 * ============================================
 */

(function () {
  'use strict';

  // ──────────────── HELPERS ────────────────

  /** Password strength checker */
  function checkPasswordStrength(password) {
    var score = 0;
    if (password.length >= 8) score++;
    if (password.length >= 12) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;

    if (score <= 1) return { level: 'weak', text: 'Weak', cls: 'weak' };
    if (score === 2) return { level: 'fair', text: 'Fair', cls: 'fair' };
    if (score === 3) return { level: 'good', text: 'Good', cls: 'good' };
    return { level: 'strong', text: 'Strong', cls: 'strong' };
  }

  /** Update strength meter UI */
  function updateStrengthMeter(password, barId, textId) {
    var bar = document.getElementById(barId);
    var text = document.getElementById(textId);
    if (!bar || !text) return;

    if (!password) {
      bar.className = 'strength-bar';
      text.textContent = '';
      return;
    }

    var result = checkPasswordStrength(password);
    bar.className = 'strength-bar ' + result.cls;
    text.textContent = result.text;
  }

  /** Toggle password visibility */
  function initTogglePassword(toggleBtnId, inputId) {
    var btn = document.getElementById(toggleBtnId);
    var input = document.getElementById(inputId);
    if (!btn || !input) return;

    btn.addEventListener('click', function () {
      var isPassword = input.type === 'password';
      input.type = isPassword ? 'text' : 'password';

      var eyeOpen = btn.querySelector('.eye-open');
      var eyeClosed = btn.querySelector('.eye-closed');
      if (eyeOpen && eyeClosed) {
        eyeOpen.style.display = isPassword ? 'none' : 'block';
        eyeClosed.style.display = isPassword ? 'block' : 'none';
      }
    });
  }

  // ──────────────── WIRE UP UI ────────────────

  function init() {
    // Login page
    initTogglePassword('toggle-login-password', 'login-password');

    // Register page
    initTogglePassword('toggle-register-password', 'register-password');
    var regPasswordInput = document.getElementById('register-password');
    if (regPasswordInput) {
      regPasswordInput.addEventListener('input', function () {
        updateStrengthMeter(this.value, 'register-strength-bar', 'register-strength-text');
      });
    }

    // Forgot-password page (step 2: numeric code, step 3: new password toggle + meter)
    var codeInput = document.getElementById('forgot-code');
    if (codeInput) {
      codeInput.addEventListener('input', function () {
        this.value = this.value.replace(/[^0-9]/g, '');
      });
    }
    initTogglePassword('toggle-new-password', 'new-password');
    var newPasswordInput = document.getElementById('new-password');
    if (newPasswordInput) {
      newPasswordInput.addEventListener('input', function () {
        updateStrengthMeter(this.value, 'new-password-strength-bar', 'new-password-strength-text');
      });
    }

    // Navbar user-menu dropdown (rendered server-side when logged in)
    var trigger = document.getElementById('nav-user-trigger');
    var menu = document.getElementById('nav-user-menu');
    if (trigger && menu) {
      trigger.addEventListener('click', function (e) {
        e.stopPropagation();
        menu.classList.toggle('active');
      });
      document.addEventListener('click', function () {
        menu.classList.remove('active');
      });
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
  // Re-run when the navbar is injected client-side on the untouched static pages.
  document.addEventListener('navbarLoaded', init);

})();
