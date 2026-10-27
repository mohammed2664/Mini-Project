/**
 * RentSphere: Local P2P Rental & Smart Pickup Hub Application Engine
 * Goa University - MCA Object-Oriented Technology Lab
 * Currency: Indian Rupees (₹ INR) | Local Community Scale
 */

const API_BASE = '/api';

// Application State Store
const state = {
    activeUser: null,
    activeCategory: 'ALL',
    users: [],
    assets: [],
    lockers: [],
    wal: [],
    disputes: [],
    tickets: [],
    policies: [],
    notifications: [],
    selectedBayId: 'A2',
    currentReserveAsset: null,
    pendingPaymentOrder: null,
    authToken: null
};

// ============================================================================
// CURRENCY FORMATTER (INDIAN RUPEES ₹ INR)
// ============================================================================
function formatInr(val) {
    const num = Number(val) || 0;
    return '₹' + num.toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function formatInrCompact(val) {
    const num = Number(val) || 0;
    return '₹' + num.toLocaleString('en-IN', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    });
}

// ============================================================================
// INITIALIZATION ON DOM LOAD
// ============================================================================
document.addEventListener('DOMContentLoaded', async () => {
    initNavigation();
    initCategoryPills();
    initUserSelector();
    initNotifications();
    initAuthModals();
    initPolicies();
    initJourneyTimeline();
    initEventListeners();

    // Check and restore existing server session using /api/auth/me
    await restoreSession();
    await fetchAllData();

    // Auto-refresh data every 8 seconds to reflect background flat-file state
    setInterval(refreshBackgroundData, 8000);
});

async function restoreSession() {
    try {
        const res = await fetch(`${API_BASE}/auth/me`);
        if (!res.ok) {
            state.activeUser = null;
            return;
        }
        const data = await res.json();
        if (data && data.authenticated && data.user) {
            state.activeUser = data.user.userId;
            const select = document.getElementById('activeUserSelect');
            if (select) {
                let opt = Array.from(select.options).find(o => o.value === data.user.userId);
                if (!opt) {
                    opt = new Option(data.user.fullName + ` (${data.user.role})`, data.user.userId);
                    select.add(opt);
                }
                select.value = data.user.userId;
            }
        } else {
            state.activeUser = null;
        }
    } catch (e) {
        console.warn('Session restoration failed:', e);
        state.activeUser = null;
    }
}

async function executeLogout() {
    try {
        await fetch(`${API_BASE}/auth/logout`, {
            method: 'POST'
        });
    } catch (e) {
        console.warn('Logout request failed:', e);
    }
    state.activeUser = null;
    state.authToken = null;
    const select = document.getElementById('activeUserSelect');
    if (select) {
        let guestOpt = Array.from(select.options).find(o => o.value === '');
        if (!guestOpt) {
            guestOpt = new Option('-- Guest (Not Signed In) --', '');
            select.add(guestOpt, 0);
        }
        select.value = '';
    }
    updateUserUI();
    showToast('🚪 Signed out successfully. You are now in Guest mode.', 'info');
    await fetchAllData();
}

function initNavigation() {
    const navPills = document.querySelectorAll('.nav-pill');
    navPills.forEach(pill => {
        pill.addEventListener('click', () => {
            navPills.forEach(p => p.classList.remove('active'));
            pill.classList.add('active');

            const viewId = pill.getAttribute('data-view');
            document.querySelectorAll('.view-section').forEach(sec => sec.classList.remove('active'));
            const targetView = document.getElementById(viewId);
            if (targetView) targetView.classList.add('active');

            if (viewId === 'hubView') renderLockerGrid();
            if (viewId === 'escrowView') renderQuarantineDeck();
            if (viewId === 'hostView') {
                populateHostDropdowns();
                runTrustScoreCalculation();
                runLiveReflection();
            }
            if (viewId === 'adminSupportView') {
                renderAdminSupportView();
            }
        });
    });
}

function initCategoryPills() {
    const pills = document.querySelectorAll('.cat-pill');
    pills.forEach(pill => {
        pill.addEventListener('click', () => {
            pills.forEach(p => p.classList.remove('active'));
            pill.classList.add('active');
            state.activeCategory = pill.getAttribute('data-category');
            renderCatalog();
        });
    });
}

function initUserSelector() {
    const select = document.getElementById('activeUserSelect');
    if (!select) return;
    select.addEventListener('change', async (e) => {
        const val = e.target.value;
        if (!val) {
            await executeLogout();
            return;
        }
        state.activeUser = val;
        await executeLogin(state.activeUser, 'RentSphere@2026');
        updateUserUI();
        fetchNotifications();
    });
}

function initEventListeners() {
    // Search & State Filters
    document.getElementById('searchInput')?.addEventListener('input', renderCatalog);
    document.getElementById('stateFilter')?.addEventListener('change', renderCatalog);

    // Modals
    document.getElementById('openListEquipmentBtn')?.addEventListener('click', () => openModal('onboardModal'));
    document.getElementById('closeOnboardModal')?.addEventListener('click', () => closeModal('onboardModal'));
    document.getElementById('closeReserveModal')?.addEventListener('click', () => closeModal('reserveModal'));
    document.getElementById('closePaymentSandboxModal')?.addEventListener('click', () => closeModal('paymentSandboxModal'));
    document.getElementById('closeDisputeModal')?.addEventListener('click', () => closeModal('disputeModal'));
    document.getElementById('closePolicyModal')?.addEventListener('click', () => closeModal('policyModal'));
    document.getElementById('closeSupportModal')?.addEventListener('click', () => closeModal('supportModal'));
    document.getElementById('closeAuthModal')?.addEventListener('click', () => closeModal('authModal'));

    // Top CTA & Policy triggers
    document.getElementById('openPolicyCenterBtn')?.addEventListener('click', openPolicyCenter);
    document.getElementById('heroLearnPoliciesBtn')?.addEventListener('click', openPolicyCenter);
    document.getElementById('heroExploreBtn')?.addEventListener('click', () => {
        document.getElementById('assetsGrid')?.scrollIntoView({ behavior: 'smooth' });
    });
    document.getElementById('heroBecomeHostBtn')?.addEventListener('click', () => openModal('onboardModal'));
    document.getElementById('openAuthModalBtn')?.addEventListener('click', () => openModal('authModal'));
    document.getElementById('signOutBtn')?.addEventListener('click', executeLogout);
    document.getElementById('openSupportTicketBtn')?.addEventListener('click', () => openModal('supportModal'));

    // Footer Policy Link Triggers
    document.querySelectorAll('.policy-link-trigger').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const policyKey = link.getAttribute('data-policy');
            openPolicyCenter(policyKey);
        });
    });

    // Form Submissions
    document.getElementById('confirmOnboardBtn')?.addEventListener('click', submitOnboardAsset);
    document.getElementById('confirmReserveBtn')?.addEventListener('click', submitReserveAsset);
    document.getElementById('rentalDaysInput')?.addEventListener('input', updateReserveEstimates);
    document.getElementById('authorizeSandboxPaymentBtn')?.addEventListener('click', authorizeSandboxPayment);
    document.getElementById('submitDisputeBtn')?.addEventListener('click', submitDamageDispute);
    document.getElementById('submitTicketBtn')?.addEventListener('click', submitSupportTicket);
    document.getElementById('savePolicyConsentBtn')?.addEventListener('click', recordPolicyConsent);

    // Hub & Locker Buttons
    document.getElementById('refreshGridBtn')?.addEventListener('click', () => {
        fetchLockers().then(() => showToast('Smart Hub sensor grid refreshed.', 'success'));
    });
    document.getElementById('pickupFromLockerBtn')?.addEventListener('click', pickupSelectedBayGear);
    document.getElementById('returnToQuarantineBtn')?.addEventListener('click', returnSelectedBayGear);
    document.getElementById('inspectorApplyStateBtn')?.addEventListener('click', applyInspectorDeviceState);

    // Admin & Support Buttons
    document.getElementById('refreshUsersBtn')?.addEventListener('click', fetchAdminUsers);
    document.getElementById('applyDeviceStateBtn')?.addEventListener('click', applySimulatedDeviceState);

    // WAL Audit Ledger Refresh
    document.getElementById('refreshWalBtn')?.addEventListener('click', () => {
        fetchWal().then(() => showToast('Audit Ledger updated from flat file.', 'info'));
    });

    // Host Studio Cloner & Diagnostics
    document.getElementById('executeCloneListingBtn')?.addEventListener('click', submitCloneListing);
    document.getElementById('runMetaBenchmarkBtn')?.addEventListener('click', runLiveReflection);
    document.getElementById('metaAssetSelect')?.addEventListener('change', runLiveReflection);
    document.getElementById('cloneSourceSelect')?.addEventListener('change', updateClonerMemoryPreview);

    // Trust Score Recalculate
    document.getElementById('recalcTrustBtn')?.addEventListener('click', runTrustScoreCalculation);
    ['sliderCond', 'sliderPunct', 'sliderEscrow', 'sliderPeer', 'sliderKyc'].forEach(id => {
        document.getElementById(id)?.addEventListener('input', runTrustScoreCalculation);
    });
}

// ============================================================================
// DATA FETCHING & SYNCHRONIZATION
// ============================================================================
async function fetchAllData() {
    await Promise.all([
        fetchAccounts(),
        fetchAssets(),
        fetchLockers(),
        fetchWal(),
        fetchHealth(),
        fetchNotifications()
    ]);
}

async function refreshBackgroundData() {
    await Promise.all([
        fetchAssets(true),
        fetchLockers(true),
        fetchAccounts(true),
        fetchWal(true),
        fetchHealth(),
        fetchNotifications()
    ]);
}

