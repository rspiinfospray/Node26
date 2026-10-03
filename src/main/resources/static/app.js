const state = { worlds: [], world: null, liveWorld: null, events: [], selectedPeonId: null, source: null, timelineTimer: null, historical: false, defaults: null, defaultZoom: 1.7, zoom: 1.7, panX: 0, panY: 0, dragging: false, dragStart: null, dragMoved: false };
const $ = selector => document.querySelector(selector);
const canvas = $('#worldCanvas');
const context = canvas.getContext('2d');
const assets = { grass: loadImage('/assets/terrain-grass.png'), rock: loadImage('/assets/terrain-rock.png'), tree: loadImage('/assets/terrain-tree.png'), food: loadImage('/assets/food-cache.png'), peon: loadImage('/assets/peon-topdown.png') };
Promise.all(Object.values(assets).map(asset => asset.ready)).then(render);

function loadImage(source) {
    const image = new Image();
    const ready = new Promise(resolve => { image.onload = resolve; image.onerror = resolve; });
    image.src = source;
    return { image, ready };
}

function resetView() {
    state.zoom = state.defaultZoom; state.panX = 0; state.panY = 0;
    if ($('#zoomValue')) $('#zoomValue').textContent = `${Math.round(state.zoom * 100)}%`;
}

function changeZoom(delta) {
    state.zoom = Math.max(1, Math.min(8, state.zoom + delta));
    $('#zoomValue').textContent = `${Math.round(state.zoom * 100)}%`;
    drawWorld();
}

async function api(path, options = {}) {
    const response = await fetch(path, { headers: { 'Content-Type': 'application/json' }, ...options });
    if (!response.ok) { const body = await response.json().catch(() => ({})); throw new Error(body.error || `Erreur HTTP ${response.status}`); }
    const text = await response.text();
    return text ? JSON.parse(text) : null;
}

async function loadWorlds(selectLatest = false) {
    state.worlds = await api('/api/worlds');
    $('#worldSelect').innerHTML = state.worlds.map(world => `<option value="${world.id}">${escapeHtml(world.name || `Monde ${world.id.slice(0, 8)}`)} · ${world.status}</option>`).join('');
    renderWorldTemplates();
    if (state.worlds.length) {
        const wanted = selectLatest || !state.world ? state.worlds[0].id : state.world.id;
        $('#worldSelect').value = wanted;
        await selectWorld(wanted);
    } else { render(); }
}

function renderWorldTemplates() {
    const select = $('#worldTemplate');
    if (!select) return;
    const selected = select.value;
    select.innerHTML = '<option value="">Configuration par défaut</option>' + state.worlds.map(world => `<option value="${world.id}">${escapeHtml(world.name || `Monde ${world.id.slice(0, 8)}`)}</option>`).join('');
    if (state.worlds.some(world => world.id === selected)) select.value = selected;
}

function applyCreationParameters(source) {
    const form = $('#createForm');
    const values = source ? {
        width:source.width, height:source.height, rockPercentage:source.rockPercentage, treePercentage:source.treePercentage,
        foodCellPercentage:source.foodCellPercentage, minFoodPerCell:source.minFoodPerCell, maxFoodPerCell:source.maxFoodPerCell,
        maxRounds:source.maxRounds, hungerHealthLossPerTurn:source.hungerHealthLossPerTurn
    } : {
        width:state.defaults.worldWidth, height:state.defaults.worldHeight, rockPercentage:state.defaults.rockPercentage,
        treePercentage:state.defaults.treePercentage, foodCellPercentage:state.defaults.foodCellPercentage,
        minFoodPerCell:state.defaults.minFoodPerCell, maxFoodPerCell:state.defaults.maxFoodPerCell,
        maxRounds:state.defaults.maxRounds, hungerHealthLossPerTurn:state.defaults.hungerHealthLossPerTurn
    };
    Object.entries(values).forEach(([name, value]) => { if (form.elements[name]) form.elements[name].value = value; });
    const teams = source ? Object.values(source.teams || {}) : [];
    const peons = source ? Object.values(source.peons || {}) : [];
    form.elements.redName.value = teams[0]?.name || 'Rouges';
    form.elements.redCount.value = teams[0] ? peons.filter(peon => peon.teamId === teams[0].id).length : 8;
    form.elements.blueName.value = teams[1]?.name || 'Bleus';
    form.elements.blueCount.value = teams[1] ? peons.filter(peon => peon.teamId === teams[1].id).length : 8;
}

async function selectWorld(worldId) {
    if (!worldId) return;
    closeStream();
    state.liveWorld = await api(`/api/worlds/${worldId}`);
    state.world = state.liveWorld;
    state.events = await api(`/api/worlds/${worldId}/events?limit=500`);
    state.historical = false;
    resetView();
    state.selectedPeonId = Object.values(state.world.peons).find(peon => peon.alive)?.id || null;
    connectStream(worldId);
    render();
}

