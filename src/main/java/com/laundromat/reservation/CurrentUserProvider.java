package com.laundromat.smart_laundromat.reservation;

/*
 * Temporary interface to test authentication with reservation
 */
public interface CurrentUserProvider {

    // Returns the currently logged-in user's database ID
    Long currentUserId();

    // Returns true if the current user is an administrator
    boolean isAdmin();
}