async function fetchHealth() {
    try {
        const res = await fetch(`${API_BASE}/health`);
        if (!res.ok) return;
        const data = await res.json();
        document.getElementById('kpiTotalAssets').textContent = data.totalAssets;
        document.getElementById('kpiLockedEscrow').textContent = Number(data.lockedEscrowCapital).toLocaleString('en-IN', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
        document.getElementById('kpiActiveLeases').textContent = data.activeLeases;
        document.getElementById('kpiInQuarantine').textContent = data.inQuarantine;
    } catch (e) {
        console.error('Health fetch error:', e);
    }
}

async function fetchAccounts(silent = false) {
    try {
        const res = await fetch(`${API_BASE}/escrow/accounts`);
        if (!res.ok) return;
        state.users = await res.json();
        updateUserUI();
    } catch (e) {
        console.error('Accounts fetch error:', e);
    }
}

async function fetchAssets(silent = false) {
    try {
        const res = await fetch(`${API_BASE}/assets`);
        if (!res.ok) return;
        state.assets = await res.json();
        renderCatalog();
        populateHostDropdowns();
        renderQuarantineDeck();
        renderRenterActiveBookings();
    } catch (e) {
        console.error('Assets fetch error:', e);
    }
}

async function fetchLockers(silent = false) {
    try {
        const res = await fetch(`${API_BASE}/lockers`);
        if (!res.ok) return;
        state.lockers = await res.json();
        renderLockerGrid();
        updateInspectorDetails();
        renderRenterActiveBookings();
    } catch (e) {
        console.error('Lockers fetch error:', e);
    }
}

async function fetchWal(silent = false) {
    try {
        const res = await fetch(`${API_BASE}/wal`);
        if (!res.ok) return;
        state.wal = await res.json();
        renderWalTable();
    } catch (e) {
        console.error('WAL fetch error:', e);
    }
}

// ============================================================================
// USER UI & IDENTITY MANAGEMENT
// ============================================================================
function updateUserUI() {
    const signOutBtn = document.getElementById('signOutBtn');
    const badge = document.getElementById('userRoleBadge');
    const walletSpan = document.getElementById('headerWalletBalance');
    const authBtn = document.getElementById('authButtonLabel');
    const navHostTab = document.getElementById('navHostTab');
    const navAdminTab = document.getElementById('navAdminTab');
    const listEquipBtn = document.getElementById('openListEquipmentBtn');

    if (!state.activeUser) {
        if (badge) {
            badge.textContent = 'GUEST';
            badge.className = 'role-tag';
        }
        if (walletSpan) walletSpan.textContent = '0.00';
        if (authBtn) authBtn.textContent = 'Sign In / Register';
        if (signOutBtn) signOutBtn.style.display = 'none';
        if (navHostTab) navHostTab.style.display = 'none';
        if (navAdminTab) navAdminTab.style.display = 'none';
        if (listEquipBtn) listEquipBtn.style.display = 'none';
        renderRenterActiveBookings();
        return;
    }

    const user = state.users.find(u => u.userId === state.activeUser);
    const role = user ? user.role : 'RENTER';
    const userName = user ? user.userName : state.activeUser;
    const balance = user ? Number(user.walletBalance) : 0;

    if (walletSpan) {
        walletSpan.textContent = balance.toLocaleString('en-IN', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
    }
    if (badge) {
        badge.textContent = role;
        badge.className = 'role-tag role-' + role.toLowerCase();
    }
    if (authBtn) {
        authBtn.textContent = userName.split(' ')[0] + ' (' + role + ')';
    }
    if (signOutBtn) {
        signOutBtn.style.display = 'inline-flex';
    }

    // Role-based navigation and action visibility (Requirement 2)
    const isAdminUser = role === 'ADMIN' || role === 'INSPECTOR' || (user && user.userId === 'inspector_almeida');
    if (isAdminUser) {
        if (navHostTab) navHostTab.style.display = 'inline-flex';
        if (navAdminTab) navAdminTab.style.display = 'inline-flex';
        if (listEquipBtn) listEquipBtn.style.display = 'inline-flex';
    } else if (role === 'HOST') {
        if (navHostTab) navHostTab.style.display = 'inline-flex';
        if (navAdminTab) navAdminTab.style.display = 'none';
        if (listEquipBtn) listEquipBtn.style.display = 'inline-flex';
    } else { // RENTER
        if (navHostTab) navHostTab.style.display = 'none';
        if (navAdminTab) navAdminTab.style.display = 'none';
        if (listEquipBtn) listEquipBtn.style.display = 'none';
    }

    // Redirect to marketplace if currently viewing a role-restricted tab
    const activeNav = document.querySelector('.nav-pill.active');
    if (activeNav) {
        const currentView = activeNav.getAttribute('data-view');
        if ((currentView === 'hostView' && role === 'RENTER') || 
            (currentView === 'adminSupportView' && !isAdminUser)) {
            const marketplaceTab = document.querySelector('.nav-pill[data-view="marketplaceView"]');
            if (marketplaceTab) marketplaceTab.click();
        }
    }

    renderRenterActiveBookings();
    updateInspectorDetails();
}

function initJourneyTimeline() {
    const steps = [
        { id: 'journeyStep1', view: 'marketplaceView', scrollId: 'assetsGrid' },
        { id: 'journeyStep2', view: 'marketplaceView', scrollId: 'assetsGrid' },
        { id: 'journeyStep3', view: 'hubView', scrollId: null },
        { id: 'journeyStep4', view: 'marketplaceView', scrollId: 'renterActiveBookingsContainer' },
        { id: 'journeyStep5', view: 'escrowView', scrollId: null }
    ];

    steps.forEach(({ id, view, scrollId }) => {
        const el = document.getElementById(id);
        if (!el) return;
        el.addEventListener('click', () => {
            const targetNav = document.querySelector(`.nav-pill[data-view="${view}"]`);
            if (targetNav) targetNav.click();
            if (scrollId) {
                setTimeout(() => {
                    document.getElementById(scrollId)?.scrollIntoView({ behavior: 'smooth' });
                }, 100);
            }
        });
    });
}

function updateJourneyTimeline() {
    const subnav = document.getElementById('renterJourneySubnav');
    if (!subnav) return;

    const myRentals = state.activeUser 
        ? state.assets.filter(a => a.renterId === state.activeUser && (a.state === 'RESERVED_ESCROW' || a.state === 'ACTIVE_LEASE' || a.state === 'INSPECTION_QUARANTINE'))
        : [];

    const reserved = myRentals.find(a => a.state === 'RESERVED_ESCROW');
    const activeLease = myRentals.find(a => a.state === 'ACTIVE_LEASE');
    const quarantine = myRentals.find(a => a.state === 'INSPECTION_QUARANTINE');

    // Reset steps
    for (let i = 1; i <= 5; i++) {
        const step = document.getElementById(`journeyStep${i}`);
        const circle = document.getElementById(`circleStep${i}`);
        const line = document.getElementById(`lineStep${i}`);
        if (step) {
            step.className = 'journey-step';
        }
        if (circle) {
            circle.textContent = i;
        }
        if (line) {
            line.className = 'step-line';
        }
    }

    if (quarantine) {
        // Stage 5 active
        for (let i = 1; i <= 4; i++) {
            document.getElementById(`journeyStep${i}`)?.classList.add('completed');
            const c = document.getElementById(`circleStep${i}`);
            if (c) c.textContent = '✓';
            document.getElementById(`lineStep${i}`)?.classList.add('completed');
        }
        document.getElementById('journeyStep5')?.classList.add('active');
        const desc = document.getElementById('descStep5');
        if (desc) desc.textContent = `${quarantine.title.split(' ')[0]} in Quarantine`;
    } else if (activeLease) {
        // Stage 4 active
        for (let i = 1; i <= 3; i++) {
            document.getElementById(`journeyStep${i}`)?.classList.add('completed');
            const c = document.getElementById(`circleStep${i}`);
            if (c) c.textContent = '✓';
            document.getElementById(`lineStep${i}`)?.classList.add('completed');
        }
        document.getElementById('journeyStep4')?.classList.add('active');
        const desc = document.getElementById('descStep4');
        if (desc) desc.textContent = `${activeLease.title.split(' ')[0]} (Bay ${activeLease.lockerCoordinate})`;
    } else if (reserved) {
        // Stage 3 active
        for (let i = 1; i <= 2; i++) {
            document.getElementById(`journeyStep${i}`)?.classList.add('completed');
            const c = document.getElementById(`circleStep${i}`);
            if (c) c.textContent = '✓';
            document.getElementById(`lineStep${i}`)?.classList.add('completed');
        }
        document.getElementById('journeyStep3')?.classList.add('active');
        const desc = document.getElementById('descStep3');
        if (desc) desc.textContent = `Bay ${reserved.lockerCoordinate} • PIN Ready`;
    } else {
        // Stage 1 active (Browse)
        document.getElementById('journeyStep1')?.classList.add('active');
    }
}

function renderRenterActiveBookings() {
    updateJourneyTimeline();

    const container = document.getElementById('renterActiveBookingsContainer');
    if (!container) return;

    if (!state.activeUser) {
        container.style.display = 'none';
        container.innerHTML = '';
        return;
    }

    const myRentals = state.assets.filter(a => a.renterId === state.activeUser && (a.state === 'RESERVED_ESCROW' || a.state === 'ACTIVE_LEASE'));
    if (myRentals.length === 0) {
        container.style.display = 'none';
        container.innerHTML = '';
        return;
    }

    container.style.display = 'block';
    container.innerHTML = `
        <div class="tactile-quick-dock">
            <div class="dock-header">
                <h3 class="dock-title">
                    <span>⚡</span> Active Rental Quick-Dock (${myRentals.length})
                </h3>
                <span class="dock-stage-badge">
                    ${myRentals.some(a => a.state === 'RESERVED_ESCROW') ? 'Stage 3 of 5: Smart Locker Pickup' : 'Stage 4 of 5: Active Lease in Field'}
                </span>
            </div>
            <div class="dock-grid">
                ${myRentals.map(a => {
                    const isReserved = a.state === 'RESERVED_ESCROW';
                    const comp = state.lockers.find(c => c.bayId === a.lockerCoordinate);
                    const hasOtp = comp && comp.otpPin && comp.otpPin.length === 6;
                    const formattedOtp = hasOtp ? comp.otpPin.split('').join(' ') : '• • • • • •';

                    return `
                        <div class="dock-card">
                            <div>
                                <div class="dock-card-top">
                                    <span class="cat-pill-badge" style="font-size:10px;">${getCategoryDisplayName(a.leafClass)}</span>
                                    <span class="state-pill ${isReserved ? 'state-reserved' : 'state-active'}">
                                        ${isReserved ? '🔒 RESERVED (Escrow Locked)' : '🚀 ACTIVE LEASE (In Use)'}
                                    </span>
                                </div>
                                <h4 style="font-family:var(--font-heading); font-size:15px; font-weight:700; margin:0 0 6px 0; color:var(--slate-900);">
                                    ${escapeHtml(a.title)}
                                </h4>
                                <div style="font-size:12px; color:var(--slate-600); margin-bottom:8px;">
                                    Smart Hub Bay: <strong style="color:var(--brand-teal);">${a.lockerCoordinate}</strong> • Host: <strong>${escapeHtml(a.ownerId)}</strong>
                                </div>
                                <div class="dock-meter-row">
                                    <span>Duration: <strong>${a.rentalDays || 1} day(s)</strong> (Tariff: ${formatInr(a.calculatedTariff * (a.rentalDays || 1))})</span>
                                    <span>Escrow Deposit: <strong>${formatInr(a.requiredCollateral)}</strong></span>
                                </div>
                            </div>

                            <div>
                                ${isReserved ? `
                                    <div class="dock-pin-vault">
                                        <div class="dock-pin-header">
                                            <span>One-Time Locker Access PIN</span>
                                            <span class="hardware-paired-badge">● Hardware Paired</span>
                                        </div>
                                        <div class="dock-pin-val">${escapeHtml(formattedOtp)}</div>
                                        <div style="font-size:10px; color:rgba(255,255,255,0.7); text-align:center;">
                                            Protected by 3-attempt brute-force lockout (CWE-307)
                                        </div>
                                    </div>
                                    <button class="btn-primary direct-pickup-btn" data-id="${a.id}" data-bay="${a.lockerCoordinate}" data-otp="${hasOtp ? escapeHtml(comp.otpPin) : ''}" style="width:100%; justify-content:center; padding:10px 14px; font-weight:700;">
                                        <span>🔓</span> 1-Click Remote Unlock Bay ${a.lockerCoordinate}
                                    </button>
                                ` : `
                                    <div style="background:var(--emerald-light); border:1px solid rgba(16,185,129,0.3); border-radius:var(--radius-sm); padding:10px 12px; margin-bottom:10px; font-size:11px; color:var(--emerald-dark); font-weight:600; display:flex; align-items:center; gap:6px;">
                                        <span>✓</span> Equipment active in field. Deposit guaranteed in Smart Escrow.
                                    </div>
                                    <button class="btn-danger direct-return-btn" data-id="${a.id}" data-bay="${a.lockerCoordinate}" style="width:100%; justify-content:center; padding:10px 14px; font-weight:700;">
                                        <span>📦</span> Return to Bay ${a.lockerCoordinate} (Enter Quarantine)
                                    </button>
                                `}
                            </div>
                        </div>
                    `;
                }).join('')}
            </div>
        </div>
    `;

    // Wire handlers for direct pickup
    container.querySelectorAll('.direct-pickup-btn').forEach(btn => {
        btn.addEventListener('click', async () => {
            const assetId = btn.getAttribute('data-id');
            const bayId = btn.getAttribute('data-bay');
            const prefilledOtp = btn.getAttribute('data-otp');
            let otp = prefilledOtp;
            if (!otp) {
                otp = prompt(`Enter 6-Digit Pickup OTP for Bay ${bayId}:`);
            }
            if (!otp || !otp.trim()) return;

            try {
                const res = await fetch(`${API_BASE}/assets/pickup`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ assetId, otpPin: otp.trim() })
                });
                const data = await res.json();
                if (data.success) {
                    showToast('🔓 ' + data.message, 'success');
                    await fetchAllData();
                } else {
                    showToast('❌ ' + (data.message || data.error), 'error');
                }
            } catch (e) {
                showToast('Pickup failed: ' + e.message, 'error');
            }
        });
    });

    // Wire handlers for direct return
    container.querySelectorAll('.direct-return-btn').forEach(btn => {
        btn.addEventListener('click', async () => {
            const assetId = btn.getAttribute('data-id');
            const bayId = btn.getAttribute('data-bay');
            if (!confirm(`Return equipment to Hub Bay ${bayId}? It will strictly enter Mandatory Quarantine for host condition inspection.`)) return;

            try {
                const res = await fetch(`${API_BASE}/assets/return`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ assetId })
                });
                const data = await res.json();
                if (data.success) {
                    showToast('📦 ' + data.message, 'success');
                    await fetchAllData();
                } else {
                    showToast('❌ ' + (data.message || data.error), 'error');
                }
            } catch (e) {
                showToast('Return failed: ' + e.message, 'error');
            }
        });
    });
}

