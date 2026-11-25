package com.flowsuite.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A tenant. The root of every isolation boundary — every tenant-owned row carries this
 * entity's id as {@code organization_id}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    /** URL-safe unique handle, derived from the name on creation. */
    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    public Organization(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }
}
