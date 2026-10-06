package org.infospray.peonsimulator.domain.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class House {
    private UUID id;
    private HexCoordinate position;
    private UUID hostPeonId;
    private Set<UUID> occupantPeonIds = new LinkedHashSet<>();

    public House() {
    }

    public House(UUID id, HexCoordinate position, UUID hostPeonId) {
        this.id = id;
        this.position = position;
        this.hostPeonId = hostPeonId;
        this.occupantPeonIds.add(hostPeonId);
    }

    public UUID getId() { return this.id; }
    public HexCoordinate getPosition() { return this.position; }
    public UUID getHostPeonId() { return this.hostPeonId; }
    public Set<UUID> getOccupantPeonIds() { return this.occupantPeonIds; }
    public void setId(UUID value) { this.id = value; }
    public void setPosition(HexCoordinate value) { this.position = value; }
    public void setHostPeonId(UUID value) { this.hostPeonId = value; }
    public void setOccupantPeonIds(Set<UUID> value) { this.occupantPeonIds = value == null ? new LinkedHashSet<>() : new LinkedHashSet<>(value); }
    public boolean isEmpty() { return this.hostPeonId == null; }
    public boolean contains(UUID peonId) { return this.occupantPeonIds.contains(peonId); }
    public void enter(UUID peonId) { this.occupantPeonIds.add(peonId); }
    public void leave(UUID peonId) { this.occupantPeonIds.remove(peonId); }
    public void claim(UUID peonId) { this.hostPeonId = peonId; this.occupantPeonIds.clear(); this.occupantPeonIds.add(peonId); }
    public void empty() { this.hostPeonId = null; this.occupantPeonIds.clear(); }
}