function getActiveUserName() {
    const user = state.users.find(u => u.userId === state.activeUser);
    return user ? user.userName : state.activeUser;
}

// ============================================================================
// TAB 1: EXPLORE MARKETPLACE
// ============================================================================
function renderCatalog() {
    const container = document.getElementById('assetsGrid');
    if (!container) return;

    const searchTerm = (document.getElementById('searchInput')?.value || '').toLowerCase();
    const stateFilter = document.getElementById('stateFilter')?.value || 'ALL';

    const filtered = state.assets.filter(a => {
        // Category filter
        if (state.activeCategory !== 'ALL') {
            if (state.activeCategory === 'CAMERAS' && a.leafClass !== 'CameraAndOpticsGear') return false;
            if (state.activeCategory === 'AUDIO_LIGHTING' && a.leafClass !== 'EventAudioAndLighting') return false;
            if (state.activeCategory === 'POWER_TOOLS' && a.leafClass !== 'PowerToolEquipment') return false;
            if (state.activeCategory === 'MOBILITY_VEHICLES' && a.leafClass !== 'EventAndMobilityVehicle') return false;
        }

        // State filter
        if (stateFilter !== 'ALL' && a.state !== stateFilter) return false;

        // Search filter
        if (searchTerm) {
            const matchTitle = (a.title || '').toLowerCase().includes(searchTerm);
            const matchBay = (a.lockerCoordinate || '').toLowerCase().includes(searchTerm);
            const matchId = (a.id || '').toLowerCase().includes(searchTerm);
            if (!matchTitle && !matchBay && !matchId) return false;
        }

        return true;
    });

    if (filtered.length === 0) {
        container.innerHTML = `
            <div style="grid-column: 1/-1; text-align:center; padding: 60px 20px; background:#FFFFFF; border-radius:var(--radius-lg); border:1px solid var(--slate-200);">
                <div style="font-size:36px; margin-bottom:12px;">🔍</div>
                <h3 style="font-family:var(--font-heading); font-size:18px; font-weight:700;">No Equipment Matches Your Criteria</h3>
                <p style="font-size:13px; color:var(--slate-500); margin-top:4px;">Try selecting a different category or clearing the search query.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = filtered.map(a => renderAssetCard(a)).join('');

    // Attach Reserve Card Handlers
    container.querySelectorAll('.reserve-card-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const assetId = btn.getAttribute('data-id');
            openReserveModal(assetId);
        });
    });
}

function renderAssetCard(a) {
    const isAvail = a.state === 'AVAILABLE';
    const stateBadgeClass = getStateBadgeClass(a.state);
    const categoryName = getCategoryDisplayName(a.leafClass);

    // Visual Banner Theme by Category
    let bannerCatClass = 'cat-cameras';
    let bannerIcon = '📷';
    if (a.leafClass === 'EventAudioAndLighting') {
        bannerCatClass = 'cat-audio';
        bannerIcon = '🔊';
    } else if (a.leafClass === 'PowerToolEquipment') {
        bannerCatClass = 'cat-tools';
        bannerIcon = '🛠️';
    } else if (a.leafClass === 'EventAndMobilityVehicle') {
        bannerCatClass = 'cat-vehicles';
        bannerIcon = '🚗';
    }

    // Host identity & verified KYC status strictly from real backend data (state.users)
    const owner = state.users.find(u => u.userId === a.ownerId);
    const hostDisplayName = owner ? owner.userName : a.ownerId;
    const isVerifiedHost = Boolean(owner && owner.isKycVerified);

    // Real specification tags strictly from backend domain fields
    let specPills = [];
    if (a.indoorOutdoorRating) specPills.push(`<span class="spec-pill">${escapeHtml(a.indoorOutdoorRating)}</span>`);
    if (a.wattageOrBatterySpec) specPills.push(`<span class="spec-pill">${escapeHtml(a.wattageOrBatterySpec)}</span>`);
    if (a.powerOrEngineSpec) specPills.push(`<span class="spec-pill">${escapeHtml(a.powerOrEngineSpec)}</span>`);
    if (a.safetyCertification) specPills.push(`<span class="spec-pill is-safety"><span aria-hidden="true">✓</span> ${escapeHtml(a.safetyCertification)}</span>`);
    if (a.protectionTier) specPills.push(`<span class="spec-pill">${escapeHtml(a.protectionTier)}</span>`);
    if (a.requiresGroundBay) specPills.push(`<span class="spec-pill" style="border-color:var(--warm-amber); color:var(--warm-amber-dark); font-weight:700;">Ground Slot (Row H)</span>`);
    if (a.accessoryKitCount && a.accessoryKitCount > 0) specPills.push(`<span class="spec-pill">${a.accessoryKitCount} Kit Items</span>`);

    const hasLockerCoordinate = Boolean(a.lockerCoordinate && a.lockerCoordinate.trim());

    return `
        <article class="asset-card" aria-labelledby="asset-title-${escapeHtml(a.id)}">
            <div class="asset-banner ${bannerCatClass}" role="img" aria-label="${escapeHtml(a.title)} category visual">
                <div class="asset-banner-icon" aria-hidden="true">${bannerIcon}</div>
                ${hasLockerCoordinate ? `
                    <div class="asset-bay-badge" title="Physical Storage Location in 8x8 Smart Hub: Bay ${escapeHtml(a.lockerCoordinate)}" aria-label="Smart Locker Bay ${escapeHtml(a.lockerCoordinate)}">
                        <span aria-hidden="true">📍</span> Hub Bay ${escapeHtml(a.lockerCoordinate)}
                    </div>
                ` : ''}
                <span class="asset-state-tag ${stateBadgeClass}" aria-label="Availability Status: ${formatStateName(a.state)}">
                    ${formatStateName(a.state)}
                </span>
            </div>

            <div class="asset-card-content">
                <div>
                    <div class="asset-host-row">
                        <div style="display:flex; align-items:center; gap:6px;">
                            <span class="asset-id-chip" title="Asset Reference ID">${escapeHtml(a.id)}</span>
                            <span class="cat-pill-badge" style="font-size:10px;">${categoryName}</span>
                        </div>
                        ${owner ? (isVerifiedHost ? `
                            <span class="verified-host-tag" title="Identity & Bank Solvency Verified via 4-Gate Pipeline">
                                <span aria-hidden="true">✓</span> Verified Host: ${escapeHtml(hostDisplayName)}
                            </span>
                        ` : `
                            <span class="unverified-host-tag" title="KYC Verification Pending">
                                <span aria-hidden="true">⏳</span> Host: ${escapeHtml(hostDisplayName)}
                            </span>
                        `) : (a.ownerId ? `
                            <span class="unverified-host-tag">
                                Host: ${escapeHtml(a.ownerId)}
                            </span>
                        ` : '')}
                    </div>

                    <h3 class="asset-title" id="asset-title-${escapeHtml(a.id)}" style="margin-top:10px;">
                        ${escapeHtml(a.title)}
                    </h3>

                    ${specPills.length > 0 ? `
                        <div class="asset-specs-row" style="margin-top:8px;">
                            ${specPills.join('')}
                        </div>
                    ` : ''}
                </div>

                <div class="asset-pricing-bar">
                    <div>
                        <div class="tariff-display" aria-label="Daily Rental Fee: ${formatInr(a.calculatedTariff)} per day">
                            ${formatInr(a.calculatedTariff)} <span class="tariff-unit">/day</span>
                        </div>
                        <div class="deposit-display" title="Held safely in EscrowVault until clean host inspection" aria-label="Refundable Escrow Deposit: ${formatInr(a.requiredCollateral)}">
                            <span aria-hidden="true">🛡️</span> + ${formatInr(a.requiredCollateral)} Refundable Escrow
                        </div>
                    </div>

                    <div>
                        ${isAvail ? `
                            <button class="btn-primary reserve-card-btn" data-id="${a.id}" aria-label="Quick Reserve ${escapeHtml(a.title)} for ${formatInr(a.calculatedTariff)} per day" style="padding:9px 18px; border-radius:var(--radius-pill); font-size:12px; font-weight:700;">
                                <span aria-hidden="true">⚡</span> Quick Reserve
                            </button>
                        ` : `
                            <button class="btn-secondary" disabled aria-disabled="true" aria-label="${escapeHtml(a.title)} is currently ${formatStateName(a.state)}" style="padding:9px 18px; border-radius:var(--radius-pill); font-size:12px; font-weight:600; cursor:not-allowed;">
                                <span aria-hidden="true">🔒</span> ${formatStateName(a.state)}
                            </button>
                        `}
                    </div>
                </div>
            </div>
        </article>
    `;
}

function getStateBadgeClass(st) {
    if (st === 'AVAILABLE') return 'state-available';
    if (st === 'RESERVED_ESCROW') return 'state-reserved';
    if (st === 'ACTIVE_LEASE') return 'state-active';
    if (st === 'INSPECTION_QUARANTINE') return 'state-quarantine';
    if (st === 'DISPUTED') return 'state-disputed';
    return 'state-reserved';
}

function formatStateName(st) {
    if (st === 'AVAILABLE') return 'Available Now';
    if (st === 'RESERVED_ESCROW') return 'Reserved';
    if (st === 'ACTIVE_LEASE') return 'Active Lease';
    if (st === 'INSPECTION_QUARANTINE') return 'Quarantine';
    if (st === 'DISPUTED') return 'In Dispute';
    return st;
}

function getCategoryDisplayName(leafClass) {
    if (leafClass === 'CameraAndOpticsGear') return '📷 Cameras & Optics';
    if (leafClass === 'EventAudioAndLighting') return '🔊 Audio & Lighting';
    if (leafClass === 'PowerToolEquipment') return '🛠️ Power Tools';
    if (leafClass === 'EventAndMobilityVehicle') return '🚗 Mobility & Vehicles';
    return '📦 General Equipment';
}

// ============================================================================
// TAB 2: 8x8 SMART HUB & PARKING MATRIX (WARM HORIZON / STITCH VARIANT 2)
// ============================================================================
function getLockerCellDetails(c) {
    const isGround = c.bayId.startsWith('H') || c.row === 7 || c.sizeTier === 'XL_GROUND_SLOT';
    const asset = state.assets.find(a => (c.currentAssetId && a.id === c.currentAssetId) || a.lockerCoordinate === c.bayId);
    
    let stateClass = 'bay-available';
    let statusText = isGround ? 'BAY READY' : 'READY';
    let statusIcon = isGround ? '🚗' : '📦';

    if (asset && asset.state === 'DISPUTED') {
        stateClass = 'bay-disputed';
        statusText = 'DISPUTE';
        statusIcon = '⚖️';
    } else if (c.status === 'QUARANTINE_LOCKED' || (asset && asset.state === 'INSPECTION_QUARANTINE')) {
        stateClass = 'bay-quarantine';
        statusText = 'HOLD';
        statusIcon = '⚠️';
    } else if (c.status === 'DISPATCHED_ACTIVE' || (asset && asset.state === 'ACTIVE_LEASE')) {
        stateClass = 'bay-active';
        statusText = 'ACTIVE';
        statusIcon = '🚀';
    } else if (c.status === 'RESERVED_LOCKED' || (asset && asset.state === 'RESERVED_ESCROW')) {
        stateClass = 'bay-reserved';
        statusText = 'RESERVED';
        statusIcon = '🔒';
    }

    let tierShort = 'S';
    if (isGround) tierShort = 'XL';
    else if (c.sizeTier === 'LARGE') tierShort = 'L';
    else if (c.sizeTier === 'MEDIUM') tierShort = 'M';

    return {
        isGround,
        asset,
        stateClass,
        statusText,
        statusIcon,
        tierShort
    };
}

function renderLockerGrid() {
    const container = document.getElementById('grid8x8Container');
    if (!container) return;

    if (!state.lockers || state.lockers.length === 0) {
        container.innerHTML = '<div style="padding:24px; text-align:center; color:var(--slate-500); grid-column:1/-1;">Loading 64 Hub Compartments...</div>';
        return;
    }

    if (!state.selectedBayId && state.lockers.length > 0) {
        state.selectedBayId = state.lockers[0].bayId;
    }

    let html = '';
    state.lockers.forEach((c, index) => {
        // Visually separate Row H (Ground Vehicle & Heavy Cargo bays index 56-63)
        if (index === 56 || (c.bayId === 'H1' && !html.includes('ground-row-divider'))) {
            html += `<div class="ground-row-divider" role="separator" aria-label="Ground Vehicle & Heavy Cargo Bays">
                <span>🚗</span> ROW H: GROUND VEHICLE &amp; HEAVY CARGO BAYS (H1–H8) • DIRECT DRIVE-IN ACCESS
            </div>`;
        }

        const details = getLockerCellDetails(c);
        const isSelected = c.bayId === state.selectedBayId;
        const groundClass = details.isGround ? 'is-ground-slot' : '';
        const selectedClass = isSelected ? 'selected' : '';
        const sensorMini = (c.weightKg && c.weightKg > 0) 
            ? `${c.weightKg.toFixed(1)}kg` 
            : (c.tempC ? `${c.tempC.toFixed(0)}°C` : '');
        const assetLabel = details.asset ? ` • Asset: ${details.asset.title}` : '';

        html += `
            <div class="bay-cell ${details.stateClass} ${groundClass} ${selectedClass}"
                 data-bay="${escapeHtml(c.bayId)}"
                 data-row="${c.row}"
                 data-col="${c.col}"
                 tabindex="0"
                 role="button"
                 aria-label="Bay ${c.bayId}, Tier ${details.tierShort}, Status: ${details.statusText}${assetLabel}"
                 aria-current="${isSelected ? 'true' : 'false'}">
                <div class="bay-top-row">
                    <span class="bay-id">${escapeHtml(c.bayId)}</span>
                    <span class="bay-tier-tag">${details.tierShort}</span>
                </div>
                <div class="bay-center-icon">${details.statusIcon}</div>
                <div class="bay-bottom-row">
                    <span class="bay-state-text">${details.statusText}</span>
                    <span class="bay-sensor-mini">${sensorMini}</span>
                </div>
            </div>
        `;
    });

    container.innerHTML = html;

    container.querySelectorAll('.bay-cell').forEach(cell => {
        const bayId = cell.getAttribute('data-bay');
        const row = parseInt(cell.getAttribute('data-row'), 10);
        const col = parseInt(cell.getAttribute('data-col'), 10);

        cell.addEventListener('click', () => {
            selectBay(bayId);
        });

        cell.addEventListener('keydown', (e) => {
            handleBayKeydown(e, bayId, row, col);
        });
    });
}

function handleBayKeydown(e, bayId, row, col) {
    if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        selectBay(bayId);
        return;
    }

    let targetRow = row;
    let targetCol = col;

    if (e.key === 'ArrowUp') {
        targetRow = row - 1;
    } else if (e.key === 'ArrowDown') {
        targetRow = row + 1;
    } else if (e.key === 'ArrowLeft') {
        targetCol = col - 1;
    } else if (e.key === 'ArrowRight') {
        targetCol = col + 1;
    } else {
        return;
    }

    if (targetRow >= 0 && targetRow < 8 && targetCol >= 0 && targetCol < 8) {
        e.preventDefault();
        const targetCell = document.querySelector(`.bay-cell[data-row="${targetRow}"][data-col="${targetCol}"]`);
        if (targetCell) {
            targetCell.focus();
            const targetBay = targetCell.getAttribute('data-bay');
            if (targetBay) selectBay(targetBay);
        }
    }
}

function selectBay(bayId) {
    if (!bayId) return;
    state.selectedBayId = bayId;

    const container = document.getElementById('grid8x8Container');
    if (container) {
        container.querySelectorAll('.bay-cell').forEach(cell => {
            const isSel = cell.getAttribute('data-bay') === bayId;
            cell.classList.toggle('selected', isSel);
            cell.setAttribute('aria-current', isSel ? 'true' : 'false');
        });
    }

    updateInspectorDetails();
}

function updateInspectorDetails() {
    const comp = state.lockers.find(c => c.bayId === state.selectedBayId) || state.lockers[0];
    if (!comp) return;

    // Header & Bay ID
    const bayIdEl = document.getElementById('inspectorBayId');
    if (bayIdEl) bayIdEl.textContent = 'Bay ' + comp.bayId;

    // Size Tier Badge
    const tierBadge = document.getElementById('inspectorSizeTier');
    const isGround = comp.bayId.startsWith('H') || comp.row === 7 || comp.sizeTier === 'XL_GROUND_SLOT';
    if (tierBadge) {
        if (isGround) {
            tierBadge.textContent = 'GROUND VEHICLE SLOT (ROW H) • XL DRIVE-IN';
            tierBadge.className = 'tier-badge tier-mobility';
        } else if (comp.sizeTier === 'LARGE') {
            tierBadge.textContent = 'LARGE LOCKER (ROWS F–G) • 80L';
            tierBadge.className = 'tier-badge tier-lighting';
        } else if (comp.sizeTier === 'MEDIUM') {
            tierBadge.textContent = 'MEDIUM LOCKER (ROWS D–E) • 40L';
            tierBadge.className = 'tier-badge tier-tools';
        } else {
            tierBadge.textContent = 'SMALL LOCKER (ROWS A–C) • 20L';
            tierBadge.className = 'tier-badge tier-cameras';
        }
    }

    // Status Pill on top of inspector
    const stateTag = document.getElementById('inspectorDeviceStateTag');
    const asset = state.assets.find(a => (comp.currentAssetId && a.id === comp.currentAssetId) || a.lockerCoordinate === comp.bayId);
    if (stateTag) {
        if (asset && asset.state === 'DISPUTED') {
            stateTag.className = 'state-pill state-disputed';
            stateTag.textContent = 'IN_DISPUTE';
        } else if (comp.status === 'QUARANTINE_LOCKED' || (asset && asset.state === 'INSPECTION_QUARANTINE')) {
            stateTag.className = 'state-pill state-quarantine';
            stateTag.textContent = 'QUARANTINE_LOCKED';
        } else if (comp.status === 'DISPATCHED_ACTIVE' || (asset && asset.state === 'ACTIVE_LEASE')) {
            stateTag.className = 'state-pill state-active';
            stateTag.textContent = 'DISPATCHED_ACTIVE';
        } else if (comp.status === 'RESERVED_LOCKED' || (asset && asset.state === 'RESERVED_ESCROW')) {
            stateTag.className = 'state-pill state-reserved';
            stateTag.textContent = 'RESERVED_LOCKED';
        } else {
            stateTag.className = 'state-pill state-available';
            stateTag.textContent = comp.status || 'ONLINE_READY';
        }
    }

    // Telemetry dials (Simulated virtual sensors)
    const telTemp = document.getElementById('telTemp');
    const telHum = document.getElementById('telHum');
    const telWeight = document.getElementById('telWeight');
    if (telTemp) telTemp.textContent = comp.tempC != null ? comp.tempC.toFixed(1) : '--';
    if (telHum) telHum.textContent = comp.humidityPct != null ? comp.humidityPct.toFixed(1) : '--';
    if (telWeight) telWeight.textContent = comp.weightKg != null ? comp.weightKg.toFixed(1) : '--';

    // Assigned Equipment
    const assetTitleEl = document.getElementById('inspectorAssetTitle');
    const assetIdEl = document.getElementById('inspectorAssetId');
    const assetOwnerEl = document.getElementById('inspectorAssetOwner');

    if (asset) {
        if (assetTitleEl) assetTitleEl.textContent = asset.title;
        if (assetIdEl) assetIdEl.textContent = `${asset.id} • ${asset.leafClass || ''} • ${formatStateName(asset.state)}`;
        if (assetOwnerEl) assetOwnerEl.textContent = `Host: ${asset.ownerId || 'Unknown'}`;
    } else {
        if (assetTitleEl) assetTitleEl.textContent = 'Empty / Available Slot';
        if (assetIdEl) assetIdEl.textContent = 'Ready for assignment';
        if (assetOwnerEl) assetOwnerEl.textContent = 'No owner assigned';
    }

    // OTP Pin Display (OWASP CWE-200 defense)
    const activeUserObj = state.users.find(u => u.userId === state.activeUser);
    const role = activeUserObj ? activeUserObj.role : 'RENTER';
    const isAdmin = role === 'ADMIN' || role === 'INSPECTOR' || (activeUserObj && activeUserObj.userId === 'inspector_almeida');
    const isRenterOfThisAsset = asset && asset.renterId === state.activeUser;

    const otpVal = document.getElementById('inspectorOtp');
    const otpTitle = document.getElementById('inspectorOtpTitle');
    const otpNotice = document.getElementById('inspectorOtpNotice');
    if (otpVal) {
        if ((isAdmin || isRenterOfThisAsset) && comp.otpPin && comp.otpPin.length === 6) {
            otpVal.textContent = comp.otpPin;
            if (otpTitle) otpTitle.textContent = '6-Digit Pickup OTP Passcode';
            if (otpNotice) otpNotice.textContent = isRenterOfThisAsset ? 'Your authorized pickup PIN' : 'Admin diagnostic vault access';
        } else {
            otpVal.textContent = '••••••';
            if (otpTitle) otpTitle.textContent = '6-Digit Pickup OTP Passcode';
            if (otpNotice) otpNotice.textContent = 'Masked for security (CWE-200). Authorized renters see PIN in Active Rentals.';
        }
    }

    // Lockout alert
    const lockoutAlert = document.getElementById('inspectorLockoutAlert');
    if (lockoutAlert) {
        lockoutAlert.style.display = (comp.lockedOut || comp.failedAttempts >= 3) ? 'block' : 'none';
    }

    // Admin Diagnostic Deck vs Locked Notice
    const adminDeck = document.getElementById('inspectorAdminDeck');
    const adminLocked = document.getElementById('inspectorAdminLocked');
    const adminTargetBay = document.getElementById('adminTargetBayLabel');

    if (isAdmin) {
        if (adminDeck) adminDeck.style.display = 'block';
        if (adminLocked) adminLocked.style.display = 'none';
        if (adminTargetBay) adminTargetBay.textContent = 'Bay ' + comp.bayId;
    } else {
        if (adminDeck) adminDeck.style.display = 'none';
        if (adminLocked) adminLocked.style.display = 'block';
    }
}

async function applyInspectorDeviceState() {
    const user = state.users.find(u => u.userId === state.activeUser);
    const isAdmin = user && (user.role === 'ADMIN' || user.role === 'INSPECTOR' || user.userId === 'inspector_almeida');
    if (!isAdmin) {
        showToast('Unauthorized: Administrator role required for hardware diagnostics.', 'error');
        return;
    }

    const bayId = state.selectedBayId || 'A2';
    const stateStr = document.getElementById('inspectorDeviceStateSelect')?.value || 'ONLINE_READY';
    const fb = document.getElementById('adminDiagFeedback');

    const confirmed = confirm(
        `[SIMULATION WARNING]\n\nYou are about to send a simulated solenoid override for Bay ${bayId} to: "${stateStr}".\n\n` +
        `Note: This is a software simulation running against the virtual locker matrix. No physical hardware is connected.\n\n` +
        `Proceed with simulation command?`
    );
    if (!confirmed) return;

    try {
        const headers = { 'Content-Type': 'application/json' };
        if (state.authToken) {
            headers['Authorization'] = `Bearer ${state.authToken}`;
        }
        const res = await fetch(`${API_BASE}/admin/device/state`, {
            method: 'POST',
            headers,
            credentials: 'same-origin',
            body: JSON.stringify({ bayId, state: stateStr })
        });
        const data = await res.json();
        if (fb) {
            fb.style.display = 'block';
            if (data.success) {
                fb.style.background = 'var(--emerald-light)';
                fb.style.color = 'var(--forest-green)';
                fb.style.border = '1px solid rgba(16,185,129,0.3)';
                fb.textContent = `Server (HTTP ${res.status}): Bay ${bayId} solenoid state -> ${stateStr} (Simulated)`;
                showToast(`Simulated Bay ${bayId} state updated to ${stateStr}`, 'success');
            } else {
                fb.style.background = 'var(--crimson-light)';
                fb.style.color = 'var(--crimson)';
                fb.style.border = '1px solid rgba(239,68,68,0.3)';
                fb.textContent = `Server (HTTP ${res.status}): ${data.error || data.message || 'Override failed'}`;
                showToast('Hardware override rejected: ' + (data.error || data.message), 'error');
            }
        }
        await fetchLockers();
    } catch (e) {
        if (fb) {
            fb.style.display = 'block';
            fb.style.background = 'var(--crimson-light)';
            fb.style.color = 'var(--crimson)';
            fb.style.border = '1px solid rgba(239,68,68,0.3)';
            fb.textContent = 'Hardware simulation network error: ' + e.message;
        }
        showToast('Device simulation error: ' + e.message, 'error');
    }
}

// Pickup & Return Handlers
async function pickupSelectedBayGear() {
    const comp = state.lockers.find(c => c.bayId === state.selectedBayId);
    if (!comp) return;

    const asset = state.assets.find(a => a.lockerCoordinate === comp.bayId && a.state === 'RESERVED_ESCROW');
    if (!asset) {
        showToast('No reserved equipment awaiting pickup in Bay ' + comp.bayId, 'warning');
        return;
    }

    const otpInput = prompt(`Enter 6-Digit Pickup OTP Passcode for Bay ${comp.bayId}:`, comp.otpPin || '');
    if (!otpInput) return;

    try {
        const res = await fetch(`${API_BASE}/assets/pickup`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ assetId: asset.id, otpPin: otpInput.trim() })
        });
        const data = await res.json();
        if (data.success) {
            showToast('🔓 ' + data.message, 'success');
            await fetchAllData();
        } else {
            showToast('❌ ' + data.message, 'error');
        }
    } catch (e) {
        showToast('Error during pickup: ' + e.message, 'error');
    }
}

async function returnSelectedBayGear() {
    const asset = state.assets.find(a => a.lockerCoordinate === state.selectedBayId && a.state === 'ACTIVE_LEASE');
    if (!asset) {
        showToast('No active leased equipment to return to Bay ' + state.selectedBayId, 'warning');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/assets/return`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ assetId: asset.id })
        });
        const data = await res.json();
        if (data.success) {
            showToast('📦 ' + data.message, 'success');
            await fetchAllData();
        } else {
            showToast('❌ ' + data.message, 'error');
        }
    } catch (e) {
        showToast('Error returning equipment: ' + e.message, 'error');
    }
}