function connectStream(worldId) {
    state.source = new EventSource(`/api/worlds/${worldId}/stream`);
    state.source.addEventListener('simulation-event', async event => {
        const data = JSON.parse(event.data);
        state.events.push(data);
        if (state.events.length > 500) state.events.shift();
        if (['PEON_TURN_COMPLETED', 'WORLD_FINISHED'].includes(data.eventType)) {
            state.liveWorld = await api(`/api/worlds/${worldId}`);
            if (!state.historical) state.world = state.liveWorld;
        }
        render();
    });
}

function closeStream() { if (state.source) state.source.close(); state.source = null; }

async function changeStatus(action) {
    if (!state.liveWorld) return;
    state.liveWorld = await api(`/api/worlds/${state.liveWorld.id}/${action}`, { method: 'POST' });
    if (!state.historical) state.world = state.liveWorld;
    render();
}

async function showSequence(sequence) {
    if (!state.liveWorld) return;
    const current = state.liveWorld.sequenceNumber;
    if (Number(sequence) >= current) {
        state.historical = false;
        state.world = state.liveWorld;
    } else {
        state.historical = true;
        state.world = await api(`/api/worlds/${state.liveWorld.id}?sequence=${sequence}`);
    }
    render();
}

function render() {
    const world = state.world;
    $('#emptyState').hidden = Boolean(world);
    if (!world) {
        $('#analysisButton').disabled = true;
        $('#openWorldJournalButton').disabled = true;
        const rect=resizeCanvas(); context.clearRect(0,0,rect.width,rect.height);
        $('#statusBadge').textContent = 'État : —';
        $('#roundValue').textContent = '—';
        $('#sequenceValue').textContent = '—';
        $('#aliveValue').textContent = '—';
        $('#deadValue').textContent = '—';
        $('#teamPopulationStats').replaceChildren();
        $('#playButton').textContent = '▶ Démarrer';
        $('#playButton').disabled = true;
        $('#stepButton').disabled = true;
        $('#timelineSlider').max = 0;
        $('#timelineSlider').value = 0;
        $('#timelineLabel').textContent = 'Séquence consultée : 0 / 0';
        $('#timelinePlayButton').disabled = true;
        $('#liveButton').disabled = true;
        $('#peonEmpty').hidden = false;
        $('#peonDetails').hidden = true;
        $('#eventCount').textContent = '0';
        $('#eventLog').innerHTML = '';
        return;
    }
    $('#analysisButton').disabled = false;
    $('#openWorldJournalButton').disabled = false;
    $('#statusBadge').textContent = `État : ${state.historical ? 'Archive' : friendlyWorldStatus(world.status)}`;
    $('#roundValue').textContent = `${world.currentRound}/${world.maxRounds}`;
    $('#sequenceValue').textContent = world.sequenceNumber;
    renderPopulationStatistics(world);
    $('#playButton').textContent = state.liveWorld?.status === 'RUNNING' ? 'Ⅱ Mettre en pause' : state.liveWorld?.status === 'PAUSED' ? '▶ Reprendre' : '▶ Démarrer';
    $('#playButton').disabled = state.liveWorld?.status === 'FINISHED';
    $('#stepButton').disabled = state.liveWorld?.status === 'FINISHED' || state.liveWorld?.status === 'RUNNING';
    $('#timelineSlider').max = state.liveWorld?.sequenceNumber || 0;
    $('#timelineSlider').value = world.sequenceNumber;
    $('#timelineLabel').textContent = `Séquence consultée : ${world.sequenceNumber} / ${state.liveWorld?.sequenceNumber || 0}`;
    $('#timelinePlayButton').disabled = (state.liveWorld?.sequenceNumber || 0) === 0;
    $('#liveButton').disabled = !state.historical;
    drawWorld();
    renderPeon();
    renderEvents();
}

function renderPopulationStatistics(world) {
    const peons = Object.values(world.peons);
    $('#aliveValue').textContent = peons.filter(peon => peon.alive).length;
    $('#deadValue').textContent = peons.filter(peon => !peon.alive).length;
    const container = $('#teamPopulationStats');
    container.replaceChildren();
    Object.values(world.teams).forEach(team => {
        const teamPeons = peons.filter(peon => peon.teamId === team.id);
        const alive = teamPeons.filter(peon => peon.alive).length;
        const item = document.createElement('span');
        item.className = 'team-population';
        item.style.setProperty('--team-color', team.color || '#999');
        item.textContent = `${team.name} : ${alive} vivants / ${teamPeons.length - alive} morts`;
        container.appendChild(item);
    });
}

function friendlyWorldStatus(status) { return ({ CREATED:'Prêt', RUNNING:'En cours', PAUSED:'En pause', FINISHED:'Terminé' })[status] || status; }

function resizeCanvas() {
    const ratio = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = Math.max(1, Math.round(rect.width * ratio));
    canvas.height = Math.max(1, Math.round(rect.height * ratio));
    context.setTransform(ratio, 0, 0, ratio, 0, 0);
    return rect;
}

