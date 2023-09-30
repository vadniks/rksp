
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Int,
    val type: Type,
    val name: String,
    val cost: Int
) {
    val serialized get() = "($id,$type,$name,$cost)"

    companion object {
        fun deserialized(component: String) = component.substring(1, component.length - 1).split(',').run {
            Component(
                this[0].toInt(),
                Type.valueOf(this[1]),
                this[2],
                this[3].toInt()
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
        SDD,
        PSU,
        CASE
    }
}
