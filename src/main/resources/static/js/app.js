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
    selectedBayId: 'A2',
    currentReserveAsset: null
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
document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    initCategoryPills();
    initUserSelector();
    initEventListeners();
    fetchAllData();

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
    select.addEventListener('change', (e) => {
        state.activeUser = e.target.value;
        updateUserUI();
        showToast('Active persona switched to ' + getActiveUserName(), 'info');
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

    // Onboard Form Submission
    document.getElementById('confirmOnboardBtn')?.addEventListener('click', submitOnboardAsset);

    // Reserve Form Submission
    document.getElementById('confirmReserveBtn')?.addEventListener('click', submitReserveAsset);
    document.getElementById('rentalDaysInput')?.addEventListener('input', updateReserveEstimates);

    // Hub & Locker Buttons
    document.getElementById('refreshGridBtn')?.addEventListener('click', () => {
        fetchLockers().then(() => showToast('Smart Hub sensor grid refreshed.', 'success'));
    });
    document.getElementById('pickupFromLockerBtn')?.addEventListener('click', pickupSelectedBayGear);
    document.getElementById('returnToQuarantineBtn')?.addEventListener('click', returnSelectedBayGear);

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
        fetchHealth()
    ]);
}

async function refreshBackgroundData() {
    await Promise.all([
        fetchAssets(true),
        fetchLockers(true),
        fetchAccounts(true),
        fetchWal(true),
        fetchHealth()
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
}

function getActiveUserName() {
    const user = state.users.find(u => u.userId === state.activeUser);
    return user ? user.userName : state.activeUser;
}

// ============================================================================
// TAB 1: EXPLORE MARKETPLACE
// ============================================================================
function getCategoryMeta(leafClass) {
    switch (leafClass) {
        case 'CameraAndOpticsGear':
            return { code: 'CAMERAS', label: 'Cameras & Optics', badgeClass: 'tier-cameras' };
        case 'EventAudioAndLighting':
            return { code: 'AUDIO_LIGHTING', label: 'Audio & Lighting', badgeClass: 'tier-audio' };
        case 'PowerToolEquipment':
            return { code: 'POWER_TOOLS', label: 'Power Tools & DIY', badgeClass: 'tier-tools' };
        case 'EventAndMobilityVehicle':
            return { code: 'MOBILITY_VEHICLES', label: 'Wedding & Mobility Car', badgeClass: 'tier-vehicles' };
        default:
            return { code: 'OTHER', label: leafClass, badgeClass: 'tier-cameras' };
    }
}

function renderCatalog() {
    const grid = document.getElementById('assetsGrid');
    if (!grid) return;

    const query = (document.getElementById('searchInput')?.value || '').toLowerCase();
    const stateFilter = document.getElementById('stateFilter')?.value || 'ALL';

    const filtered = state.assets.filter(a => {
        const catMeta = getCategoryMeta(a.leafClass);
        const matchesQuery = a.title.toLowerCase().includes(query) ||
                             a.leafClass.toLowerCase().includes(query) ||
                             catMeta.label.toLowerCase().includes(query) ||
                             a.id.toLowerCase().includes(query) ||
                             a.lockerCoordinate.toLowerCase().includes(query);

        const matchesCategory = (state.activeCategory === 'ALL') || (catMeta.code === state.activeCategory);
        const matchesState = (stateFilter === 'ALL') || (a.state === stateFilter);

        return matchesQuery && matchesCategory && matchesState;
    });

    if (filtered.length === 0) {
        grid.innerHTML = `
            <div style="grid-column:1/-1; text-align:center; padding:40px; background:#fff; border-radius:var(--radius-lg); border:1px solid var(--slate-200);">
                <p style="font-size:16px; font-weight:700; color:var(--slate-600);">No equipment matching filter criteria.</p>
                <p style="font-size:12px; color:var(--slate-400); margin-top:4px;">Try selecting another category pill or clearing your search term.</p>
            </div>
        `;
        return;
    }

    grid.innerHTML = filtered.map(a => {
        const catMeta = getCategoryMeta(a.leafClass);

        let stateClass = 'state-available';
        let stateLabel = 'Available';
        if (a.state === 'RESERVED_ESCROW') { stateClass = 'state-reserved'; stateLabel = 'Reserved (Escrow)'; }
        if (a.state === 'ACTIVE_LEASE') { stateClass = 'state-active'; stateLabel = 'Out on Rent'; }
        if (a.state === 'INSPECTION_QUARANTINE') { stateClass = 'state-quarantine'; stateLabel = 'In Inspection'; }

        const canRent = a.state === 'AVAILABLE';
        const isGroundBay = a.lockerCoordinate.startsWith('H');

        return `
            <div class="asset-card">
                <div class="asset-card-header">
                    <div>
                        <span class="tier-badge ${catMeta.badgeClass}">${catMeta.label}</span>
                        <div class="asset-id-tag">${a.id} • Host: ${a.ownerId}</div>
                    </div>
                    <span class="state-badge ${stateClass}">${stateLabel}</span>
                </div>
                <div class="asset-card-body">
                    <h3 class="asset-title">${escapeHtml(a.title)}</h3>
                    
                    <ul class="spec-list">
                        <li class="spec-item">
                            <span class="spec-label">Assigned Bay</span>
                            <span class="spec-val" style="color:var(--brand-teal-dark); font-weight:700;">
                                ${isGroundBay ? '🚗 Ground Slot ' + a.lockerCoordinate : '📦 Hub Locker ' + a.lockerCoordinate}
                            </span>
                        </li>
                        <li class="spec-item">
                            <span class="spec-label">Protection Tier</span>
                            <span class="spec-val">All-Risk Verified</span>
                        </li>
                        ${a.ipRating ? `
                        <li class="spec-item">
                            <span class="spec-label">Weather Rating</span>
                            <span class="spec-val">${a.ipRating}</span>
                        </li>` : ''}
                        ${a.soilType ? `
                        <li class="spec-item">
                            <span class="spec-label">Spec Info</span>
                            <span class="spec-val">${a.soilType}</span>
                        </li>` : ''}
                        ${isGroundBay ? `
                        <li class="spec-item" style="grid-column:1/-1;">
                            <span class="spec-val" style="color:var(--warm-amber-dark); font-size:11px;">⚠️ Row H Ground Parking Slot</span>
                        </li>` : ''}
                    </ul>

                    <div class="pricing-row">
                        <div class="rate-box">
                            <h5>Daily Rental Tariff</h5>
                            <div class="rate-amount">${formatInr(a.calculatedTariff)}<span style="font-size:11px; font-weight:500; color:var(--slate-500);">/day</span></div>
                        </div>
                        <div class="rate-box" style="text-align:right;">
                            <h5>Refundable Deposit</h5>
                            <div class="rate-amount" style="color:var(--slate-700);">${formatInr(a.requiredCollateral)}</div>
                        </div>
                    </div>
                </div>
                <div class="asset-card-footer">
                    <span class="locker-pin-badge">📍 Bay ${a.lockerCoordinate}</span>
                    <button class="btn-primary" ${canRent ? '' : 'disabled style="opacity:0.5; cursor:not-allowed;"'} 
                            onclick="openReserveModal('${a.id}')">
                        ${canRent ? '<span>⚡</span> Rent Now' : stateLabel}
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

// ============================================================================
// TAB 2: SMART PICKUP HUB & PARKING GRID (8x8 A1–H8)
// ============================================================================
function renderLockerGrid() {
    const container = document.getElementById('grid8x8Container');
    if (!container) return;

    if (state.lockers.length === 0) {
        container.innerHTML = '<div style="grid-column:1/-1; text-align:center; padding:20px;">Loading 64 pickup hub compartments...</div>';
        return;
    }

    container.innerHTML = state.lockers.map(c => {
        let stateClass = 'bay-available';
        if (c.status === 'ONLINE_READY') stateClass = 'bay-ready';
        if (c.status === 'RESERVED_LOCKED') stateClass = 'bay-reserved';
        if (c.status === 'DISPATCHED_ACTIVE') stateClass = 'bay-active';
        if (c.status === 'QUARANTINE_LOCKED') stateClass = 'bay-quarantine';
        if (c.row === 7 && c.status === 'AVAILABLE') stateClass = 'bay-ground';

        const isSelected = c.bayId === state.selectedBayId ? 'selected' : '';

        let icon = '📦';
        if (c.row === 7) icon = '🚗';
        if (c.status === 'QUARANTINE_LOCKED') icon = '⚠️';
        if (c.status === 'DISPATCHED_ACTIVE') icon = '🚀';

        return `
            <div class="bay-cell ${stateClass} ${isSelected}" onclick="selectBay('${c.bayId}')" title="${c.bayId} - ${c.status}">
                <div class="bay-id-label">${c.bayId}</div>
                <div class="bay-mini-icon">${icon}</div>
            </div>
        `;
    }).join('');
}

function selectBay(bayId) {
    state.selectedBayId = bayId;
    renderLockerGrid();
    updateInspectorDetails();
}

function updateInspectorDetails() {
    const comp = state.lockers.find(c => c.bayId === state.selectedBayId);
    if (!comp) return;

    document.getElementById('inspectorBayId').textContent = 'Bay ' + comp.bayId;
    document.getElementById('inspectorSizeTier').textContent = comp.sizeTier;
    document.getElementById('telTemp').textContent = Number(comp.tempC).toFixed(1);
    document.getElementById('telHum').textContent = Number(comp.humidityPct).toFixed(1);
    document.getElementById('telWeight').textContent = Number(comp.weightKg).toFixed(1);
    document.getElementById('inspectorOtp').textContent = comp.otpPin || '000000';

    const asset = state.assets.find(a => a.id === comp.currentAssetId || a.lockerCoordinate === comp.bayId);
    if (asset) {
        document.getElementById('inspectorAssetTitle').textContent = asset.title;
        document.getElementById('inspectorAssetId').textContent = asset.id + ' • State: ' + asset.state;
    } else {
        document.getElementById('inspectorAssetTitle').textContent = comp.occupied ? 'Occupied Equipment' : 'Empty Compartment';
        document.getElementById('inspectorAssetId').textContent = comp.occupied ? comp.currentAssetId : 'Available for allocation';
    }
}

async function pickupSelectedBayGear() {
    const comp = state.lockers.find(c => c.bayId === state.selectedBayId);
    if (!comp) return;

    const asset = state.assets.find(a => a.lockerCoordinate === comp.bayId && a.state === 'RESERVED_ESCROW');
    if (!asset) {
        showToast('Bay ' + comp.bayId + ' does not have equipment in RESERVED_ESCROW state to unlock.', 'error');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/assets/pickup`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ assetId: asset.id, otpPin: comp.otpPin })
        });
        const data = await res.json();
        if (data.success) {
            showToast('✅ ' + data.message, 'success');
            await fetchAllData();
        } else {
            showToast('❌ ' + data.message, 'error');
        }
    } catch (e) {
        showToast('Network error during pickup: ' + e.message, 'error');
    }
}