function geometry(world, rect) {
    const sqrt3 = Math.sqrt(3);
    const availableWidth = rect.width - 40;
    const availableHeight = rect.height - 40;
    const coordinates = Object.values(world.cells).map(cell => cell.coordinate);
    const rawX = coordinates.map(coordinate => 1.5 * coordinate.q);
    const rawY = coordinates.map(coordinate => sqrt3 * (coordinate.r + coordinate.q / 2));
    const bounds = { minX:Math.min(...rawX), maxX:Math.max(...rawX), minY:Math.min(...rawY), maxY:Math.max(...rawY) };
    const byWidth = availableWidth / (bounds.maxX - bounds.minX + 2);
    const byHeight = availableHeight / (bounds.maxY - bounds.minY + 2);
    const size = Math.max(2, Math.min(14, byWidth, byHeight)) * state.zoom;
    const mapWidth = size * (bounds.maxX - bounds.minX + 2);
    const mapHeight = size * (bounds.maxY - bounds.minY + 2);
    return { size, offsetX:(rect.width-mapWidth)/2+size-bounds.minX*size+state.panX, offsetY:(rect.height-mapHeight)/2+size-bounds.minY*size+state.panY };
}

function centerOf(coordinate, geo) { return { x: geo.offsetX + geo.size * 1.5 * coordinate.q, y: geo.offsetY + geo.size * Math.sqrt(3) * (coordinate.r + coordinate.q / 2) }; }
function hexPath(x, y, size) { context.beginPath(); for (let i = 0; i < 6; i++) { const angle = Math.PI / 180 * (60 * i); const px = x + size * Math.cos(angle); const py = y + size * Math.sin(angle); i ? context.lineTo(px, py) : context.moveTo(px, py); } context.closePath(); }

function drawWorld() {
    const world = state.world;
    const rect = resizeCanvas();
    context.clearRect(0, 0, rect.width, rect.height);
    if (!world) return;
    const geo = geometry(world, rect);
    const selected = world.peons[state.selectedPeonId];
    const mental = $('#mentalMapToggle').checked && selected ? selected.mentalMap : null;
    Object.values(world.cells).forEach(cell => {
        const known = !mental || mental[`${cell.coordinate.q}:${cell.coordinate.r}`];
        const rendered = mental && known ? { ...cell, terrain: known.terrain, foodQuantity: known.rememberedFoodQuantity, occupantPeonIds: known.rememberedOccupants } : cell;
        const point = centerOf(cell.coordinate, geo);
        if (!known) { hexPath(point.x, point.y, geo.size - .35); context.fillStyle = '#0c0f0c'; context.fill(); }
        else { drawTexturedCell(rendered, point, geo); }
        context.strokeStyle = !known ? '#151915' : '#73806955'; context.lineWidth = .6; context.stroke();
        if (known && rendered.foodQuantity > 0) { drawFood(rendered, point, geo); }
    });
    if (!mental) Object.values(world.peons).filter(peon => peon.alive).forEach(peon => drawPeon(peon, world, geo));
    else if (selected) {
        drawRememberedPeons(selected, world, geo);
        drawPeon(selected, world, geo);
    }
    canvas._geometry = geo;
}

function rememberedPeonPresentations(selected) {
    const newestByPeon = new Map();
    Object.values(selected.mentalMap || {}).forEach(memory => {
        const observations = memory.rememberedPeons?.length
            ? memory.rememberedPeons
            : (memory.rememberedOccupants || []).map(peonId => ({ peonId, position:memory.coordinate, observedAtSequence:memory.lastObservedAtSequence, relation:'UNKNOWN' }));
        observations.filter(observation => observation.peonId !== selected.id).forEach(observation => {
            const previous = newestByPeon.get(observation.peonId);
            if (!previous || observation.observedAtSequence > previous.observedAtSequence) newestByPeon.set(observation.peonId, observation);
        });
    });
    return [...newestByPeon.values()];
}

function drawRememberedPeons(selected, world, geo) {
    const groups = new Map();
    rememberedPeonPresentations(selected).forEach(memory => {
        const key = `${memory.position.q}:${memory.position.r}`;
        groups.set(key, [...(groups.get(key) || []), memory]);
    });
    groups.forEach(memories => memories.forEach((memory, index) => {
        const peon = world.peons[memory.peonId];
        if (peon) drawPeon(peon, world, geo, { position:memory.position, remembered:true, relation:memory.relation, index, total:memories.length });
    }));
}

