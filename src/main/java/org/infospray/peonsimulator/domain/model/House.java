package org.infospray.peonsimulator.domain.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class House {
    private UUID id;
    private HexCoordinate position;
    private UUID hostPeonId;
    private Set<UUID> ownerPeonIds = new LinkedHashSet<>();
    private Set<UUID> occupantPeonIds = new LinkedHashSet<>();

    public House() {
    }

    public House(UUID id, HexCoordinate position, UUID hostPeonId) {
        this.id = id;
        this.position = position;
        this.hostPeonId = hostPeonId;
        this.ownerPeonIds.add(hostPeonId);
        this.occupantPeonIds.add(hostPeonId);
    }

    public UUID getId() { return this.id; }
    public HexCoordinate getPosition() { return this.position; }
    public UUID getHostPeonId() { return this.hostPeonId; }
    public Set<UUID> getOccupantPeonIds() { return this.occupantPeonIds; }
    public Set<UUID> getOwnerPeonIds() { if (this.ownerPeonIds.isEmpty() && this.hostPeonId != null) { this.ownerPeonIds.add(this.hostPeonId); } return this.ownerPeonIds; }
    public void setId(UUID value) { this.id = value; }
    public void setPosition(HexCoordinate value) { this.position = value; }
    public void setHostPeonId(UUID value) { this.hostPeonId = value; if (value != null) { this.ownerPeonIds.add(value); } }
    public void setOwnerPeonIds(Set<UUID> value) { this.ownerPeonIds = value == null ? new LinkedHashSet<>() : new LinkedHashSet<>(value); if (this.hostPeonId != null) { this.ownerPeonIds.add(this.hostPeonId); } }
    public void setOccupantPeonIds(Set<UUID> value) { this.occupantPeonIds = value == null ? new LinkedHashSet<>() : new LinkedHashSet<>(value); }
    public boolean isEmpty() { return this.occupantPeonIds.isEmpty(); }
    public boolean isOwned() { return !this.getOwnerPeonIds().isEmpty(); }
    public boolean isOwner(UUID peonId) { return this.getOwnerPeonIds().contains(peonId); }
    public boolean contains(UUID peonId) { return this.occupantPeonIds.contains(peonId); }
    public void enter(UUID peonId) { this.occupantPeonIds.add(peonId); }
    public void leave(UUID peonId) { this.occupantPeonIds.remove(peonId); }
    public void claim(UUID peonId) { this.hostPeonId = peonId; this.ownerPeonIds.clear(); this.ownerPeonIds.add(peonId); this.occupantPeonIds.clear(); this.occupantPeonIds.add(peonId); }
    public void inviteOwner(UUID peonId) { this.ownerPeonIds.add(peonId); this.occupantPeonIds.add(peonId); }
    public void removeOwner(UUID peonId) { this.ownerPeonIds.remove(peonId); this.occupantPeonIds.remove(peonId); if (peonId.equals(this.hostPeonId)) { this.hostPeonId = this.ownerPeonIds.stream().findFirst().orElse(null); } }
    public void empty() { this.hostPeonId = null; this.ownerPeonIds.clear(); this.occupantPeonIds.clear(); }
}