async function returnSelectedBayGear() {
    const comp = state.lockers.find(c => c.bayId === state.selectedBayId);
    if (!comp) return;

    const asset = state.assets.find(a => a.lockerCoordinate === comp.bayId && a.state === 'ACTIVE_LEASE');
    if (!asset) {
        showToast('Bay ' + comp.bayId + ' does not have active equipment out on rent to return.', 'error');
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
            showToast('⚠️ MANDATORY QUARANTINE: ' + data.message, 'warning');
            await fetchAllData();
        } else {
            showToast('❌ ' + data.message, 'error');
        }
    } catch (e) {
        showToast('Network error during return: ' + e.message, 'error');
    }
}

// ============================================================================
// TAB 3: ESCROW WALLET, RETURNS & INSPECTION
// ============================================================================
function renderQuarantineDeck() {
    const container = document.getElementById('quarantineCardsGrid');
    if (!container) return;

    const quarantineItems = state.assets.filter(a => a.state === 'INSPECTION_QUARANTINE');

    if (quarantineItems.length === 0) {
        container.innerHTML = `
            <div style="grid-column:1/-1; text-align:center; padding:30px; background:#fff; border-radius:var(--radius-lg); border:1px solid var(--slate-200);">
                <span style="font-size:32px;">🛡️</span>
                <p style="font-size:16px; font-weight:700; color:var(--emerald-dark); margin-top:8px;">Post-Return Inspection Queue Clear</p>
                <p style="font-size:12px; color:var(--slate-500);">All returned items have been verified and security deposits released in ₹ INR.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = quarantineItems.map(a => `
        <div class="quarantine-card">
            <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                <div>
                    <span class="state-badge state-quarantine">POST-RETURN INSPECTION</span>
                    <h3 style="font-family:var(--font-heading); font-size:16px; font-weight:800; margin-top:6px;">${escapeHtml(a.title)}</h3>
                    <div style="font-size:11px; font-family:monospace; color:var(--slate-500);">ID: ${a.id} • Host: ${a.ownerId} • Renter: ${a.renterId}</div>
                </div>
                <div style="font-weight:700; font-size:14px; color:var(--brand-teal-dark);">Bay ${a.lockerCoordinate}</div>
            </div>

            <div style="margin:14px 0; background:var(--slate-50); padding:10px 14px; border-radius:var(--radius-sm); font-size:12px;">
                <div style="display:flex; justify-content:space-between; margin-bottom:4px;">
                    <span>Held Security Deposit in Escrow:</span>
                    <strong style="color:var(--crimson);">${formatInr(a.requiredCollateral)}</strong>
                </div>
                <div style="font-size:11px; color:var(--slate-500);">Latest Log: ${escapeHtml(a.maintenanceLogs[a.maintenanceLogs.length - 1] || 'Returned to hub')}</div>
            </div>

            <!-- Damage Deduction Controller -->
            <div class="damage-slider-box">
                <div style="display:flex; justify-content:space-between; font-size:12px; font-weight:700; margin-bottom:6px;">
                    <span>Damage Deduction:</span>
                    <span id="damageLabel_${a.id}" style="color:var(--crimson);">20% (${formatInr(a.requiredCollateral * 0.20)})</span>
                </div>
                <input type="range" class="damage-slider" id="damageSlider_${a.id}" min="0" max="100" step="5" value="20"
                       oninput="updateDamageSlider('${a.id}', ${a.requiredCollateral})">
            </div>

            <div style="display:grid; grid-template-columns:1fr 1fr; gap:10px; margin-top:14px;">
                <button class="btn-success" onclick="approveInspection('${a.id}')">
                    <span>✅</span> Approve Clean & Refund
                </button>
                <button class="btn-danger" onclick="slashInspection('${a.id}', ${a.requiredCollateral})">
                    <span>⚠️</span> Deduct Repair Cost
                </button>
            </div>
        </div>
    `).join('');
}

function updateDamageSlider(assetId, collateral) {
    const slider = document.getElementById(`damageSlider_${assetId}`);
    const label = document.getElementById(`damageLabel_${assetId}`);
    if (slider && label) {
        const pct = slider.value;
        const penalty = (collateral * (pct / 100.0));
        label.textContent = `${pct}% (${formatInr(penalty)})`;
    }
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

async function slashInspection(assetId, collateral) {
    const slider = document.getElementById(`damageSlider_${assetId}`);
    const damagePct = slider ? parseFloat(slider.value) : 20.0;

    try {
        const res = await fetch(`${API_BASE}/assets/inspect-slash`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ assetId, hostId: state.activeUser, damagePercentage: damagePct })
        });
        const data = await res.json();
        if (data.success) {
            showToast('⚠️ ' + data.message, 'warning');
            await fetchAllData();
        } else {
            showToast('❌ ' + data.message, 'error');
        }
    } catch (e) {
        showToast('Error slashing inspection: ' + e.message, 'error');
    }
}

function renderWalTable() {
    const tbody = document.getElementById('walTableBody');
    if (!tbody) return;

    if (state.wal.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">No WAL ledger entries found.</td></tr>';
        return;
    }

    tbody.innerHTML = state.wal.slice().reverse().map(w => {
        let actionColor = 'var(--brand-teal-dark)';
        if (w.actionType?.includes('BLOCKED') || w.actionType?.includes('SLASH')) actionColor = 'var(--crimson)';
        if (w.actionType === 'RESERVE') actionColor = 'var(--warm-amber-dark)';

        return `
            <tr>
                <td style="font-size:11px; font-family:monospace; color:var(--slate-500);">${escapeHtml(w.timestamp || '')}</td>
                <td style="font-family:monospace; font-weight:700;">${escapeHtml(w.txId || '')}</td>
                <td style="font-family:monospace;">${escapeHtml(w.entityId || '')}</td>
                <td><strong style="color:${actionColor};">${escapeHtml(w.actionType || '')}</strong></td>
                <td><span style="font-size:11px; color:var(--slate-400);">${escapeHtml(w.previousState || '')}</span></td>
                <td><span class="state-badge" style="font-size:10px;">${escapeHtml(w.newState || '')}</span></td>
                <td><strong>${escapeHtml(w.actor || '')}</strong></td>
                <td style="font-size:12px;">${escapeHtml(w.details || '')}</td>
            </tr>
        `;
    }).join('');
}

// ============================================================================
// TAB 4: HOST STUDIO, CLONER & TRUST SCORE
// ============================================================================
function populateHostDropdowns() {
    const cloneSelect = document.getElementById('cloneSourceSelect');
    const metaSelect = document.getElementById('metaAssetSelect');

    const optionsHtml = state.assets.map(a => `
        <option value="${a.id}">${a.title} (${a.id} • ${a.lockerCoordinate})</option>
    `).join('');

    if (cloneSelect) cloneSelect.innerHTML = optionsHtml;
    if (metaSelect) metaSelect.innerHTML = optionsHtml;
}

function updateClonerMemoryPreview() {
    const assetId = document.getElementById('cloneSourceSelect')?.value;
    if (!assetId) return;
    fetch(`${API_BASE}/meta/clone?id=${assetId}`)
        .then(r => r.json())
        .then(cloneData => {
            document.getElementById('shallowLogsId').textContent = cloneData.shallowLogsId;
            document.getElementById('deepLogsId').textContent = cloneData.deepLogsId;
        })
        .catch(console.error);
}

async function submitCloneListing() {
    const assetId = document.getElementById('cloneSourceSelect')?.value;
    const targetBay = document.getElementById('cloneTargetBayInput')?.value?.trim().toUpperCase();

    if (!assetId) {
        showToast('Please select a source listing to clone.', 'error');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/assets/clone`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ assetId, targetBay })
        });
        const data = await res.json();
        if (data.success) {
            showToast(`🧬 ${data.message} Allocated Bay: ${data.targetBay}`, 'success');
            await fetchAllData();
            updateClonerMemoryPreview();
        } else {
            showToast('❌ ' + data.message, 'error');
        }
    } catch (e) {
        showToast('Error cloning listing: ' + e.message, 'error');
    }
}

