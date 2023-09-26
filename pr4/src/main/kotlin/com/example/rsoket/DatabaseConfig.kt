
package com.example.rsoket

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.hibernate5.HibernateTransactionManager
import org.springframework.orm.hibernate5.LocalSessionFactoryBean
import java.util.*

@EnableJpaRepositories(basePackages = ["com.example.rsoket.config"])
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
            setPackagesToScan("com.example.virt3")
            hibernateProperties = Properties().apply {
                put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL92Dialect")
                put("hibernate.hbm2ddl.auto", "update")
            }
        }

    @Suppress("unused")
    val transactionManager @Bean("transactionManager")
    get() = HibernateTransactionManager(entityManagerFactory.`object`!!)
}