function drawTexturedCell(cell, point, geo) {
    const asset = cell.terrain === 'ROCK' ? assets.rock.image : cell.terrain === 'TREE' ? assets.tree.image : assets.grass.image;
    hexPath(point.x, point.y, geo.size - .35);
    context.save(); context.clip();
    if (asset.complete && asset.naturalWidth) {
        const crop = Math.min(360, asset.naturalWidth, asset.naturalHeight);
        const hash = Math.abs((cell.coordinate.q * 73856093) ^ (cell.coordinate.r * 19349663));
        const sx = hash % Math.max(1, asset.naturalWidth - crop);
        const sy = Math.floor(hash / 97) % Math.max(1, asset.naturalHeight - crop);
        context.drawImage(asset, sx, sy, crop, crop, point.x - geo.size, point.y - geo.size, geo.size * 2, geo.size * 2);
        context.fillStyle = cell.terrain === 'ROCK' ? '#1112' : cell.terrain === 'TREE' ? '#07180720' : '#17320d18'; context.fillRect(point.x - geo.size, point.y - geo.size, geo.size * 2, geo.size * 2);
    } else {
        context.fillStyle = cell.terrain === 'ROCK' ? '#4a4d48' : cell.terrain === 'TREE' ? '#254425' : '#526847'; context.fillRect(point.x - geo.size, point.y - geo.size, geo.size * 2, geo.size * 2);
    }
    context.restore(); hexPath(point.x, point.y, geo.size - .35);
}

function drawFood(cell, point, geo) {
    const image = assets.food.image;
    const quantityScale = .72 + Math.min(cell.foodQuantity, 4) * .08;
    const size = geo.size * quantityScale;
    if (image.complete && image.naturalWidth) { context.drawImage(image, point.x - size / 2, point.y - size / 2, size, size); }
    else { context.beginPath(); context.fillStyle = '#f1c75b'; context.arc(point.x, point.y, Math.max(1.2, geo.size * .18), 0, Math.PI * 2); context.fill(); }
    if (geo.size > 10) { context.fillStyle = '#fff'; context.font = `700 ${Math.max(7, geo.size * .32)}px sans-serif`; context.textAlign = 'center'; context.fillText(cell.foodQuantity, point.x + size * .32, point.y + size * .34); }
}

function drawPeon(peon, world, geo, presentation = {}) {
    context.save();
    if (presentation.remembered) context.globalAlpha = .48;
    const position = presentation.position || peon.position;
    const point = centerOf(position, geo);
    const team = world.teams[peon.teamId];
    const sameCell = Object.values(world.peons).filter(other => other.alive && other.position.q === position.q && other.position.r === position.r);
    const index = presentation.index ?? sameCell.findIndex(other => other.id === peon.id);
    const total = presentation.total ?? sameCell.length;
    const angle = Math.max(0, index) * Math.PI * 2 / Math.max(1, total);
    const spread = Math.min(geo.size * .35, 5);
    const x = point.x + Math.cos(angle) * spread;
    const y = point.y + Math.sin(angle) * spread;
    const spriteHeight = Math.max(6, geo.size * 1.28); const spriteWidth = spriteHeight * .72;
    context.beginPath(); context.ellipse(x, y + spriteHeight * .25, spriteWidth * .43, spriteHeight * .28, 0, 0, Math.PI * 2); context.fillStyle = `${team?.color || '#ddd'}cc`; context.fill();
    if (!presentation.remembered && peon.id === state.selectedPeonId) { context.lineWidth = Math.max(1, geo.size * .09); context.strokeStyle = '#fff'; context.stroke(); }
    if (presentation.remembered) { context.setLineDash([2, 2]); context.lineWidth = Math.max(1, geo.size * .07); context.strokeStyle = presentation.relation === 'ENEMY' ? '#ff6666' : presentation.relation === 'ALLY' ? '#7fdb86' : '#f4d58a'; context.stroke(); context.setLineDash([]); }
    const image = assets.peon.image;
    if (image.complete && image.naturalWidth) { context.drawImage(image, x - spriteWidth / 2, y - spriteHeight * .62, spriteWidth, spriteHeight); }
    else { context.beginPath(); context.arc(x, y, Math.max(2, geo.size * .28), 0, Math.PI * 2); context.fillStyle = team?.color || '#eee'; context.fill(); }
    if (!presentation.remembered && geo.size > 9) { const barWidth = spriteWidth; context.fillStyle = '#180d0d'; context.fillRect(x - barWidth / 2, y - spriteHeight * .7, barWidth, 2); context.fillStyle = '#df5b5b'; context.fillRect(x - barWidth / 2, y - spriteHeight * .7, barWidth * peon.healthPoints / peon.maxHealthPoints, 2); }
    context.restore();
}

