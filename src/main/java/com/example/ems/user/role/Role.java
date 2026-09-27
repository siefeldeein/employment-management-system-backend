package com.example.ems.user.role;

import com.example.ems.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name; // e.g., "ROLE_ADMIN"

    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

}
