package com.laundromat.smart_laundromat.reservation;


import org.springframework.stereotype.Component;

@Component
public class TemporaryCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Long currentUserId() {
        // Temporary user ID for backend testing
        return 1L;
    }

    @Override
    public boolean isAdmin() {
        // Temporary admin permission for development
        return true;
    }
}