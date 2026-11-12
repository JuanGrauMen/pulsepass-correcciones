package com.pulsepass.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=100)
    private String firstName;

    @Column(nullable=false, length=100)
    private String lastName;

    @Column(length=30)
    private String phone;
    @Column(length=100)
    private String city;
    private LocalDate birthDate;

    @OneToOne
    @JoinColumn(name="user_id", nullable=false, unique=true)
    private User user;

    public UserProfile(){}

    public Long getId(){ return id; }
    public void setId(Long id){ this.id=id; }

    public String getFirstName(){ return firstName; }
    public void setFirstName(String firstName){ this.firstName=firstName; }

    public String getLastName(){ return lastName; }
    public void setLastName(String lastName){ this.lastName=lastName; }

    public String getPhone(){ return phone; }
    public void setPhone(String phone){ this.phone=phone; }

    public String getCity(){ return city; }
    public void setCity(String city){ this.city=city; }

    public LocalDate getBirthDate(){ return birthDate; }
    public void setBirthDate(LocalDate birthDate){ this.birthDate=birthDate; }

    public User getUser(){ return user; }
    public void setUser(User user){ this.user=user; }
}