// ============================================================================
// TAB 3: ESCROW WALLET, QUARANTINE & AUDIT WAL
// ============================================================================
function renderQuarantineDeck() {
    const container = document.getElementById('quarantineCardsGrid');
    if (!container) return;

    const quarantined = state.assets.filter(a => a.state === 'INSPECTION_QUARANTINE' || a.state === 'DISPUTED');
    if (quarantined.length === 0) {
        container.innerHTML = `
            <div style="grid-column: 1/-1; text-align:center; padding: 40px 20px; background:#FFFFFF; border-radius:var(--radius-lg); border:1px solid var(--slate-200);">
                <div style="font-size:32px; margin-bottom:8px;">✅</div>
                <h3 style="font-family:var(--font-heading); font-size:16px; font-weight:700;">No Returns Awaiting Inspection</h3>
                <p style="font-size:12px; color:var(--slate-500); margin-top:4px;">All returned equipment has been verified and deposits settled.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = quarantined.map(a => {
        const isDisputed = a.state === 'DISPUTED';
        const user = state.users.find(u => u.userId === state.activeUser);
        const role = user ? user.role : 'RENTER';
        const isAssetOwner = a.ownerId === state.activeUser;
        const isAdmin = role === 'ADMIN' || role === 'INSPECTOR' || (user && user.userId === 'inspector_almeida');
        const isRenter = a.renterId === state.activeUser;
        const canHostInspect = isAssetOwner || isAdmin;

        return `
            <div class="quarantine-card ${isDisputed ? 'disputed-card' : ''}">
                <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                    <div>
                        <span class="cat-pill-badge">${getCategoryDisplayName(a.leafClass)}</span>
                        <h4 style="font-family:var(--font-heading); font-size:16px; font-weight:800; margin-top:6px;">${escapeHtml(a.title)}</h4>
                        <div style="font-size:12px; color:var(--slate-500);">Bay: <strong>${a.lockerCoordinate}</strong> • Renter: <strong>${escapeHtml(a.renterId)}</strong> • Host: <strong>${escapeHtml(a.ownerId)}</strong></div>
                    </div>
                    <span class="state-pill ${isDisputed ? 'state-disputed' : 'state-quarantine'}">
                        ${isDisputed ? '⚖️ IN DISPUTE' : '⚠️ MANDATORY QUARANTINE'}
                    </span>
                </div>

                <div style="background:var(--slate-50); padding:12px; border-radius:var(--radius-md); margin:14px 0; border:1px solid var(--slate-200);">
                    <div style="display:flex; justify-content:space-between; font-size:12px; margin-bottom:4px;">
                        <span>Held Security Collateral:</span>
                        <strong style="color:var(--crimson); font-size:14px;">${formatInr(a.requiredCollateral)}</strong>
                    </div>
                    <div style="font-size:11px; color:var(--slate-500);">
                        ${isDisputed 
                            ? 'Arbitration active by Goa Hub Lead. Deposit release frozen pending evidence review.' 
                            : 'Host must approve condition within 24h. Renter may contest deductions.'}
                    </div>
                </div>

                ${!isDisputed ? `
                    ${canHostInspect ? `
                        <div style="display:flex; flex-direction:column; gap:8px;">
                            <button class="btn-primary approve-inspection-btn" data-id="${a.id}" style="justify-content:center;">
                                <span>✅</span> Approve Clean Return (100% Deposit Refund)
                            </button>
                            <button class="btn-danger slash-inspection-btn" data-id="${a.id}" style="justify-content:center;">
                                <span>⚠️</span> Flag 20% Damage Penalty
                            </button>
                            <div style="font-size:11px; color:var(--slate-500); text-align:center;">
                                Host Inspection Authority: Verify physical condition before releasing escrow.
                            </div>
                        </div>
                    ` : isRenter ? `
                        <div style="display:flex; flex-direction:column; gap:8px;">
                            <button class="btn-secondary contest-dispute-btn" data-id="${a.id}" data-collateral="${a.requiredCollateral}" data-title="${escapeHtml(a.title)}" style="justify-content:center; font-size:12px;">
                                <span>⚖️</span> Contest Claim / File Formal Dispute
                            </button>
                            <div style="font-size:11px; color:var(--slate-500); text-align:center;">
                                Waiting for Host (${escapeHtml(a.ownerId)}) inspection. Click above to freeze deposit if disputed.
                            </div>
                        </div>
                    ` : `
                        <div style="font-size:12px; color:var(--slate-500); text-align:center; padding:10px; background:#FFFFFF; border-radius:var(--radius-sm); border:1px dashed var(--slate-200);">
                            🔒 Inspection reserved for Asset Owner (<strong>${escapeHtml(a.ownerId)}</strong>) or Hub Administrator.
                        </div>
                    `}
                ` : `
                    <div style="font-size:12px; font-weight:700; color:var(--purple); text-align:center; padding:8px; background:var(--purple-light); border-radius:var(--radius-sm);">
                        ⚖️ Formal Dispute Record Active (Review in Admin Hub)
                    </div>
                `}
            </div>
        `;
    }).join('');

    // Attach Handlers
    container.querySelectorAll('.approve-inspection-btn').forEach(btn => {
        btn.addEventListener('click', () => approveInspection(btn.getAttribute('data-id')));
    });

    container.querySelectorAll('.slash-inspection-btn').forEach(btn => {
        btn.addEventListener('click', () => slashInspection(btn.getAttribute('data-id')));
    });

    container.querySelectorAll('.contest-dispute-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const assetId = btn.getAttribute('data-id');
            const collateral = btn.getAttribute('data-collateral');
            const title = btn.getAttribute('data-title');
            openDisputeModal(assetId, title, collateral);
        });
    });
}

async function approveInspection(assetId) {
    try {
        const res = await fetch(`${API_BASE}/assets/inspect-approve`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ assetId, hostId: state.activeUser })
        });
        const data = await res.json();
        if (data.success) {
            showToast('✅ ' + data.message, 'success');
            await fetchAllData();
        } else {
            showToast('❌ ' + data.message, 'error');
        }
    } catch (e) {
        showToast('Error approving inspection: ' + e.message, 'error');
    }
}

async function slashInspection(assetId) {
    try {
        const res = await fetch(`${API_BASE}/assets/inspect-slash`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ assetId, hostId: state.activeUser, damagePercentage: 20.0 })
        });
        const data = await res.json();
        if (data.success) {
            showToast('⚠️ ' + data.message, 'warning');
            await fetchAllData();
        } else {
            showToast('❌ ' + data.message, 'error');
        }
    } catch (e) {
        showToast('Error slashing collateral: ' + e.message, 'error');
    }
}

function renderWalTable() {
    const tbody = document.getElementById('walTableBody');
    if (!tbody) return;

    if (state.wal.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; padding:20px;">No audit log records loaded.</td></tr>';
        return;
    }

    tbody.innerHTML = state.wal.map(e => `
        <tr>
            <td style="font-family:monospace; font-size:11px;">${e.timestamp || ''}</td>
            <td style="font-family:monospace; font-size:11px; color:var(--brand-teal);">${e.txId || ''}</td>
            <td style="font-weight:700;">${e.entityId || ''}</td>
            <td><span class="action-tag">${e.actionType || ''}</span></td>
            <td style="font-size:11px; color:var(--slate-400);">${e.previousState || ''}</td>
            <td style="font-size:11px; font-weight:700;">${e.newState || ''}</td>
            <td style="font-size:11px;">${e.actor || ''}</td>
            <td style="font-size:12px; color:var(--slate-600);">${escapeHtml(e.details || '')}</td>
        </tr>
    `).join('');
}

// ============================================================================
// TAB 4: HOST STUDIO, CLONER & TRUST SCORE ENGINE
// ============================================================================
function populateHostDropdowns() {
    const cloneSel = document.getElementById('cloneSourceSelect');
    const metaSel = document.getElementById('metaAssetSelect');

    if (cloneSel) {
        cloneSel.innerHTML = state.assets.map(a => `
            <option value="${a.id}">${a.title} (${a.id} • ${formatInrCompact(a.calculatedTariff)}/day)</option>
        `).join('');
    }

    if (metaSel) {
        metaSel.innerHTML = state.assets.map(a => `
            <option value="${a.id}">${a.title} (${a.leafClass})</option>
        `).join('');
    }
}

function updateClonerMemoryPreview() {
    const sourceId = document.getElementById('cloneSourceSelect')?.value;
    if (!sourceId) return;

    fetch(`${API_BASE}/meta/clone?id=${sourceId}`)
        .then(r => r.json())
        .then(rep => {
            document.getElementById('shallowLogsId').textContent = rep.shallowLogsId;
            document.getElementById('deepLogsId').textContent = rep.deepLogsId;
        })
        .catch(console.error);
}

async function submitCloneListing() {
    const sourceId = document.getElementById('cloneSourceSelect')?.value;
    const targetBay = document.getElementById('cloneTargetBayInput')?.value || '';

    if (!sourceId) return;

    try {
        const res = await fetch(`${API_BASE}/assets/clone`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ assetId: sourceId, targetBay })
        });
        const data = await res.json();
        if (data.success) {
            showToast('⚡ ' + data.message, 'success');
            await fetchAllData();
        } else {
            showToast('❌ ' + data.message, 'error');
        }
    } catch (e) {
        showToast('Error cloning listing: ' + e.message, 'error');
    }
}

async function runTrustScoreCalculation() {
    const cond = Number(document.getElementById('sliderCond')?.value || 90);
    const punct = Number(document.getElementById('sliderPunct')?.value || 95);
    const escrow = Number(document.getElementById('sliderEscrow')?.value || 85);
    const peer = Number(document.getElementById('sliderPeer')?.value || 90);
    const kyc = Number(document.getElementById('sliderKyc')?.value || 95);

    document.getElementById('valCond').textContent = cond.toFixed(1);
    document.getElementById('valPunct').textContent = punct.toFixed(1);
    document.getElementById('valEscrow').textContent = escrow.toFixed(1);
    document.getElementById('valPeer').textContent = peer.toFixed(1);
    document.getElementById('valKyc').textContent = kyc.toFixed(1);

    try {
        const res = await fetch(`${API_BASE}/trust-score`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                memberName: getActiveUserName(),
                conditionHistory: cond,
                punctualityRate: punct,
                escrowCompletion: escrow,
                peerRating: peer,
                kycTier: kyc
            })
        });
        const data = await res.json();
        document.getElementById('trustCompositeScore').textContent = data.simulatedScore.toFixed(2);
        document.getElementById('trustTierBadge').textContent = data.simulatedTier;
        document.getElementById('trustPolicyText').textContent = data.policyRecommendation;
    } catch (e) {
        console.error('Trust score calculation error:', e);
    }
}

async function runLiveReflection() {
    const assetId = document.getElementById('metaAssetSelect')?.value || state.assets[0]?.id;
    if (!assetId) return;

    try {
        const res = await fetch(`${API_BASE}/meta/reflect?id=${assetId}`);
        if (!res.ok) return;
        const rep = await res.json();

        const chainHtml = rep.inheritanceChain.map((cls, idx) => `
            <span style="font-weight:${idx === 0 ? '800' : '500'}; color:${idx === 0 ? 'var(--brand-teal)' : 'var(--slate-600)'};">
                ${cls}
            </span>
        `).join(' &rarr; ');

        document.getElementById('reflectionInheritanceChain').innerHTML = chainHtml;

        const codeLines = [
            `// RTTI Introspection via java.lang.reflect for ${rep.className}`,
            `Package: ${rep.packageName}`,
            `Inheritance Depth: ${rep.inheritanceChain.length} tiers`,
            `Interfaces: ${rep.implementedInterfaces.join(', ') || 'None'}`,
            '',
            '// Declared Runtime Fields:',
            ...rep.declaredFields.map(f => `  ${f.modifiers} ${f.type} ${f.name} = "${f.value}";`),
            '',
            '// Declared Methods:',
            ...rep.declaredMethods.map(m => `  ${m.modifiers} ${m.returnType} ${m.name}(${m.parameters});`)
        ].join('\n');

        document.getElementById('reflectionDetailsBox').textContent = codeLines;
    } catch (e) {
        console.error('Reflection inspector error:', e);
    }
}