function renderPeon() {
    const peon = state.world?.peons[state.selectedPeonId];
    $('#peonEmpty').hidden = Boolean(peon);
    $('#peonDetails').hidden = !peon;
    if (!peon) return;
    const team = state.world.teams[peon.teamId];
    $('#peonName').textContent = peon.firstName;
    $('#peonTeam').textContent = `${team?.name || 'Sans équipe'} · ${peon.alive ? 'vivant' : 'mort'}`;
    $('#peonAvatar').style.background = `${team?.color || '#777'} url('/assets/peon-topdown.png') center 22% / 135% auto no-repeat`;
    $('#peonAvatar').style.borderColor = team?.color || '#aaa';
    $('#healthBar').style.width = `${peon.healthPoints * 100 / peon.maxHealthPoints}%`;
    $('#healthValue').textContent = `${peon.healthPoints}/${peon.maxHealthPoints}`;
    $('#xpBar').style.width = `${peon.experiencePoints % peon.experiencePerLevel * 100 / peon.experiencePerLevel}%`;
    $('#xpValue').textContent = `${peon.experiencePoints} XP`;
    $('#levelValue').textContent = peon.level;
    $('#damageValue').textContent = peon.attackDamage;
    $('#positionValue').textContent = `${peon.position.q}, ${peon.position.r}`;
    $('#memoryValue').textContent = Object.keys(peon.mentalMap || {}).length;
    renderPersonality(peon);
    renderPeonActions(peon);
    $('#peonId').textContent = peon.id;
}

function renderPersonality(peon) {
    const personality = peon.personality || { prudence:50, aggressiveness:50, curiosity:50, solidarity:50, riskAppetite:50 };
    const traits = [['Prudence', personality.prudence], ['Agressivité', personality.aggressiveness], ['Curiosité', personality.curiosity], ['Solidarité', personality.solidarity], ['Goût du risque', personality.riskAppetite]];
    $('#personalityTraits').innerHTML = traits.map(([label, value]) => `<div class="personality-trait"><span>${label}</span><div><i style="width:${Math.max(0, Math.min(100, value))}%"></i></div><strong>${value}</strong></div>`).join('');
    const history = peon.actionHistory || [];
    const learned = Object.values(peon.learnedActions || {});
    const successes = learned.reduce((sum, item) => sum + (item.successes || 0), 0);
    const failures = learned.reduce((sum, item) => sum + (item.failures || 0), 0);
    $('#learningSummary').textContent = `${history.length} expérience(s) mémorisée(s) · ${successes} leçon(s) positive(s) · ${failures} erreur(s)`;
}

function renderPeonActions(peon) {
    const actions = visibleEvents().filter(event => event.peonId === peon.id && event.eventType === 'PEON_DECISION_MADE').slice(-8).reverse();
    $('#peonActionCount').textContent = actions.length;
    $('#peonActionLog').innerHTML = actions.length
        ? actions.map(event => { const candidates = event.payload?.candidates || []; const alternatives = candidates.slice(0, 3).map(candidate => `${friendlyAction(candidate.actionType)} ${Number(candidate.score).toFixed(1)}`).join(' · '); return `<article class="peon-action"><strong>${friendlyAction(event.payload?.actionType)} <em>score ${Number(event.payload?.selectedScore ?? 0).toFixed(1)}</em></strong><span>Tour ${event.roundNumber} · action n° ${event.sequenceNumber}${event.payload?.explorationChoice ? ' · expérimentation' : ''}</span><p>${eventDescription(event)}</p>${alternatives ? `<small>Options : ${alternatives}</small>` : ''}</article>`; }).join('')
        : '<p class="muted action-empty">Aucune action à cette étape de la simulation.</p>';
}

function renderEvents() {
    const events = visibleEvents().slice(-500).reverse();
    $('#eventCount').textContent = events.length;
    $('#eventLog').innerHTML = events.map(event => {
        const actor = peonName(event.peonId) || 'Monde';
        return `<article class="event"><strong>${friendlyEvent(event.eventType)}</strong><span>Tour ${event.roundNumber} · action n° ${event.sequenceNumber} · étape ${event.eventIndex} · ${actor}</span><p>${eventDescription(event)}</p></article>`;
    }).join('');
}