async function runLiveReflection() {
    const select = document.getElementById('metaAssetSelect');
    if (!select || !select.value) return;
    const assetId = select.value;

    try {
        const resReflect = await fetch(`${API_BASE}/meta/reflect?id=${assetId}`);
        const reflectData = await resReflect.json();

        // 5-Tier Inheritance Chain Display
        const chainEl = document.getElementById('reflectionInheritanceChain');
        chainEl.innerHTML = reflectData.inheritanceChain.map((cls, idx) => `
            <span style="background:var(--slate-100); padding:3px 8px; border-radius:4px; margin-right:4px; display:inline-block;">
                Tier ${idx + 1}: ${cls.split('.').pop()}
            </span>
        `).join(' &rarr; ');

        let details = `// Live Java Reflection Analysis: ${reflectData.className}\n`;
        details += `// Package: ${reflectData.packageName}\n`;
        details += `// Implemented Interfaces: ${reflectData.implementedInterfaces.join(', ') || 'None'}\n\n`;
        details += `--- Declared Fields (${reflectData.declaredFields.length}) ---\n`;
        reflectData.declaredFields.forEach(f => {
            details += ` ${f.modifiers} ${f.type} ${f.name} = "${f.value}";\n`;
        });
        details += `\n--- Declared Methods (${reflectData.declaredMethods.length}) ---\n`;
        reflectData.declaredMethods.forEach(m => {
            details += ` ${m.modifiers} ${m.returnType} ${m.name}(${m.parameters});\n`;
        });
        document.getElementById('reflectionDetailsBox').textContent = details;

        // Also refresh cloner memory preview
        updateClonerMemoryPreview();
    } catch (e) {
        console.error('Reflection error:', e);
    }
}