// ============================================================================
// TAB 5: ADMIN, GRIEVANCE & SUPPORT HUB (IT RULES 2021)
// ============================================================================
async function renderAdminSupportView() {
    await Promise.all([
        fetchDisputes(),
        fetchSupportTickets(),
        fetchAdminUsers()
    ]);
}

async function fetchDisputes() {
    try {
        const res = await fetch(`${API_BASE}/disputes`);
        if (!res.ok) return;
        state.disputes = await res.json();

        const container = document.getElementById('disputeListContainer');
        const badge = document.getElementById('disputeCountBadge');
        const activeDisputes = state.disputes.filter(d => d.status === 'OPEN' || d.status === 'UNDER_REVIEW');

        badge.textContent = activeDisputes.length + ' Active';

        if (state.disputes.length === 0) {
            container.innerHTML = '<div class="empty-state-box">No damage disputes recorded.</div>';
            return;
        }

        container.innerHTML = state.disputes.map(d => `
            <div class="dispute-item-card">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <strong>${d.disputeId} • Asset: ${d.assetId}</strong>
                    <span class="state-pill ${d.status === 'OPEN' ? 'state-disputed' : 'state-available'}">${d.status}</span>
                </div>
                <div style="font-size:12px; margin:4px 0;">
                    Renter: <strong>${d.renterId}</strong> vs Host: <strong>${d.ownerId}</strong> | Held: <strong>${formatInr(d.heldDeposit)}</strong>
                </div>
                <div style="font-size:11px; color:var(--slate-600); margin:4px 0;">
                    <em>Reason: "${escapeHtml(d.renterReason)}"</em>
                </div>
                <div style="font-size:11px; color:var(--slate-500);">
                    <em>Evidence: "${escapeHtml(d.evidenceNotes)}"</em>
                </div>
                ${d.status === 'OPEN' ? `
                    <div style="display:flex; gap:8px; margin-top:8px;">
                        <button class="btn-primary arbitrate-refund-btn" data-id="${d.disputeId}" style="padding:4px 10px; font-size:11px;">
                            <span>✅</span> Rule for Renter (Full Refund)
                        </button>
                        <button class="btn-danger arbitrate-slash-btn" data-id="${d.disputeId}" style="padding:4px 10px; font-size:11px;">
                            <span>⚠️</span> Rule for Host (Slash Penalty)
                        </button>
                    </div>
                ` : `
                    <div style="font-size:10px; color:var(--emerald-dark); margin-top:4px; font-weight:700;">
                        ${d.adminNotes || 'Arbitrated'}
                    </div>
                `}
            </div>
        `).join('');

        container.querySelectorAll('.arbitrate-refund-btn').forEach(b => {
            b.addEventListener('click', () => arbitrateDispute(b.getAttribute('data-id'), 'RESOLVED_REFUND'));
        });
        container.querySelectorAll('.arbitrate-slash-btn').forEach(b => {
            b.addEventListener('click', () => arbitrateDispute(b.getAttribute('data-id'), 'RESOLVED_SLASH'));
        });
    } catch (e) {
        console.error('Fetch disputes error:', e);
    }
}

