
package com.example.rsocket

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "components")
@Entity
data class Component(
    val type: Type,
    val name: String,
    val cost: Int,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Int? = null
) {
    val serialized get() = "($type,$name,$cost,$id)"

    companion object {
        fun deserialized(component: String) = component.substring(1, component.length - 1).split(',').run {
            Component(
                Type.valueOf(this[0]),
                this[1],
                this[2].toInt(),
                if (this.size == 4) this[3].toIntOrNull() else null
            )
        }
    }

    enum class Type {
        CPU,
        MOTHERBOARD,
        RAM,
        GPU,
        COOLER,
        HDD,
        SSD,
        PSU,
        CASE
    }
}
