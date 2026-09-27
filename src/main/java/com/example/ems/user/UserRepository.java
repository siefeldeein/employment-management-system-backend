package com.example.ems.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("Select u From User u Left Join Fetch u.roles Where u.username =:username")
    Optional<User> findByUsernameWithRoles(String username);

}
