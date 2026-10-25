package rentsphere.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Tier 1: Base Domain Entity
 * MCA OOT Lab - Assignment 2 (5-Tier Inheritance Hierarchy)
 */
public abstract class AbstractDomainEntity {
    protected final String id;
    protected final long createdAt;
    protected long updatedAt;

    public AbstractDomainEntity() {
        this.id = "ENT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }

    public AbstractDomainEntity(String id) {
        this.id = (id == null || id.isBlank()) ? "ENT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() : id;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }

    public AbstractDomainEntity(String id, long createdAt, long updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now().toEpochMilli();
    }

    /**
     * Polymorphic pipe-delimited flat file serialization hook
     */
    public abstract String serializeToPipe();
}
