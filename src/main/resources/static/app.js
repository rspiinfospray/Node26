const state = { worlds: [], world: null, liveWorld: null, events: [], selectedPeonId: null, selectedHouseId: null, source: null, timelineTimer: null, historical: false, defaults: null, simulationSpeed: null, defaultZoom: 1.7, zoom: 1.7, panX: 0, panY: 0, dragging: false, dragStart: null, dragMoved: false };
const $ = selector => document.querySelector(selector);
const viewport = $('#worldViewport');
const pixiApp = new PIXI.Application();
await pixiApp.init({ preference:'webgl', resizeTo:viewport, backgroundAlpha:0, antialias:true, autoDensity:true, resolution:window.devicePixelRatio || 1, autoStart:false });
viewport.appendChild(pixiApp.canvas);
const canvas = pixiApp.canvas;
const assets = Object.fromEntries(await Promise.all(Object.entries({ grass:'/assets/terrain-grass.png', rock:'/assets/terrain-rock.png', tree:'/assets/terrain-tree.png', food:'/assets/food-cache.png', peon:'/assets/peon-topdown.png', grave:'/assets/grave.png', house:'/assets/house.png' }).map(async ([name, source]) => [name, await PIXI.Assets.load(source)])));
const animationState = { peons:new Map(), animations:[], frame:null, generation:0, reducedMotion:window.matchMedia('(prefers-reduced-motion: reduce)').matches };

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
    state.selectedHouseId = null;
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
            render();
            if (!state.historical && data.eventType === 'PEON_TURN_COMPLETED') playActionAnimations(state.events.filter(item => item.sequenceNumber === data.sequenceNumber));
            return;
        }
    });
}

function closeStream() { if (state.source) state.source.close(); state.source = null; }

async function changeStatus(action) {
    if (!state.liveWorld) return;
    state.liveWorld = await api(`/api/worlds/${state.liveWorld.id}/${action}`, { method: 'POST' });
    if (!state.historical) state.world = state.liveWorld;
    render();
}

function renderSimulationSpeed() {
    const speed = state.simulationSpeed;
    const button = $('#speedButton');
    if (!speed) { button.disabled = true; return; }
    button.disabled = false;
    button.textContent = `Vitesse ×${speed.multiplier} · ${speed.delayMilliseconds} ms`;
    const nextDelay = speed.multiplier === 1 ? Math.max(1, Math.floor(speed.delayMilliseconds / 2)) : speed.delayMilliseconds * 2;
    button.title = speed.multiplier === 1 ? `Passer en vitesse ×2 (${nextDelay} ms par séquence)` : `Revenir en vitesse ×1 (${nextDelay} ms par séquence)`;
    button.classList.toggle('fast', speed.multiplier === 2);
    button.setAttribute('aria-pressed', String(speed.multiplier === 2));
}