function visibleEvents() { return state.events.filter(event => event.sequenceNumber <= (state.world?.sequenceNumber ?? 0)); }
function peonName(peonId) { return peonId ? state.world?.peons?.[peonId]?.firstName || state.liveWorld?.peons?.[peonId]?.firstName || `Peon ${peonId.slice(0, 8)}` : null; }
function friendlyAction(type) { return ({ VOIR:'Observer', MANGER:'Manger', SE_DEPLACER:'Se déplacer', ATTAQUER:'Attaquer', COMMUNIQUER:'Communiquer', NE_RIEN_FAIRE:'Ne rien faire' })[type] || type || 'Action inconnue'; }
function friendlyPurpose(purpose) { return ({ SURVIVRE:'survivre', GAGNER_DES_NIVEAUX:'gagner des niveaux', AIDER_LES_ALLIES:'aider les autres peons' })[purpose] || purpose?.replaceAll('_', ' ').toLowerCase(); }
function friendlyReason(reason) { return ({ HUNGER:'faim', ATTACK:'attaque', MOVEMENT:'coût du déplacement', NO_FOOD:'aucune nourriture sur la case', INVALID_DESTINATION:'destination inaccessible', INVALID_TARGET:'cible invalide', NO_ALLY_IN_RANGE:'aucun allié sur la case', PEON_NOT_ALIVE:'le peon est mort', NO_SURVIVORS:'aucun survivant', MAX_ROUNDS_REACHED:'nombre maximal de tours atteint' })[reason] || reason?.replaceAll('_', ' ').toLowerCase(); }
function coordinate(value) { return value ? `(${value.q}, ${value.r})` : ''; }
function eventDescription(event) {
    const payload = event.payload || {};
    switch (event.eventType) {
        case 'PEON_DECISION_MADE': return `${friendlyAction(payload.actionType)} pour ${friendlyPurpose(payload.purpose) || 'poursuivre son objectif'}${payload.reason ? ` — ${payload.reason}` : ''}.`;
        case 'PEON_MOVED': return `Déplacement de ${coordinate(payload.from)} vers ${coordinate(payload.to)}.`;
        case 'PEON_SAW': return `${payload.visibleCells ?? 0} case(s) visible(s) autour du peon.`;
        case 'FOOD_CONSUMED': return `Nourriture en ${coordinate(payload.position)} : ${payload.quantityBefore} → ${payload.quantityAfter}.`;
        case 'PEON_ATE': return `PV : ${payload.healthBefore} → ${payload.healthAfter}.`;
        case 'PEON_ATTACKED': return `${peonName(event.peonId)} attaque ${peonName(payload.targetPeonId)} : ${payload.damage} dégâts, PV ${payload.healthBefore} → ${payload.healthAfter}.`;
        case 'PEON_EXPERIENCE_GAINED': return `+${payload.amount} XP, total : ${payload.experienceAfter} XP.`;
        case 'PEON_LEVELED_UP': return `Niveau ${payload.levelBefore} → ${payload.levelAfter} · PV max ${payload.maxHealthBefore} → ${payload.maxHealthAfter} · dégâts ${payload.attackDamageBefore} → ${payload.attackDamageAfter}.`;
        case 'PEON_HUNGER_APPLIED': return `Faim : −${payload.loss} PV, ${payload.healthBefore} → ${payload.healthAfter}.`;
        case 'PEON_MOVEMENT_COST_APPLIED': return `Effort du déplacement : −${payload.loss} PV, ${payload.healthBefore} → ${payload.healthAfter}.`;
        case 'PEON_DIED': return `Mort causée par ${friendlyReason(payload.reason)} en ${coordinate(payload.position)}.`;
        case 'PEON_COMMUNICATED': return `Souvenirs partagés avec ${payload.allyCount} allié(s).`;
        case 'PEON_IDLED': return 'Le peon reste sur place.';
        case 'PEON_ACTION_REJECTED': return `${friendlyAction(payload.actionType)} impossible : ${friendlyReason(payload.reason)}.`;
        case 'PEON_LEARNED': return `Leçon : ${payload.reward >= 0 ? '+' : ''}${payload.reward} · valeur apprise ${Number(payload.expectedReward).toFixed(1)} pour ${friendlyAction(payload.actionType)}.`;
        case 'PEON_TURN_COMPLETED': return `L’action de ${peonName(event.peonId)} est terminée. Prochain tour du monde : ${payload.nextRound}.`;
        case 'WORLD_FINISHED': return `Fin de la simulation : ${friendlyReason(payload.reason)}.`;
        default: return friendlyReason(payload.reason) || '';
    }
}

function friendlyEvent(type) { return ({ PEON_DECISION_MADE:'Décision prise', PEON_MOVED:'Déplacement', PEON_MOVEMENT_COST_APPLIED:'Coût du déplacement', PEON_SAW:'Observation', PEON_ATE:'Repas', FOOD_CONSUMED:'Nourriture consommée', PEON_ATTACKED:'Attaque', PEON_EXPERIENCE_GAINED:'Expérience gagnée', PEON_LEVELED_UP:'Niveau supérieur', PEON_HUNGER_APPLIED:'Effet de la faim', PEON_DIED:'Mort d’un peon', PEON_COMMUNICATED:'Communication', PEON_IDLED:'Inactivité', PEON_LEARNED:'Leçon apprise', PEON_TURN_COMPLETED:'Action du peon terminée', WORLD_FINISHED:'Monde terminé', PEON_ACTION_REJECTED:'Action impossible' })[type] || type.replaceAll('_', ' ').toLowerCase(); }

function escapeHtml(value) {
    const element = document.createElement('span');
    element.textContent = value ?? '';
    return element.innerHTML;
}

