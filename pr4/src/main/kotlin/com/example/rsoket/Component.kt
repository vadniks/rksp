
package com.example.rsoket

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "components")
@Entity
data class Component(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,
    val type: Type,
    val name: String,
    val cost: String
) {
    enum class Type {
        CPU,
        MOTHERBOARD,
        RAM,
        GPU,
        COOLER,
        HDD,
        SDD,
        PSU,
        CASE
    }
}
