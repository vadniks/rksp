
package com.example.rsocket

import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON
import org.springframework.context.annotation.Scope
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Scope(SCOPE_SINGLETON) interface Repository : JpaRepository<Component, Int> {

    @Query("truncate table components restart identity", nativeQuery = true)
    fun prune()
}