async function runTrustScoreCalculation() {
    const condition = parseFloat(document.getElementById('sliderCond')?.value || 90.0);
    const punctuality = parseFloat(document.getElementById('sliderPunct')?.value || 95.0);
    const escrow = parseFloat(document.getElementById('sliderEscrow')?.value || 85.0);
    const peer = parseFloat(document.getElementById('sliderPeer')?.value || 90.0);
    const kyc = parseFloat(document.getElementById('sliderKyc')?.value || 95.0);

    // Update label numbers
    document.getElementById('valCond').textContent = condition.toFixed(1);
    document.getElementById('valPunct').textContent = punctuality.toFixed(1);
    document.getElementById('valEscrow').textContent = escrow.toFixed(1);
    document.getElementById('valPeer').textContent = peer.toFixed(1);
    document.getElementById('valKyc').textContent = kyc.toFixed(1);

    try {
        const res = await fetch(`${API_BASE}/trust-score`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                memberName: getActiveUserName(),
                conditionHistory: condition,
                punctualityRate: punctuality,
                escrowCompletion: escrow,
                peerRating: peer,
                kycTier: kyc,
                deltaCondition: 5.0,
                deltaPunctuality: 4.0,
                deltaEscrow: 2.0,
                deltaPeer: 3.0,
                deltaKyc: 0.0
            })
        });
        const data = await res.json();
        document.getElementById('trustCompositeScore').textContent = Number(data.simulatedScore).toFixed(2);
        document.getElementById('trustTierBadge').textContent = data.simulatedTier;
        document.getElementById('trustPolicyText').textContent = data.policyRecommendation;
    } catch (e) {
        console.error('Trust Score calculation error:', e);
    }
}