async function arbitrateDispute(disputeId, decision) {
    try {
        const res = await fetch(`${API_BASE}/disputes/resolve`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                disputeId,
                decision,
                adminNotes: 'Arbitrated by Prof. Almeida (Goa Hub Lead) after photo review.'
            })
        });
        const data = await res.json();
        if (data.success) {
            showToast('⚖️ Dispute arbitrated: ' + decision, 'success');
            await fetchAllData();
            await fetchDisputes();
        } else {
            showToast('❌ ' + data.message, 'error');
        }
    } catch (e) {
        showToast('Error resolving dispute: ' + e.message, 'error');
    }
}

async function fetchSupportTickets() {
    try {
        const res = await fetch(`${API_BASE}/support/tickets`);
        if (!res.ok) return;
        state.tickets = await res.json();

        const container = document.getElementById('ticketListContainer');
        const badge = document.getElementById('ticketCountBadge');
        const openTickets = state.tickets.filter(t => t.status === 'OPEN');

        badge.textContent = openTickets.length + ' Open';

        if (state.tickets.length === 0) {
            container.innerHTML = '<div class="empty-state-box">No customer grievances on file.</div>';
            return;
        }

        container.innerHTML = state.tickets.map(t => `
            <div class="ticket-item-card">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <strong>${t.ticketId} • [${t.category}]</strong>
                    <span class="state-pill ${t.status === 'OPEN' ? 'state-quarantine' : 'state-available'}">${t.status}</span>
                </div>
                <div style="font-weight:700; font-size:13px; margin:4px 0;">${escapeHtml(t.subject)}</div>
                <div style="font-size:12px; color:var(--slate-600);">${escapeHtml(t.description)}</div>
                ${t.status === 'OPEN' ? `
                    <button class="btn-secondary resolve-ticket-btn" data-id="${t.ticketId}" style="margin-top:8px; padding:4px 10px; font-size:11px;">
                        <span>✓</span> Mark Resolved by Grievance Lead
                    </button>
                ` : `
                    <div style="font-size:10px; color:var(--emerald-dark); margin-top:4px;">${escapeHtml(t.resolution)}</div>
                `}
            </div>
        `).join('');

        container.querySelectorAll('.resolve-ticket-btn').forEach(b => {
            b.addEventListener('click', () => resolveTicket(b.getAttribute('data-id')));
        });
    } catch (e) {
        console.error('Fetch support tickets error:', e);
    }
}

