package com.pulsepass.entity;

import jakarta.persistence.*;

@Entity
@Table(name="artists")
public class Artist {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=150)
    private String stageName;

    @Column(nullable=false, length=100)
    private String country;

    @Column(nullable=false, length=100)
    private String genre;

    @Column(nullable=false)
    private Boolean active;

    public Artist(){}

    public Long getId(){ return id; }
    public void setId(Long id){ this.id=id; }

    public String getStageName(){ return stageName; }
    public void setStageName(String stageName){ this.stageName=stageName; }

    public String getCountry(){ return country; }
    public void setCountry(String country){ this.country=country; }

    public String getGenre(){ return genre; }
    public void setGenre(String genre){ this.genre=genre; }

    public Boolean getActive(){ return active; }
    public void setActive(Boolean active){ this.active=active; }
}