function renderAnalysis(report) {
    const actions = Object.entries(report.actionsByType).sort((left, right) => right[1] - left[1]);
    const actionMarkup = actions.length ? actions.map(([type, count]) => `<span class="analysis-action">${escapeHtml(friendlyAction(type))} <strong>${count}</strong></span>`).join('') : '<span class="muted">Aucune action enregistrée.</span>';
    const teamRows = report.teams.map(team => `<tr><td><span class="analysis-team-name" style="--team-color:${escapeHtml(team.color)}">${escapeHtml(team.name)}</span></td><td>${team.alivePeons}/${team.totalPeons}</td><td>${team.averageHealthOfLiving}</td><td>${team.averageLevel}</td><td>${team.averageExperience}</td><td>${team.moves}</td><td>${team.meals}</td><td>${team.attacks}</td><td>${team.communications}</td></tr>`).join('');
    const trends = report.behavioralTrends.map(trend => `<li>${escapeHtml(trend)}</li>`).join('');
    $('#analysisContent').innerHTML = `<div class="analysis-summary"><div class="analysis-stat"><span>Actions</span><strong>${report.totalActions}</strong></div><div class="analysis-stat"><span>Événements</span><strong>${report.totalEvents}</strong></div><div class="analysis-stat"><span>Vivants</span><strong>${report.alivePeons}</strong></div><div class="analysis-stat"><span>Morts</span><strong>${report.deadPeons}</strong></div></div><section class="analysis-section"><h3>Répartition des décisions</h3><div class="analysis-actions">${actionMarkup}</div></section><section class="analysis-section"><h3>Comparaison des équipes</h3><table class="analysis-team-table"><thead><tr><th>Équipe</th><th>Vivants</th><th>PV moy.</th><th>Niv. moy.</th><th>XP moy.</th><th>Dépl.</th><th>Repas</th><th>Att.</th><th>Comm.</th></tr></thead><tbody>${teamRows}</tbody></table></section><section class="analysis-section"><h3>Tendances observées</h3><ul class="analysis-trends">${trends || '<li>Pas encore assez de données pour dégager une tendance.</li>'}</ul></section>`;
}

canvas.addEventListener('click', event => {
    if (state.dragMoved) { state.dragMoved = false; return; }
    if (!state.world || !canvas._geometry) return;
    const rect = canvas.getBoundingClientRect(); const x = event.clientX - rect.left; const y = event.clientY - rect.top;
    const selected = state.world.peons[state.selectedPeonId];
    const mental = $('#mentalMapToggle').checked && selected;
    const candidates = mental
        ? [{ peon:selected, position:selected.position }, ...rememberedPeonPresentations(selected).map(memory => ({ peon:state.world.peons[memory.peonId], position:memory.position })).filter(candidate => candidate.peon)]
        : Object.values(state.world.peons).filter(peon => peon.alive).map(peon => ({ peon, position:peon.position }));
    const nearest = candidates.map(candidate => ({ peon:candidate.peon, point:centerOf(candidate.position, canvas._geometry) })).sort((a,b) => Math.hypot(a.point.x-x,a.point.y-y)-Math.hypot(b.point.x-x,b.point.y-y))[0];
    if (nearest && Math.hypot(nearest.point.x-x, nearest.point.y-y) < canvas._geometry.size * 1.3) { state.selectedPeonId = nearest.peon.id; render(); }
});

canvas.addEventListener('pointerdown', event => { state.dragging = true; state.dragMoved = false; state.dragStart = { x:event.clientX, y:event.clientY, panX:state.panX, panY:state.panY }; canvas.classList.add('dragging'); canvas.setPointerCapture(event.pointerId); });
canvas.addEventListener('pointermove', event => { if (!state.dragging) return; const dx=event.clientX-state.dragStart.x; const dy=event.clientY-state.dragStart.y; if (Math.abs(dx)+Math.abs(dy)>3) state.dragMoved=true; state.panX=state.dragStart.panX+dx; state.panY=state.dragStart.panY+dy; drawWorld(); });
canvas.addEventListener('pointerup', event => { state.dragging=false; canvas.classList.remove('dragging'); if (canvas.hasPointerCapture(event.pointerId)) canvas.releasePointerCapture(event.pointerId); });
canvas.addEventListener('pointercancel', () => { state.dragging=false; canvas.classList.remove('dragging'); });
canvas.addEventListener('wheel', event => { event.preventDefault(); changeZoom(event.deltaY < 0 ? .35 : -.35); }, { passive:false });