async function resolveTicket(ticketId) {
    try {
        const res = await fetch(`${API_BASE}/support/reply`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                ticketId,
                status: 'RESOLVED',
                resolution: 'Addressed and resolved within statutory 24h IT Rules SLA.'
            })
        });
        const data = await res.json();
        if (data.success) {
            showToast('✅ Grievance ticket resolved.', 'success');
            await fetchSupportTickets();
        }
    } catch (e) {
        showToast('Error resolving ticket: ' + e.message, 'error');
    }
}

async function fetchAdminUsers() {
    try {
        const res = await fetch(`${API_BASE}/admin/users`);
        if (!res.ok) return;
        const users = await res.json();
        const tbody = document.getElementById('adminUsersTableBody');
        if (!tbody) return;

        tbody.innerHTML = users.map(u => `
            <tr>
                <td style="font-weight:700;">${u.userId}</td>
                <td>${escapeHtml(u.email)}</td>
                <td><span class="role-tag role-${u.role.toLowerCase()}">${u.role}</span></td>
                <td><span style="font-size:11px; font-weight:700; color:${u.accountStatus === 'ACTIVE' ? 'var(--emerald-dark)' : 'var(--crimson)'}">${u.accountStatus}</span></td>
                <td>${u.kycVerified ? '✅ Verified' : '⏳ Pending'}</td>
                <td>
                    <button class="btn-secondary toggle-status-btn" data-id="${u.userId}" data-status="${u.accountStatus}" style="padding:2px 8px; font-size:10px;">
                        ${u.accountStatus === 'ACTIVE' ? 'Suspend' : 'Activate'}
                    </button>
                </td>
            </tr>
        `).join('');

        tbody.querySelectorAll('.toggle-status-btn').forEach(b => {
            b.addEventListener('click', () => {
                const uid = b.getAttribute('data-id');
                const cur = b.getAttribute('data-status');
                const next = cur === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
                toggleUserStatus(uid, next);
            });
        });
    } catch (e) {
        console.error('Admin users fetch error:', e);
    }
}

async function toggleUserStatus(userId, status) {
    try {
        const res = await fetch(`${API_BASE}/admin/users/status`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId, status })
        });
        const data = await res.json();
        if (data.success) {
            showToast(`User ${userId} status set to ${status}`, 'info');
            await fetchAdminUsers();
        }
    } catch (e) {
        showToast('Error updating status: ' + e.message, 'error');
    }
}

async function applySimulatedDeviceState() {
    const bayId = document.getElementById('simBayIdInput')?.value.toUpperCase().trim() || 'A2';
    const stateStr = document.getElementById('simDeviceStateSelect')?.value || 'ONLINE_READY';

    try {
        const headers = { 'Content-Type': 'application/json' };
        if (state.authToken) {
            headers['Authorization'] = `Bearer ${state.authToken}`;
        }
        const res = await fetch(`${API_BASE}/admin/device/state`, {
            method: 'POST',
            headers,
            credentials: 'same-origin',
            body: JSON.stringify({ bayId, state: stateStr })
        });
        const data = await res.json();
        if (data.success) {
            showToast(`Simulated Bay ${bayId} state updated to ${stateStr}`, 'success');
            await fetchLockers();
        } else {
            showToast('Error: ' + data.error, 'error');
        }
    } catch (e) {
        showToast('Device simulation error: ' + e.message, 'error');
    }
}

// ============================================================================
// IN-APP NOTIFICATIONS CENTER
// ============================================================================
function initNotifications() {
    const bell = document.getElementById('notifBellBtn');
    const dropdown = document.getElementById('notifDropdown');
    const markReadBtn = document.getElementById('markNotifsReadBtn');

    bell?.addEventListener('click', (e) => {
        e.stopPropagation();
        dropdown?.classList.toggle('show');
    });

    document.addEventListener('click', (e) => {
        if (!dropdown?.contains(e.target) && e.target !== bell) {
            dropdown?.classList.remove('show');
        }
    });

    markReadBtn?.addEventListener('click', async () => {
        try {
            await fetch(`${API_BASE}/notifications/read`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId: state.activeUser })
            });
            fetchNotifications();
        } catch (e) {
            console.error(e);
        }
    });
}

async function fetchNotifications() {
    try {
        const res = await fetch(`${API_BASE}/notifications?userId=${state.activeUser}`);
        if (!res.ok) return;
        state.notifications = await res.json();

        const badge = document.getElementById('notifBadge');
        const list = document.getElementById('notifList');
        const unread = state.notifications.filter(n => !n.read);

        if (unread.length > 0) {
            badge.style.display = 'block';
            badge.textContent = unread.length;
        } else {
            badge.style.display = 'none';
        }

        if (state.notifications.length === 0) {
            list.innerHTML = '<div class="notif-empty">No notifications yet</div>';
            return;
        }

        list.innerHTML = state.notifications.map(n => `
            <div class="notif-item ${!n.read ? 'unread' : ''}">
                <div class="notif-title">${escapeHtml(n.title)}</div>
                <div>${escapeHtml(n.message)}</div>
            </div>
        `).join('');
    } catch (e) {
        console.error('Fetch notifs error:', e);
    }
}

// ============================================================================
// LEGAL & REGULATORY POLICY CENTER (20 DOCUMENTS)
// ============================================================================
function initPolicies() {
    document.getElementById('policySearchInput')?.addEventListener('input', (e) => {
        renderPolicyAccordion(e.target.value.toLowerCase());
    });
}

async function openPolicyCenter(focusKey = null) {
    openModal('policyModal');
    if (state.policies.length === 0) {
        try {
            const res = await fetch(`${API_BASE}/policies`);
            if (res.ok) {
                state.policies = await res.json();
            }
        } catch (e) {
            console.error(e);
        }
    }
    renderPolicyAccordion('');
    if (focusKey && typeof focusKey === 'string') {
        setTimeout(() => {
            const el = document.getElementById('policy-item-' + focusKey);
            if (el) el.scrollIntoView({ behavior: 'smooth' });
        }, 200);
    }
}

function renderPolicyAccordion(search = '') {
    const container = document.getElementById('policyAccordionContainer');
    if (!container) return;

    const filtered = state.policies.filter(p => {
        if (!search) return true;
        return p.title.toLowerCase().includes(search) || p.summary.toLowerCase().includes(search) || p.jurisdictionNote.toLowerCase().includes(search);
    });

    if (filtered.length === 0) {
        container.innerHTML = '<div style="padding:20px; text-align:center;">No matching policy documents found.</div>';
        return;
    }

    container.innerHTML = filtered.map(p => `
        <div class="policy-card-item" id="policy-item-${p.id}">
            <div class="policy-card-title">
                <span>${p.title}</span>
                <span style="font-size:11px; font-weight:600; color:var(--brand-teal);">${p.version}</span>
            </div>
            <div class="policy-card-summary">${escapeHtml(p.summary)}</div>
            <div class="policy-card-jurisdiction">⚖️ Framework: ${escapeHtml(p.jurisdictionNote)}</div>
            <div style="font-size:11px; color:var(--slate-700); margin-top:8px; line-height:1.5; border-top:1px dashed var(--slate-200); padding-top:6px;">
                ${escapeHtml(p.content)}
            </div>
        </div>
    `).join('');
}

async function recordPolicyConsent() {
    try {
        const res = await fetch(`${API_BASE}/policies/accept`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: state.activeUser,
                policyIds: 'tos,privacy,rental_agreement,cancellation,deposit_payment'
            })
        });
        const data = await res.json();
        if (data.success) {
            showToast('✍️ Legal consent logged with auditable timestamp.', 'success');
            closeModal('policyModal');
        }
    } catch (e) {
        showToast('Error recording consent: ' + e.message, 'error');
    }
}

