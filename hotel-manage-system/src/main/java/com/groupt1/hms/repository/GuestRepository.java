package com.groupt1.hms.repository;

import com.groupt1.hms.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Integer> {

    Optional<Guest> findByUserAccount_UserId(Integer userId);

    Optional<Guest> findByCccd(String cccd);
}