async function toggleSimulationSpeed() {
    if (!state.simulationSpeed) return;
    const multiplier = state.simulationSpeed.multiplier === 1 ? 2 : 1;
    $('#speedButton').disabled = true;
    try { state.simulationSpeed = await api(`/api/simulation/speed?multiplier=${multiplier}`, { method:'POST' }); } catch (error) { window.alert(`Le changement de vitesse a échoué : ${error.message}`); } finally { renderSimulationSpeed(); }
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
        clearWorldStage();
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
        $('#houseDetails').hidden = true;
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
    renderSelection();
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

function resizeRenderer() {
    const rect = viewport.getBoundingClientRect();
    pixiApp.renderer.resize(Math.max(1, rect.width), Math.max(1, rect.height));
    return rect;
}

function clearWorldStage() {
    cancelAnimations();
    pixiApp.stage.removeChildren().forEach(child => child.destroy({ children:true }));
    pixiApp.render();
}

function cancelAnimations() {
    animationState.generation++;
    animationState.animations = [];
    animationState.peons.clear();
    if (animationState.frame !== null) cancelAnimationFrame(animationState.frame);
    animationState.frame = null;
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
function hexPoints(size) { return Array.from({ length:6 }, (_, index) => { const angle = Math.PI / 3 * index; return [size * Math.cos(angle), size * Math.sin(angle)]; }).flat(); }

function drawWorld() {
    const world = state.world;
    const rect = resizeRenderer();
    cancelAnimations();
    pixiApp.stage.removeChildren().forEach(child => child.destroy({ children:true }));
    if (!world) { pixiApp.render(); return; }
    const geo = geometry(world, rect);
    const selected = world.peons[state.selectedPeonId];
    const mental = $('#mentalMapToggle').checked && selected?.alive ? selected.mentalMap : null;
    Object.values(world.cells).forEach(cell => {
        const known = !mental || mental[`${cell.coordinate.q}:${cell.coordinate.r}`];
        const rendered = mental && known ? { ...cell, terrain: known.terrain, foodQuantity: known.rememberedFoodQuantity, occupantPeonIds: known.rememberedOccupants } : cell;
        const point = centerOf(cell.coordinate, geo);
        drawCell(rendered, point, geo, Boolean(known));
        if (known && rendered.foodQuantity > 0) { drawFood(rendered, point, geo); }
    });
    Object.values(world.houses || {}).filter(house => !mental || mental[`${house.position.q}:${house.position.r}`]).forEach(house => drawHouse(house, geo));
    if (!mental) {
        drawGraves(world, geo);
        Object.values(world.peons).filter(peon => peon.alive && !peon.insideHouseId).forEach(peon => drawPeon(peon, world, geo));
    }
    else if (selected) {
        drawRememberedGraves(selected, geo);
        drawRememberedPeons(selected, world, geo);
        drawPeon(selected, world, geo);
    }
    viewport._geometry = geo;
    pixiApp.render();
}

function drawHouse(house, geo) {
    const point = centerOf(house.position, geo);
    if (house.id === state.selectedHouseId) { pixiApp.stage.addChild(new PIXI.Graphics().circle(point.x, point.y, Math.max(7, geo.size * .72)).stroke({ color:'#f4d58a', width:Math.max(1, geo.size * .09) })); }
    const sprite = new PIXI.Sprite(assets.house);
    const size = Math.max(10, geo.size * 1.55);
    sprite.anchor.set(.5, .58); sprite.position.set(point.x, point.y); sprite.width = size; sprite.height = size;
    pixiApp.stage.addChild(sprite);
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

function drawCell(cell, point, geo, known) {
    const texture = cell.terrain === 'ROCK' ? assets.rock : cell.terrain === 'TREE' ? assets.tree : assets.grass;
    const fill = known ? { texture, textureSpace:'local', color:cell.terrain === 'ROCK' ? '#dddddd' : '#e8ffe1' } : '#0c0f0c';
    const cellGraphic = new PIXI.Graphics().poly(hexPoints(geo.size - .35)).fill(fill).stroke({ color:known ? '#738069' : '#151915', alpha:known ? .34 : 1, width:.6 });
    cellGraphic.position.set(point.x, point.y);
    pixiApp.stage.addChild(cellGraphic);
}

function drawFood(cell, point, geo) {
    const quantityScale = .72 + Math.min(cell.foodQuantity, 4) * .08;
    const size = geo.size * quantityScale;
    const sprite = new PIXI.Sprite(assets.food);
    sprite.anchor.set(.5); sprite.position.set(point.x, point.y); sprite.width = size; sprite.height = size;
    pixiApp.stage.addChild(sprite);
    if (geo.size > 10) {
        const label = new PIXI.Text({ text:String(cell.foodQuantity), style:{ fill:'#ffffff', fontFamily:'sans-serif', fontSize:Math.max(7, geo.size * .32), fontWeight:'700', stroke:{ color:'#17120a', width:2 } } });
        label.anchor.set(.5); label.position.set(point.x + size * .32, point.y + size * .34); pixiApp.stage.addChild(label);
    }
}

function positionedGraves(graves, geo) {
    const groups = new Map();
    graves.forEach(grave => {
        const key = `${grave.position.q}:${grave.position.r}`;
        groups.set(key, [...(groups.get(key) || []), grave]);
    });
    return [...groups.values()].flatMap(graves => graves.map((grave, index) => {
        const center = centerOf(grave.position, geo);
        const angle = graves.length === 1 ? .35 : index * Math.PI * 2 / graves.length;
        const spread = geo.size * .42;
        return { grave, point:{ x:center.x + Math.cos(angle) * spread, y:center.y + Math.sin(angle) * spread } };
    }));
}

function gravePresentations(world, geo) { return positionedGraves(Object.values(world.graves || {}), geo); }

function rememberedGravePresentations(selected, geo) {
    const newestByPeon = new Map();
    Object.values(selected.mentalMap || {}).flatMap(memory => memory.rememberedGraves || []).forEach(grave => {
        const previous = newestByPeon.get(grave.peonId);
        if (!previous || grave.observedAtSequence > previous.observedAtSequence) newestByPeon.set(grave.peonId, grave);
    });
    return positionedGraves([...newestByPeon.values()], geo);
}

function drawGraves(world, geo) {
    drawGravePresentations(gravePresentations(world, geo), geo, false);
}

function drawRememberedGraves(selected, geo) {
    drawGravePresentations(rememberedGravePresentations(selected, geo), geo, true);
}

function drawGravePresentations(presentations, geo, remembered) {
    presentations.forEach(({ grave, point }) => {
        const sprite = new PIXI.Sprite(assets.grave);
        const size = Math.max(7, geo.size * .9);
        sprite.anchor.set(.5); sprite.position.set(point.x, point.y); sprite.width = size; sprite.height = size;
        sprite.alpha = remembered ? .5 : grave.peonId === state.selectedPeonId ? 1 : .88;
        if (grave.peonId === state.selectedPeonId) {
            const selection = new PIXI.Graphics().circle(point.x, point.y, size * .46).stroke({ color:'#f4d58a', width:Math.max(1, geo.size * .08) });
            pixiApp.stage.addChild(selection);
        }
        pixiApp.stage.addChild(sprite);
    });
}

function drawPeon(peon, world, geo, presentation = {}) {
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
    const relationColor = presentation.relation === 'ENEMY' ? '#ff6666' : presentation.relation === 'ALLY' ? '#7fdb86' : '#f4d58a';
    const peonContainer = new PIXI.Container();
    peonContainer.position.set(x, y);
    peonContainer.alpha = presentation.remembered ? .48 : 1;
    const marker = new PIXI.Graphics().ellipse(0, spriteHeight * .25, spriteWidth * .43, spriteHeight * .28).fill({ color:team?.color || '#dddddd', alpha:.8 });
    if (!presentation.remembered && peon.id === state.selectedPeonId) marker.stroke({ color:'#ffffff', width:Math.max(1, geo.size * .09) });
    if (presentation.remembered) marker.stroke({ color:relationColor, width:Math.max(1, geo.size * .07) });
    peonContainer.addChild(marker);
    const sprite = new PIXI.Sprite(assets.peon);
    sprite.anchor.set(.5, .62); sprite.width = spriteWidth; sprite.height = spriteHeight;
    peonContainer.addChild(sprite);
    if (!presentation.remembered && geo.size > 9) {
        const barWidth = spriteWidth;
        const barY = -spriteHeight * .7;
        const healthBar = new PIXI.Graphics().rect(-barWidth / 2, barY, barWidth, 2).fill('#180d0d').rect(-barWidth / 2, barY, barWidth * peon.healthPoints / peon.maxHealthPoints, 2).fill('#df5b5b');
        peonContainer.addChild(healthBar);
        const levelRadius = Math.max(3, geo.size * .2);
        const levelX = barWidth / 2 + levelRadius + 1;
        const levelBadge = new PIXI.Graphics().circle(levelX, barY + 1, levelRadius).fill('#d9a441').stroke({ color:'#211708', width:Math.max(.7, geo.size * .04) });
        const levelLabel = new PIXI.Text({ text:String(peon.level), style:{ fill:'#17130b', fontFamily:'sans-serif', fontSize:Math.max(5, levelRadius * 1.45), fontWeight:'800' } });
        levelLabel.anchor.set(.5); levelLabel.position.set(levelX, barY + 1);
        peonContainer.addChild(levelBadge, levelLabel);
    }
    pixiApp.stage.addChild(peonContainer);
    if (!presentation.remembered) animationState.peons.set(peon.id, peonContainer);
}

function playActionAnimations(events) {
    if (animationState.reducedMotion || !viewport._geometry || !events.length) return;
    const moved = events.some(event => event.eventType === 'PEON_MOVED');
    events.forEach(event => {
        const delay = moved && event.eventType === 'PEON_SAW' ? 300 : 0;
        if (event.eventType === 'PEON_MOVED') animateMovement(event);
        if (event.eventType === 'PEON_ATE') animateEating(event);
        if (event.eventType === 'PEON_COMMUNICATED') animateCommunication(event);
        if (event.eventType === 'PEON_SAW') animateObservation(event, delay);
        if (event.eventType === 'PEON_ATTACKED') animateAttack(event);
        if (event.eventType === 'PEON_CHOPPED_WOOD') animateChopWood(event);
        if (event.eventType === 'PEON_LEVELED_UP') animateLevelUp(event);
    });
}

function queueAnimation(duration, delay, update, complete = () => {}) {
    const multiplier = state.simulationSpeed?.multiplier || 1;
    animationState.animations.push({ duration:duration / multiplier, delay:delay / multiplier, update, complete, startedAt:null });
    if (animationState.frame === null) animationState.frame = requestAnimationFrame(runAnimations);
}

function runAnimations(timestamp) {
    animationState.animations = animationState.animations.filter(animation => {
        if (animation.startedAt === null) animation.startedAt = timestamp;
        const elapsed = timestamp - animation.startedAt - animation.delay;
        if (elapsed < 0) return true;
        const progress = Math.min(1, elapsed / animation.duration);
        animation.update(progress);
        if (progress < 1) return true;
        animation.complete();
        return false;
    });
    pixiApp.render();
    animationState.frame = animationState.animations.length ? requestAnimationFrame(runAnimations) : null;
}

function removeEffect(effect) {
    if (effect.parent) effect.parent.removeChild(effect);
    effect.destroy({ children:true });
}

function animateMovement(event) {
    const actor = animationState.peons.get(event.peonId);
    if (!actor || !event.payload?.from || !event.payload?.to) return;
    const from = centerOf(event.payload.from, viewport._geometry);
    const to = centerOf(event.payload.to, viewport._geometry);
    const final = { x:actor.x, y:actor.y };
    const start = { x:final.x + from.x - to.x, y:final.y + from.y - to.y };
    actor.position.set(start.x, start.y);
    queueAnimation(430, 0, progress => {
        const eased = 1 - Math.pow(1 - progress, 3);
        actor.position.set(start.x + (final.x - start.x) * eased, start.y + (final.y - start.y) * eased);
        actor.scale.set(1 + Math.sin(progress * Math.PI * 4) * .06);
    }, () => { actor.position.set(final.x, final.y); actor.scale.set(1); });
}

function animateEating(event) {
    const actor = animationState.peons.get(event.peonId);
    if (!actor) return;
    const glow = new PIXI.Graphics().circle(0, 0, Math.max(5, viewport._geometry.size * .55)).fill({ color:'#f5c451', alpha:.38 }).stroke({ color:'#fff2a6', width:2 });
    actor.addChildAt(glow, 0);
    queueAnimation(500, 0, progress => {
        const pulse = Math.sin(progress * Math.PI);
        glow.scale.set(.45 + progress * 1.25); glow.alpha = (1 - progress) * .75;
        actor.scale.set(1 + pulse * .22);
    }, () => { actor.scale.set(1); removeEffect(glow); });
}

function animateCommunication(event) {
    const actor = animationState.peons.get(event.peonId);
    if (!actor) return;
    const rings = new PIXI.Container();
    [0, 1, 2].forEach(index => rings.addChild(new PIXI.Graphics().circle(0, 0, Math.max(4, viewport._geometry.size * .45)).stroke({ color:'#62d8ff', width:2, alpha:.9 - index * .2 })));
    actor.addChildAt(rings, 0);
    queueAnimation(540, 0, progress => {
        rings.children.forEach((ring, index) => { const local = Math.max(0, Math.min(1, progress * 1.65 - index * .24)); ring.scale.set(.5 + local * 2.6); ring.alpha = local > 0 ? 1 - local : 0; });
    }, () => removeEffect(rings));
}

function animateObservation(event, delay = 0) {
    const actor = animationState.peons.get(event.peonId);
    if (!actor) return;
    const peon = state.world?.peons?.[event.peonId];
    const radius = Math.max(8, viewport._geometry.size * (peon?.level || 1) * 1.7);
    const sight = new PIXI.Graphics().circle(0, 0, radius).fill({ color:'#d8f5b2', alpha:.08 }).stroke({ color:'#d8f5b2', width:1.5, alpha:.7 });
    sight.scale.set(.12); sight.alpha = 0; actor.addChildAt(sight, 0);
    queueAnimation(280, delay, progress => { const eased = 1 - Math.pow(1 - progress, 2); sight.scale.set(.12 + eased * .88); sight.alpha = Math.sin(progress * Math.PI) * .72; }, () => removeEffect(sight));
}

function animateAttack(event) {
    const actor = animationState.peons.get(event.peonId);
    if (!actor) return;
    const target = animationState.peons.get(event.payload?.targetPeonId);
    const slash = new PIXI.Graphics().moveTo(-viewport._geometry.size * .55, viewport._geometry.size * .55).lineTo(viewport._geometry.size * .55, -viewport._geometry.size * .55).stroke({ color:'#fff0d0', width:Math.max(2, viewport._geometry.size * .16) });
    slash.alpha = 0; actor.addChild(slash);
    const actorOrigin = { x:actor.x, y:actor.y };
    const targetOrigin = target ? { x:target.x, y:target.y } : null;
    queueAnimation(460, 0, progress => {
        const strike = Math.sin(progress * Math.PI);
        actor.x = actorOrigin.x + strike * viewport._geometry.size * .32;
        actor.rotation = strike * .16;
        slash.alpha = Math.sin(progress * Math.PI) * .95;
        slash.scale.set(.45 + progress * .8);
        if (target && targetOrigin) target.x = targetOrigin.x + Math.sin(progress * Math.PI * 8) * viewport._geometry.size * .12 * (1 - progress);
    }, () => {
        actor.position.set(actorOrigin.x, actorOrigin.y); actor.rotation = 0;
        if (target && targetOrigin) target.position.set(targetOrigin.x, targetOrigin.y);
        removeEffect(slash);
    });
}

function animateChopWood(event) {
    const actor = animationState.peons.get(event.peonId);
    if (!actor) return;
    const axe = new PIXI.Container();
    const size = viewport._geometry.size;
    const handle = new PIXI.Graphics().roundRect(-size * .055, -size * .52, size * .11, size * .72, size * .04).fill('#7b4b25');
    const head = new PIXI.Graphics().poly([-size * .08, -size * .5, size * .34, -size * .62, size * .37, -size * .3, -size * .08, -size * .34]).fill('#c9d0d0').stroke({ color:'#4c5558', width:Math.max(1, size * .05) });
    const actorOriginX = actor.x;
    axe.addChild(handle, head); axe.position.set(size * .36, -size * .3); axe.rotation = -.8; actor.addChild(axe);
    queueAnimation(520, 0, progress => {
        const swing = Math.sin(Math.min(1, progress * 1.35) * Math.PI);
        axe.rotation = -.8 + swing * 1.75;
        actor.rotation = -swing * .1;
        actor.x = actorOriginX + Math.sin(progress * Math.PI * 4) * size * .025;
    }, () => { actor.x = actorOriginX; actor.rotation = 0; removeEffect(axe); });
}

function animateLevelUp(event) {
    const actor = animationState.peons.get(event.peonId);
    if (!actor) return;
    const size = viewport._geometry.size;
    const celebration = new PIXI.Container();
    const aura = new PIXI.Graphics().circle(0, 0, Math.max(7, size * .72)).fill({ color:'#f6c453', alpha:.24 }).stroke({ color:'#ffe79a', width:2.2, alpha:.95 });
    const innerRing = new PIXI.Graphics().circle(0, 0, Math.max(4, size * .4)).stroke({ color:'#fff7c7', width:1.4, alpha:.9 });
    const rays = new PIXI.Graphics();
    for (let index = 0; index < 10; index++) {
        const angle = index * Math.PI * 2 / 10;
        rays.moveTo(Math.cos(angle) * size * .78, Math.sin(angle) * size * .78).lineTo(Math.cos(angle) * size * 1.3, Math.sin(angle) * size * 1.3);
    }
    rays.stroke({ color:'#ffd65c', width:Math.max(1, size * .09), alpha:.9 });
    const levelLabel = new PIXI.Text({ text:`NIVEAU ${event.payload?.levelAfter ?? ''}`, style:{ fill:'#fff1a8', fontFamily:'Georgia, serif', fontSize:Math.max(9, size * .55), fontWeight:'700', stroke:{ color:'#4d3000', width:3 }, dropShadow:{ color:'#000000', alpha:.65, blur:2, distance:2 } } });
    levelLabel.anchor.set(.5); levelLabel.y = -size * 1.2;
    celebration.addChild(aura, innerRing, rays, levelLabel);
    celebration.scale.set(.2); celebration.alpha = 0; actor.addChild(celebration);
    queueAnimation(560, 0, progress => {
        const arrival = 1 - Math.pow(1 - Math.min(1, progress * 2.2), 3);
        celebration.scale.set(.2 + arrival * .95);
        celebration.alpha = Math.min(1, progress * 5) * (1 - Math.max(0, progress - .72) / .28);
        celebration.rotation = progress * .22;
        rays.rotation = -progress * .9;
        aura.scale.set(1 + Math.sin(progress * Math.PI * 3) * .12);
        levelLabel.y = -size * (1.2 + progress * .8);
        levelLabel.rotation = -celebration.rotation;
    }, () => removeEffect(celebration));
}

function renderSelection() {
    const house = state.world?.houses?.[state.selectedHouseId];
    $('#inspectorTitle').textContent = house ? 'MAISON SÉLECTIONNÉE' : 'PEON SÉLECTIONNÉ';
    $('#houseDetails').hidden = !house;
    if (house) { $('#peonEmpty').hidden = true; $('#peonDetails').hidden = true; renderHouse(house); return; }
    renderPeon();
}

function renderHouse(house) {
    const occupantIds = [...(house.occupantPeonIds || [])];
    const occupants = occupantIds.map(id => state.world.peons[id]).filter(Boolean);
    const host = house.hostPeonId ? state.world.peons[house.hostPeonId] : null;
    $('#houseStatus').textContent = house.hostPeonId ? 'Occupée et protégée' : 'Vide · libre à la prise de possession';
    $('#housePosition').textContent = `${house.position.q}, ${house.position.r}`;
    $('#houseHost').textContent = host?.firstName || 'Aucun';
    $('#houseOccupantCount').textContent = occupants.length;
    $('#houseId').textContent = house.id;
    $('#houseOccupants').innerHTML = occupants.length ? occupants.map(peon => { const team = state.world.teams[peon.teamId]; return `<button class="house-occupant" data-peon-id="${peon.id}"><span>${escapeHtml(peon.firstName)} · ${escapeHtml(team?.name || 'Sans équipe')}</span><i style="--team-color:${escapeHtml(team?.color || '#888')}"></i></button>`; }).join('') : '<p class="muted">Personne ne se trouve à l’intérieur.</p>';
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
    $('#woodValue').textContent = peon.inventory?.WOOD || 0;
    $('#shelterValue').textContent = peon.insideHouseId ? 'À l’abri dans une maison · faim réduite de moitié' : 'À l’extérieur';
    renderPersonality(peon);
    renderPeonActions(peon);
    $('#peonId').textContent = peon.id;
}

function renderPersonality(peon) {
    const personality = peon.personality || { prudence:50, aggressiveness:50, curiosity:50, solidarity:50 };
    const traits = [['Prudence', personality.prudence], ['Agressivité', personality.aggressiveness], ['Curiosité', personality.curiosity], ['Solidarité', personality.solidarity], ['Ingéniosité', personality.ingenuity ?? 50]];
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
function friendlyAction(type) { return ({ VOIR:'Observer', MANGER:'Manger', SE_DEPLACER:'Se déplacer', ATTAQUER:'Attaquer', COMMUNIQUER:'Communiquer', COUPER_DU_BOIS:'Couper du bois', CONSTRUIRE_MAISON:'Construire une maison', NE_RIEN_FAIRE:'Ne rien faire' })[type] || type || 'Action inconnue'; }
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
        case 'PEON_ATTACKED': return `${peonName(event.peonId)} attaque ${peonName(payload.targetPeonId)} : ${payload.damage} dégâts, PV ${payload.healthBefore} → ${payload.healthAfter}${payload.lethal ? ` · coup mortel, ${payload.attackerExperienceGain} XP pour le vainqueur` : ` · ${payload.attackerExperienceGain ?? 40} XP pour l’attaquant`}.`;
        case 'PEON_EXPERIENCE_GAINED': return `+${payload.amount} XP, total : ${payload.experienceAfter} XP.`;
        case 'PEON_LEVELED_UP': return `Niveau ${payload.levelBefore} → ${payload.levelAfter} · PV max ${payload.maxHealthBefore} → ${payload.maxHealthAfter} · dégâts ${payload.attackDamageBefore} → ${payload.attackDamageAfter}.`;
        case 'PEON_LEVEL_UP_HEAL_APPLIED': return `Soin de niveau : PV ${payload.healthBefore} → ${payload.healthAfter}/${payload.maxHealthPoints}.`;
        case 'PEON_HUNGER_APPLIED': return `Faim : −${payload.loss} PV, ${payload.healthBefore} → ${payload.healthAfter}.`;
        case 'PEON_MOVEMENT_COST_APPLIED': return `Effort du déplacement : −${payload.loss} PV, ${payload.healthBefore} → ${payload.healthAfter}.`;
        case 'PEON_DIED': return `Mort causée par ${friendlyReason(payload.reason)} en ${coordinate(payload.position)}.`;
        case 'PEON_COMMUNICATED': return `Souvenirs partagés avec ${payload.allyCount} allié(s).`;
        case 'PEON_CHOPPED_WOOD': return `Bois récolté : +${payload.woodCollected}, inventaire ${payload.woodAfter}. PV de l’arbre : ${payload.treeHealthBefore} → ${payload.treeHealthAfter}.`;
        case 'TREE_CUT_DOWN': return `L’arbre en ${coordinate(payload.position)} devient une plaine.`;
        case 'HOUSE_BUILT': return `Maison construite en ${coordinate(payload.position)} avec ${payload.woodSpent} bois.`;
        case 'HOUSE_CLAIMED': return `Maison vide prise en possession en ${coordinate(payload.position)}.`;
        case 'PEON_ENTERED_HOUSE': return 'Le peon entre dans la maison.';
        case 'PEON_LEFT_HOUSE': return 'Le peon quitte la maison.';
        case 'HOUSE_EMPTIED': return 'La maison devient vide et peut être reprise.';
        case 'HOUSE_ENTRY_REQUESTED': return `Demande d’entrée adressée à ${peonName(payload.hostPeonId)}.`;
        case 'HOUSE_ENTRY_ACCEPTED': return `Entrée de ${peonName(payload.guestPeonId)} acceptée.`;
        case 'HOUSE_ENTRY_REFUSED': return `Entrée de ${peonName(payload.guestPeonId)} refusée.`;
        case 'HOUSE_ENTRY_INVITED': return `${peonName(payload.guestPeonId)} est invité dans la maison.`;
        case 'PEON_IDLED': return 'Le peon reste sur place.';
        case 'PEON_ACTION_REJECTED': return `${friendlyAction(payload.actionType)} impossible : ${friendlyReason(payload.reason)}.`;
        case 'PEON_LEARNED': return `Leçon : ${payload.reward >= 0 ? '+' : ''}${payload.reward} · valeur apprise ${Number(payload.expectedReward).toFixed(1)} pour ${friendlyAction(payload.actionType)}.`;
        case 'PEON_TURN_COMPLETED': return `L’action de ${peonName(event.peonId)} est terminée. Prochain tour du monde : ${payload.nextRound}.`;
        case 'WORLD_FINISHED': return `Fin de la simulation : ${friendlyReason(payload.reason)}.`;
        default: return friendlyReason(payload.reason) || '';
    }
}

function friendlyEvent(type) { return ({ PEON_DECISION_MADE:'Décision prise', PEON_MOVED:'Déplacement', PEON_MOVEMENT_COST_APPLIED:'Coût du déplacement', PEON_SAW:'Observation', PEON_ATE:'Repas', FOOD_CONSUMED:'Nourriture consommée', PEON_ATTACKED:'Attaque', PEON_EXPERIENCE_GAINED:'Expérience gagnée', PEON_LEVELED_UP:'Niveau supérieur', PEON_LEVEL_UP_HEAL_APPLIED:'Soin de niveau', PEON_HUNGER_APPLIED:'Effet de la faim', PEON_DIED:'Mort d’un peon', PEON_COMMUNICATED:'Communication', PEON_IDLED:'Inactivité', PEON_LEARNED:'Leçon apprise', PEON_TURN_COMPLETED:'Action du peon terminée', WORLD_FINISHED:'Monde terminé', PEON_ACTION_REJECTED:'Action impossible' })[type] || type.replaceAll('_', ' ').toLowerCase(); }

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
    if (!state.world || !viewport._geometry) return;
    const rect = canvas.getBoundingClientRect(); const x = event.clientX - rect.left; const y = event.clientY - rect.top;
    const selected = state.world.peons[state.selectedPeonId];
    const mental = $('#mentalMapToggle').checked && selected?.alive;
    const peonCandidates = mental
        ? [{ peon:selected, position:selected.position }, ...rememberedPeonPresentations(selected).map(memory => ({ peon:state.world.peons[memory.peonId], position:memory.position })).filter(candidate => candidate.peon), ...rememberedGravePresentations(selected, viewport._geometry).map(({ grave, point }) => ({ peon:state.world.peons[grave.peonId], point })).filter(candidate => candidate.peon)]
        : [...Object.values(state.world.peons).filter(peon => peon.alive && !peon.insideHouseId).map(peon => ({ peon, position:peon.position })), ...gravePresentations(state.world, viewport._geometry).map(({ grave, point }) => ({ peon:state.world.peons[grave.peonId], point }))];
    const houseCandidates = Object.values(state.world.houses || {}).filter(house => !mental || selected?.mentalMap?.[`${house.position.q}:${house.position.r}`]).map(house => ({ house, position:house.position }));
    const candidates = [...peonCandidates.map(candidate => ({ ...candidate, kind:'peon' })), ...houseCandidates.map(candidate => ({ ...candidate, kind:'house' }))];
    const nearest = candidates.map(candidate => ({ ...candidate, point:candidate.point || centerOf(candidate.position, viewport._geometry) })).sort((a,b) => Math.hypot(a.point.x-x,a.point.y-y)-Math.hypot(b.point.x-x,b.point.y-y))[0];
    if (nearest && Math.hypot(nearest.point.x-x, nearest.point.y-y) < viewport._geometry.size * 1.3) {
        if (nearest.kind === 'house') { state.selectedHouseId = nearest.house.id; } else { state.selectedPeonId = nearest.peon.id; state.selectedHouseId = null; }
        render();
    }
});