// ============================================================================
// AUTHENTICATION & PERSONA MANAGEMENT (PBKDF2)
// ============================================================================
function initAuthModals() {
    const tabLogin = document.getElementById('tabAuthLogin');
    const tabReg = document.getElementById('tabAuthRegister');
    const tabQuick = document.getElementById('tabAuthQuick');

    const formLogin = document.getElementById('authLoginForm');
    const formReg = document.getElementById('authRegisterForm');
    const formQuick = document.getElementById('authQuickForm');

    tabLogin?.addEventListener('click', () => {
        [tabLogin, tabReg, tabQuick].forEach(t => t.classList.remove('active'));
        tabLogin.classList.add('active');
        formLogin.style.display = 'block';
        formReg.style.display = 'none';
        formQuick.style.display = 'none';
    });

    tabReg?.addEventListener('click', () => {
        [tabLogin, tabReg, tabQuick].forEach(t => t.classList.remove('active'));
        tabReg.classList.add('active');
        formLogin.style.display = 'none';
        formReg.style.display = 'block';
        formQuick.style.display = 'none';
    });

    tabQuick?.addEventListener('click', () => {
        [tabLogin, tabReg, tabQuick].forEach(t => t.classList.remove('active'));
        tabQuick.classList.add('active');
        formLogin.style.display = 'none';
        formReg.style.display = 'none';
        formQuick.style.display = 'block';
    });

    // Execute Login
    document.getElementById('executeLoginBtn')?.addEventListener('click', async () => {
        const email = document.getElementById('loginEmail')?.value;
        const password = document.getElementById('loginPassword')?.value;
        await executeLogin(email, password);
    });

    // Execute Register
    document.getElementById('executeRegisterBtn')?.addEventListener('click', async () => {
        const fullName = document.getElementById('regFullName')?.value;
        const email = document.getElementById('regEmail')?.value;
        const password = document.getElementById('regPassword')?.value;
        const role = document.getElementById('regRole')?.value;
        const phone = document.getElementById('regPhone')?.value;

        if (!email || !password || !fullName) {
            showToast('Please fill all required fields.', 'warning');
            return;
        }

        try {
            const res = await fetch(`${API_BASE}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password, fullName, role, phone })
            });
            const data = await res.json();
            if (data.success) {
                state.authToken = data.token;
                state.activeUser = data.user.userId;
                closeModal('authModal');
                showToast(`✨ Account created for ${data.user.fullName}!`, 'success');
                await fetchAllData();
            } else {
                showToast('❌ ' + data.error, 'error');
            }
        } catch (e) {
            showToast('Registration error: ' + e.message, 'error');
        }
    });

    // Quick persona buttons
    document.querySelectorAll('.quick-persona-btn').forEach(btn => {
        btn.addEventListener('click', async () => {
            const userKey = btn.getAttribute('data-user');
            await executeLogin(userKey, 'RentSphere@2026');
        });
    });
}

async function executeLogin(emailOrUser, password) {
    try {
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: emailOrUser, password })
        });
        const data = await res.json();
        if (data.success) {
            state.authToken = data.token;
            state.activeUser = data.user.userId;

            // Update Persona select dropdown
            const select = document.getElementById('activeUserSelect');
            if (select) {
                let opt = Array.from(select.options).find(o => o.value === data.user.userId);
                if (!opt) {
                    opt = new Option(data.user.fullName + ` (${data.user.role})`, data.user.userId);
                    select.add(opt);
                }
                select.value = data.user.userId;
            }

            closeModal('authModal');
            showToast(`🔑 Signed in as ${data.user.fullName} (${data.user.role})`, 'success');
            await fetchAllData();
        } else {
            showToast('❌ ' + data.error, 'error');
        }
    } catch (e) {
        showToast('Login error: ' + e.message, 'error');
    }
}

// ============================================================================
// RESERVATION & PAYMENT SANDBOX (₹ INR)
// ============================================================================
function openReserveModal(assetId) {
    if (!state.activeUser) {
        openModal('authModal');
        showToast('Please sign in or select a demo persona to rent equipment.', 'warning');
        return;
    }

    const asset = state.assets.find(a => a.id === assetId);
    if (!asset) return;

    state.currentReserveAsset = asset;
    document.getElementById('reserveAssetId').value = asset.id;
    document.getElementById('modalAssetTitle').textContent = asset.title;
    document.getElementById('modalAssetBay').textContent = 'Target Hub Bay / Slot: ' + asset.lockerCoordinate;
    document.getElementById('rentalDaysInput').value = 2;

    updateReserveEstimates();
    openModal('reserveModal');
}

function updateReserveEstimates() {
    if (!state.currentReserveAsset) return;
    const days = parseInt(document.getElementById('rentalDaysInput')?.value) || 1;
    const tariff = state.currentReserveAsset.calculatedTariff * days;
    const collateral = state.currentReserveAsset.requiredCollateral;
    const total = tariff + collateral;

    document.getElementById('modalTariffEstimate').textContent = formatInr(tariff);
    document.getElementById('modalCollateralEstimate').textContent = formatInr(collateral);
    document.getElementById('modalTotalEscrow').textContent = formatInr(total);
}

async function submitReserveAsset() {
    if (!state.currentReserveAsset) return;
    const assetId = state.currentReserveAsset.id;
    const days = parseInt(document.getElementById('rentalDaysInput')?.value) || 1;
    const payOption = document.querySelector('input[name="paymentOption"]:checked')?.value || 'wallet';

    if (payOption === 'gateway') {
        // Open Indian Payment Sandbox Modal
        closeModal('reserveModal');
        try {
            const res = await fetch(`${API_BASE}/payments/create-order`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ assetId, renterId: state.activeUser, rentalDays: days })
            });
            const data = await res.json();
            if (data.success) {
                state.pendingPaymentOrder = { ...data, assetId, days };
                document.getElementById('sandboxOrderId').textContent = data.orderId;
                document.getElementById('sandboxTariff').textContent = formatInr(data.rentalTariff);
                document.getElementById('sandboxDeposit').textContent = formatInr(data.securityDeposit);
                document.getElementById('sandboxTotal').textContent = formatInr(data.totalAmountPayable);
                openModal('paymentSandboxModal');
            } else {
                showToast('❌ Order creation failed: ' + data.message, 'error');
            }
        } catch (e) {
            showToast('Gateway order error: ' + e.message, 'error');
        }
        return;
    }

    // Direct Escrow Lock
    await executeReservation(assetId, days);
}

async function authorizeSandboxPayment() {
    if (!state.pendingPaymentOrder) return;
    const { orderId, assetId, days } = state.pendingPaymentOrder;

    try {
        const verifyRes = await fetch(`${API_BASE}/payments/verify`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ orderId, signature: 'sandbox_valid_sig' })
        });
        const verifyData = await verifyRes.json();
        if (verifyData.success) {
            closeModal('paymentSandboxModal');
            showToast('✅ Payment Captured via Indian Gateway Sandbox!', 'success');
            await executeReservation(assetId, days);
        } else {
            showToast('❌ Payment verification failed', 'error');
        }
    } catch (e) {
        showToast('Payment processing error: ' + e.message, 'error');
    }
}

async function executeReservation(assetId, days) {
    try {
        const res = await fetch(`${API_BASE}/assets/reserve`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ assetId, renterId: state.activeUser, rentalDays: days })
        });
        const data = await res.json();
        if (data.success) {
            closeModal('reserveModal');
            showToast(`🛡️ ${data.message} (Pickup OTP: ${data.otpPin})`, 'success');
            await fetchAllData();
        } else {
            showToast(`❌ Gate ${data.gateIndex} (${data.gateName}): ${data.message}`, 'error');
        }
    } catch (e) {
        showToast('Error reserving asset: ' + e.message, 'error');
    }
}

// ============================================================================
// DAMAGE DISPUTES & GRIEVANCE SUPPORT TICKETS
// ============================================================================
function openDisputeModal(assetId, title, collateral) {
    document.getElementById('disputeAssetId').value = assetId;
    document.getElementById('disputeAssetTitle').textContent = title;
    document.getElementById('disputeHeldCollateral').textContent = 'Held Deposit: ' + formatInr(collateral);
    document.getElementById('disputeReasonInput').value = '';
    document.getElementById('disputeEvidenceInput').value = '';
    openModal('disputeModal');
}

async function submitDamageDispute() {
    const assetId = document.getElementById('disputeAssetId')?.value;
    const reason = document.getElementById('disputeReasonInput')?.value.trim();
    const evidence = document.getElementById('disputeEvidenceInput')?.value.trim();

    if (!reason) {
        showToast('Please provide a reason for contesting the deduction.', 'warning');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/disputes/file`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                assetId,
                renterId: state.activeUser,
                renterReason: reason,
                evidenceNotes: evidence
            })
        });
        const data = await res.json();
        if (data.success) {
            closeModal('disputeModal');
            showToast('⚖️ ' + data.message, 'success');
            await fetchAllData();
        } else {
            showToast('❌ ' + data.message, 'error');
        }
    } catch (e) {
        showToast('Dispute filing error: ' + e.message, 'error');
    }
}

async function submitSupportTicket() {
    const cat = document.getElementById('ticketCategory')?.value;
    const sub = document.getElementById('ticketSubject')?.value.trim();
    const desc = document.getElementById('ticketDescription')?.value.trim();

    if (!sub || !desc) {
        showToast('Please enter both subject and description.', 'warning');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/support/tickets`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: state.activeUser,
                category: cat,
                subject: sub,
                description: desc
            })
        });
        const data = await res.json();
        if (data.success) {
            closeModal('supportModal');
            showToast(`📨 Ticket ${data.ticketId} registered. SLA: 24h.`, 'success');
            await fetchSupportTickets();
        }
    } catch (e) {
        showToast('Support ticket error: ' + e.message, 'error');
    }
}

// ============================================================================
// ONBOARDING NEW ASSET
// ============================================================================
async function submitOnboardAsset() {
    const leafClass = document.getElementById('onboardLeafClass')?.value;
    const title = document.getElementById('onboardTitle')?.value.trim();
    const baseDailyRate = document.getElementById('onboardRate')?.value;
    const securityCollateral = document.getElementById('onboardCollateral')?.value;
    const lockerBay = document.getElementById('onboardBay')?.value.toUpperCase().trim();

    if (!title) {
        showToast('Please enter equipment title.', 'warning');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/assets/onboard`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                leafClass,
                title,
                ownerId: state.activeUser,
                baseDailyRate,
                securityCollateral,
                lockerBay
            })
        });
        const data = await res.json();
        if (data.success) {
            closeModal('onboardModal');
            showToast('✅ ' + data.message, 'success');
            await fetchAllData();
        } else {
            showToast('❌ ' + data.message, 'error');
        }
    } catch (e) {
        showToast('Error onboarding asset: ' + e.message, 'error');
    }
}

// ============================================================================
// UI HELPERS (MODALS, TOASTS, ESCAPING)
// ============================================================================
function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('active');
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('active');
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    let icon = 'ℹ️';
    if (type === 'success') icon = '✅';
    if (type === 'error') icon = '❌';
    if (type === 'warning') icon = '⚠️';

    toast.innerHTML = `<span>${icon}</span><span style="flex:1;">${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(20px)';
        setTimeout(() => toast.remove(), 300);
    }, 4500);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
