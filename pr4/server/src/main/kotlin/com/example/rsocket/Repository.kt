
package com.example.rsocket

import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON
import org.springframework.context.annotation.Scope
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Scope(SCOPE_SINGLETON) interface Repository : JpaRepository<Component, Int> {

    @Transactional
    @Modifying
    @Query(value = "truncate table components restart identity", nativeQuery = true)
    fun prune()
}
