package com.example.test.service;

import com.example.test.entity.Staff;
import com.example.test.repository.StaffRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaffDetailsService implements UserDetailsService {

    private final StaffRepository staffRepository;

    public StaffDetailsService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Staff staff = staffRepository.findByUserName(username);
        if (staff == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        String authorityName = staffRepository.findAuthorityNameByUserName(username)
                .orElse("USER");

        List<SimpleGrantedAuthority> authorities;

        if ("ADMIN".equalsIgnoreCase(authorityName)) {
            authorities = Arrays.asList(
                    new SimpleGrantedAuthority("ADMIN"),
                    new SimpleGrantedAuthority("COLLECTOR"),
                    new SimpleGrantedAuthority("DRIVER")
            );
        } else {
            authorities = Arrays.asList(
                    new SimpleGrantedAuthority(authorityName)
            );
        }

        return new User(staff.getUserName(), staff.getPassword(), authorities);
    }

}
