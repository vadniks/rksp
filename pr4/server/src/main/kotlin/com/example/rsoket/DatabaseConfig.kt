
package com.example.rsoket

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.hibernate5.HibernateTransactionManager
import org.springframework.orm.hibernate5.LocalSessionFactoryBean
import java.util.*

private const val PACKAGE = "com.example.rsoket"

@EnableJpaRepositories(basePackages = [PACKAGE])
@Configuration
class DatabaseConfig {
    val dataSource @Bean(name = ["dataSource"]) get() = HikariDataSource(HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://localhost:5432/db"
        driverClassName = "org.postgresql.Driver"
        username = "postgres"
        password = "postgres"
    })

    val entityManagerFactory @Bean(name = ["entityManagerFactory"])
    get() = LocalSessionFactoryBean().apply {
            setDataSource(dataSource)
            setPackagesToScan(PACKAGE)
            hibernateProperties = Properties().apply {
                put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                put("hibernate.hbm2ddl.auto", "update")
            }
        }

    @Suppress("unused")
    val transactionManager @Bean("transactionManager")
    get() = HibernateTransactionManager(entityManagerFactory.`object`!!)
}
