/**
 * RentSphere: Local P2P Rental & Smart Pickup Hub Application Engine
 * Goa University - MCA Object-Oriented Technology Lab
 * Currency: Indian Rupees (₹ INR) | Local Community Scale
 */

const API_BASE = '/api';

// Application State Store
const state = {
    activeUser: 'renter_deepak',
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
    initEventListeners();

    // Auto-login default persona so session cookie and token match active user
    await executeLogin(state.activeUser, 'RentSphere@2026');
    await fetchAllData();

    // Auto-refresh data every 8 seconds to reflect background flat-file state
    setInterval(refreshBackgroundData, 8000);
});

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
        state.activeUser = e.target.value;
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
    const user = state.users.find(u => u.userId === state.activeUser);
    if (!user) return;

    document.getElementById('headerWalletBalance').textContent = Number(user.walletBalance).toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
    const badge = document.getElementById('userRoleBadge');
    badge.textContent = user.role;
    badge.className = 'role-tag role-' + user.role.toLowerCase();

    const authBtn = document.getElementById('authButtonLabel');
    if (authBtn) authBtn.textContent = user.userName.split(' ')[0] + ' (' + user.role + ')';
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

    let specificPills = '';
    if (a.indoorOutdoorRating) specificPills += `<span class="spec-pill">${escapeHtml(a.indoorOutdoorRating)}</span>`;
    if (a.powerOrEngineSpec) specificPills += `<span class="spec-pill">${escapeHtml(a.powerOrEngineSpec)}</span>`;
    if (a.wattageOrBatterySpec) specificPills += `<span class="spec-pill">${escapeHtml(a.wattageOrBatterySpec)}</span>`;
    if (a.safetyCertification) specificPills += `<span class="spec-pill is-safety">✓ ${escapeHtml(a.safetyCertification)}</span>`;

    return `
        <div class="asset-card">
            <div class="asset-card-top">
                <span class="cat-pill-badge">${categoryName}</span>
                <span class="state-pill ${stateBadgeClass}">${formatStateName(a.state)}</span>
            </div>

            <h3 class="asset-title">${escapeHtml(a.title)}</h3>
            <div class="asset-meta-row">
                <span>📍 Bay: <strong>${a.lockerCoordinate}</strong></span>
                <span>Host: <strong>${escapeHtml(a.ownerId)}</strong></span>
            </div>

            <div class="specs-pills-row">
                ${specificPills}
            </div>

            <div class="price-container">
                <div class="price-block">
                    <span class="price-label">Daily Rental Rate</span>
                    <span class="price-val">${formatInr(a.calculatedTariff)}</span>
                </div>
                <div class="price-block">
                    <span class="price-label">Refundable Deposit</span>
                    <span class="price-val">${formatInr(a.requiredCollateral)}</span>
                </div>
            </div>

            <button class="btn-primary reserve-card-btn" data-id="${a.id}" ${isAvail ? '' : 'disabled'} style="width:100%; justify-content:center; margin-top:14px;">
                <span>${isAvail ? '🛒 Rent & Lock Escrow' : '🔒 Currently Unavailable'}</span>
            </button>
        </div>
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
// TAB 2: 8x8 SMART HUB & PARKING MATRIX
// ============================================================================
function renderLockerGrid() {
    const container = document.getElementById('grid8x8Container');
    if (!container) return;

    if (state.lockers.length === 0) {
        container.innerHTML = '<div style="padding:20px;">Loading 64 Hub Compartments...</div>';
        return;
    }

    container.innerHTML = state.lockers.map(c => {
        const isGround = c.row === 8;
        const isSelected = c.bayId === state.selectedBayId;
        const cellClass = getLockerCellClass(c.status, isGround);

        return `
            <div class="bay-cell ${cellClass} ${isSelected ? 'selected' : ''}" data-bay="${c.bayId}">
                <span class="bay-id">${c.bayId}</span>
                <span class="bay-state-text">${formatLockerStatus(c.status)}</span>
                ${isGround ? '<span style="font-size:8px; font-weight:800; color:var(--purple);">GROUND SLOT</span>' : ''}
            </div>
        `;
    }).join('');

    container.querySelectorAll('.bay-cell').forEach(cell => {
        cell.addEventListener('click', () => {
            state.selectedBayId = cell.getAttribute('data-bay');
            renderLockerGrid();
            updateInspectorDetails();
        });
    });
}

function getLockerCellClass(status, isGround) {
    if (isGround) return 'cell-ground';
    if (status === 'ONLINE_READY' || status === 'AVAILABLE') return 'cell-ready';
    if (status === 'RESERVED_LOCKED') return 'cell-reserved';
    if (status === 'DISPATCHED_ACTIVE') return 'cell-active';
    if (status === 'QUARANTINE_LOCKED') return 'cell-quarantine';
    return 'cell-ready';
}

function formatLockerStatus(st) {
    if (st === 'ONLINE_READY' || st === 'AVAILABLE') return 'READY';
    if (st === 'RESERVED_LOCKED') return 'LOCKED';
    if (st === 'DISPATCHED_ACTIVE') return 'VACATED';
    if (st === 'QUARANTINE_LOCKED') return 'HOLD';
    return st;
}

function updateInspectorDetails() {
    const comp = state.lockers.find(c => c.bayId === state.selectedBayId);
    if (!comp) return;

    document.getElementById('inspectorBayId').textContent = 'Bay ' + comp.bayId;
    const tierBadge = document.getElementById('inspectorSizeTier');
    tierBadge.textContent = comp.sizeTier.replace('_', ' ');

    document.getElementById('telTemp').textContent = comp.tempC.toFixed(1);
    document.getElementById('telHum').textContent = comp.humidityPct.toFixed(1);
    document.getElementById('telWeight').textContent = comp.weightKg.toFixed(1);

    const asset = state.assets.find(a => a.id === comp.currentAssetId || a.lockerCoordinate === comp.bayId);
    if (asset) {
        document.getElementById('inspectorAssetTitle').textContent = asset.title;
        document.getElementById('inspectorAssetId').textContent = asset.id + ' • ' + formatStateName(asset.state);
    } else {
        document.getElementById('inspectorAssetTitle').textContent = 'Empty / Available Slot';
        document.getElementById('inspectorAssetId').textContent = 'Ready for assignment';
    }

    // OTP Pin display: masked if empty (OWASP CWE-200 defense)
    const otpVal = document.getElementById('inspectorOtp');
    otpVal.textContent = comp.otpPin && comp.otpPin.length === 6 ? comp.otpPin : '••••••';
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
        return `
            <div class="quarantine-card ${isDisputed ? 'disputed-card' : ''}">
                <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                    <div>
                        <span class="cat-pill-badge">${getCategoryDisplayName(a.leafClass)}</span>
                        <h4 style="font-family:var(--font-heading); font-size:16px; font-weight:800; margin-top:6px;">${escapeHtml(a.title)}</h4>
                        <div style="font-size:12px; color:var(--slate-500);">Bay: <strong>${a.lockerCoordinate}</strong> • Renter: <strong>${escapeHtml(a.renterId)}</strong></div>
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
                    <div style="display:flex; flex-direction:column; gap:10px;">
                        <button class="btn-primary approve-inspection-btn" data-id="${a.id}" style="justify-content:center;">
                            <span>✅</span> Approve Clean Return (100% Deposit Refund)
                        </button>
                        <button class="btn-danger slash-inspection-btn" data-id="${a.id}" style="justify-content:center;">
                            <span>⚠️</span> Flag 20% Damage Penalty
                        </button>
                        <button class="btn-secondary contest-dispute-btn" data-id="${a.id}" data-collateral="${a.requiredCollateral}" data-title="${escapeHtml(a.title)}" style="justify-content:center; font-size:12px;">
                            <span>⚖️</span> Contest Claim / File Formal Dispute
                        </button>
                    </div>
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
        const res = await fetch(`${API_BASE}/admin/device/state`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
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
