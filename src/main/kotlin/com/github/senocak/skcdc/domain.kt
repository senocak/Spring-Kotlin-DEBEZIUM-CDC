package com.github.senocak.skcdc

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.Date
import java.util.UUID
import org.hibernate.annotations.GenericGenerator
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Entity
@Table(name = "users")
data class User(
    @Column var name: String? = null,
    @Column var email: String? = null,
    @Column var password: String? = null
){
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    var id: UUID? = null

    @Column var createdAt: Date = Date()
    @Column var updatedAt: Date = Date()
    @Column var emailActivatedAt: Date? = null
}

@Repository
interface UserRepository: CrudRepository<User, UUID>