// ============================================================================
// RESERVATION & ONBOARDING MODALS
// ============================================================================
function openReserveModal(assetId) {
    const asset = state.assets.find(a => a.id === assetId);
    if (!asset) return;

    state.currentReserveAsset = asset;
    document.getElementById('reserveAssetId').value = asset.id;
    document.getElementById('modalAssetTitle').textContent = asset.title;
    document.getElementById('modalAssetBay').textContent = 'Assigned Hub Bay: ' + asset.lockerCoordinate;
    document.getElementById('rentalDaysInput').value = '2';

    updateReserveEstimates();
    openModal('reserveModal');
}

function updateReserveEstimates() {
    if (!state.currentReserveAsset) return;
    const days = parseInt(document.getElementById('rentalDaysInput')?.value || 1, 10);
    const tariff = state.currentReserveAsset.calculatedTariff * days;
    const collateral = state.currentReserveAsset.requiredCollateral;
    const total = tariff + collateral;

    document.getElementById('modalTariffEstimate').textContent = formatInr(tariff);
    document.getElementById('modalCollateralEstimate').textContent = formatInr(collateral);
    document.getElementById('modalTotalEscrow').textContent = formatInr(total);
}

async function submitReserveAsset() {
    const assetId = document.getElementById('reserveAssetId')?.value;
    const days = parseInt(document.getElementById('rentalDaysInput')?.value || 1, 10);

    try {
        const res = await fetch(`${API_BASE}/assets/reserve`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                assetId,
                renterId: state.activeUser,
                rentalDays: days
            })
        });
        const data = await res.json();

        if (data.success) {
            closeModal('reserveModal');
            showToast(`🎉 ${data.message} Pickup OTP: ${data.otpPin}`, 'success');
            await fetchAllData();
        } else {
            // 4-Gate Fail-Fast Abort Diagnostic
            showToast(`🚫 Gate ${data.gateIndex} (${data.gateName}) Failed: ${data.message}`, 'error');
        }
    } catch (e) {
        showToast('Error reserving asset: ' + e.message, 'error');
    }
}

async function submitOnboardAsset() {
    const leafClass = document.getElementById('onboardLeafClass')?.value;
    const title = document.getElementById('onboardTitle')?.value?.trim();
    const baseDailyRate = parseFloat(document.getElementById('onboardRate')?.value || 1200);
    const securityCollateral = parseFloat(document.getElementById('onboardCollateral')?.value || 5000);
    const lockerBay = document.getElementById('onboardBay')?.value?.trim().toUpperCase();

    if (!title) {
        showToast('Equipment title is required.', 'error');
        return;
    }
    if (!lockerBay) {
        showToast('Target hub bay is required.', 'error');
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
