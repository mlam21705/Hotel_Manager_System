package com.groupt1.hms.service.impl;

import com.groupt1.hms.dto.UserProfileDTO;
import com.groupt1.hms.entity.Guest;
import com.groupt1.hms.entity.UserAccount;
import com.groupt1.hms.repository.GuestRepository;
import com.groupt1.hms.repository.UserAccountRepository;
import com.groupt1.hms.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final UserAccountRepository userAccountRepository;
    private final GuestRepository guestRepository;

    @Override
    public UserProfileDTO getUserProfile(Integer userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileDTO dto = UserProfileDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().getRoleName() : null)
                .build();

        // If user is a Customer, load Guest details
        if (dto.getRole() != null && (dto.getRole().equals("Customer") || dto.getRole().equals("Guest"))) {
            guestRepository.findByUserAccount_UserId(userId).ifPresent(guest -> {
                dto.setGuestName(guest.getGuestName());
                dto.setPhoneNumber(guest.getPhoneNumber());
                dto.setCccd(guest.getCccd());
                dto.setAddress(guest.getAddress());
            });
        }

        return dto;
    }

    @Override
    @Transactional
    public void updateUserProfile(Integer userId, UserProfileDTO dto) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check email uniqueness if email has changed
        if (!user.getEmail().equalsIgnoreCase(dto.getEmail())) {
            if (userAccountRepository.existsByEmail(dto.getEmail())) {
                throw new RuntimeException("Email is already in use by another account.");
            }
            user.setEmail(dto.getEmail());
        }

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "";

        // If Customer, update Guest entity
        if (roleName.equals("Customer") || roleName.equals("Guest")) {
            Guest guest = guestRepository.findByUserAccount_UserId(userId)
                    .orElseGet(() -> {
                        Guest newGuest = new Guest();
                        newGuest.setUserAccount(user);
                        return newGuest;
                    });

            // Check CCCD uniqueness if it has changed
            if (guest.getCccd() == null || !guest.getCccd().equalsIgnoreCase(dto.getCccd())) {
                Optional<Guest> existingGuestByCccd = guestRepository.findByCccd(dto.getCccd());
                if (existingGuestByCccd.isPresent() && !existingGuestByCccd.get().getGuestId().equals(guest.getGuestId())) {
                    throw new RuntimeException("CCCD/Passport is already in use by another guest.");
                }
            }

            guest.setGuestName(dto.getGuestName());
            guest.setPhoneNumber(dto.getPhoneNumber());
            guest.setCccd(dto.getCccd());
            guest.setAddress(dto.getAddress());

            guestRepository.save(guest);
        }

        userAccountRepository.save(user);
    }
}