$('#houseOccupants').addEventListener('click', event => {
    const button = event.target.closest('[data-peon-id]');
    if (!button) return;
    state.selectedPeonId = button.dataset.peonId; state.selectedHouseId = null; render();
});

canvas.addEventListener('pointerdown', event => { state.dragging = true; state.dragMoved = false; state.dragStart = { x:event.clientX, y:event.clientY, panX:state.panX, panY:state.panY }; canvas.classList.add('dragging'); canvas.setPointerCapture(event.pointerId); });
canvas.addEventListener('pointermove', event => { if (!state.dragging) return; const dx=event.clientX-state.dragStart.x; const dy=event.clientY-state.dragStart.y; if (Math.abs(dx)+Math.abs(dy)>3) state.dragMoved=true; state.panX=state.dragStart.panX+dx; state.panY=state.dragStart.panY+dy; drawWorld(); });
canvas.addEventListener('pointerup', event => { state.dragging=false; canvas.classList.remove('dragging'); if (canvas.hasPointerCapture(event.pointerId)) canvas.releasePointerCapture(event.pointerId); });
canvas.addEventListener('pointercancel', () => { state.dragging=false; canvas.classList.remove('dragging'); });
canvas.addEventListener('wheel', event => { event.preventDefault(); changeZoom(event.deltaY < 0 ? .35 : -.35); }, { passive:false });

$('#worldSelect').addEventListener('change', event => selectWorld(event.target.value));
$('#speedButton').addEventListener('click', toggleSimulationSpeed);
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
    const [defaults, simulationSpeed] = await Promise.all([api('/api/config'), api('/api/simulation/speed')]);
    state.defaults = defaults;
    state.simulationSpeed = simulationSpeed;
    renderSimulationSpeed();
    state.defaultZoom = defaults.defaultZoomPercent / 100;
    resetView();
    applyCreationParameters(null);
    await loadWorlds();
}

bootstrap().catch(error => { $('#formError').textContent = error.message; console.error(error); });