$('#worldSelect').addEventListener('change', event => selectWorld(event.target.value));
const analysisDialog = $('#analysisDialog');
const worldJournalDialog = $('#worldJournalDialog');
$('#openWorldJournalButton').addEventListener('click', () => { if (state.world) worldJournalDialog.showModal(); });
$('#closeWorldJournalButton').addEventListener('click', () => worldJournalDialog.close());
$('#analysisButton').addEventListener('click', async () => {
    if (!state.liveWorld) return;
    $('#analysisLoading').hidden = false;
    $('#analysisContent').replaceChildren();
    analysisDialog.showModal();
    try {
        const report = await api(`/api/worlds/${state.liveWorld.id}/analysis`);
        $('#analysisLoading').hidden = true;
        renderAnalysis(report);
    } catch (error) {
        $('#analysisLoading').hidden = true;
        $('#analysisContent').innerHTML = `<p class="analysis-error">Impossible d’analyser cette simulation : ${escapeHtml(error.message)}</p>`;
    }
});
$('#closeAnalysisDialog').addEventListener('click', () => analysisDialog.close());
$('#deleteWorldsButton').addEventListener('click', async () => {
    if (!state.worlds.length || !window.confirm(`Supprimer définitivement les ${state.worlds.length} monde(s), leurs événements et leurs historiques ?`)) return;
    const button = $('#deleteWorldsButton');
    const initialLabel = button.textContent;
    button.disabled = true;
    button.textContent = 'Suppression…';
    closeStream();
    try {
        await api('/api/worlds', { method:'DELETE' });
        state.worlds=[]; state.world=null; state.liveWorld=null; state.events=[]; state.selectedPeonId=null; state.historical=false;
        $('#worldSelect').innerHTML='<option>Aucun monde</option>';
        renderWorldTemplates();
        render();
        button.textContent = 'Mondes supprimés';
        window.setTimeout(() => { button.textContent = initialLabel; button.disabled = false; }, 1800);
    } catch (error) {
        button.textContent = initialLabel;
        button.disabled = false;
        window.alert(`La suppression a échoué : ${error.message}`);
        await loadWorlds();
    }
});
$('#playButton').addEventListener('click', () => changeStatus(state.liveWorld?.status === 'RUNNING' ? 'pause' : 'start'));
$('#stepButton').addEventListener('click', () => changeStatus('step'));
$('#mentalMapToggle').addEventListener('change', render);
$('#zoomIn').addEventListener('click', () => changeZoom(.5));
$('#zoomOut').addEventListener('click', () => changeZoom(-.5));
$('#zoomReset').addEventListener('click', () => { resetView(); drawWorld(); });
$('#timelineSlider').addEventListener('input', event => { $('#timelineLabel').textContent = `Séquence consultée : ${event.target.value} / ${state.liveWorld?.sequenceNumber || 0}`; });
$('#timelineSlider').addEventListener('change', event => showSequence(event.target.value));
$('#liveButton').addEventListener('click', () => showSequence(state.liveWorld?.sequenceNumber || 0));
$('#timelinePlayButton').addEventListener('click', () => {
    if (state.timelineTimer) { clearInterval(state.timelineTimer); state.timelineTimer = null; $('#timelinePlayButton').textContent = '▶ Rejouer'; return; }
    if (Number($('#timelineSlider').value) >= Number($('#timelineSlider').max)) $('#timelineSlider').value = 0;
    $('#timelinePlayButton').textContent = 'Ⅱ Suspendre le rejeu';
    state.timelineTimer = setInterval(async () => { const next = Number($('#timelineSlider').value) + 1; if (next > Number($('#timelineSlider').max)) { clearInterval(state.timelineTimer); state.timelineTimer = null; $('#timelinePlayButton').textContent = '▶ Rejouer'; return; } $('#timelineSlider').value = next; await showSequence(next); }, 350);
});
window.addEventListener('resize', drawWorld);

const dialog = $('#createDialog');
[$('#newWorldButton'), $('#emptyCreateButton')].forEach(button => button.addEventListener('click', () => dialog.showModal()));
$('#closeDialog').addEventListener('click', () => dialog.close());
$('#worldTemplate').addEventListener('change', event => applyCreationParameters(state.worlds.find(world => world.id === event.target.value) || null));
$('#createForm').addEventListener('submit', async event => {
    event.preventDefault(); $('#formError').textContent = '';
    const data = Object.fromEntries(new FormData(event.target));
    const duplicateWorld = state.worlds.find(world => world.name?.trim().toLocaleLowerCase('fr') === data.name.trim().toLocaleLowerCase('fr'));
    if (duplicateWorld) { $('#formError').textContent = `Un monde nomm\u00e9 "${data.name.trim()}" existe d\u00e9j\u00e0.`; return; }
    const number = key => Number(data[key]);
    const request = { name:data.name.trim(), width:number('width'), height:number('height'), rockPercentage:number('rockPercentage'), treePercentage:number('treePercentage'), foodCellPercentage:number('foodCellPercentage'), minFoodPerCell:number('minFoodPerCell'), maxFoodPerCell:number('maxFoodPerCell'), hungerHealthLossPerTurn:number('hungerHealthLossPerTurn'), maxRounds:number('maxRounds'), teams:[{name:data.redName.trim(),peonCount:number('redCount')},{name:data.blueName.trim(),peonCount:number('blueCount')}] };
    try { await api('/api/worlds', { method:'POST', body:JSON.stringify(request) }); dialog.close(); await loadWorlds(true); } catch (error) { $('#formError').textContent = error.message; }
});

async function bootstrap() {
    const defaults = await api('/api/config');
    state.defaults = defaults;
    state.defaultZoom = defaults.defaultZoomPercent / 100;
    resetView();
    applyCreationParameters(null);
    await loadWorlds();
}

bootstrap().catch(error => { $('#formError').textContent = error.message; console.error(error); });
