package com.projects.airbnb.utility;

import com.projects.airbnb.entity.User;
import com.projects.airbnb.exception.UnAuthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


public final class AppUtils {

    private AppUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnAuthorizedException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            throw new UnAuthorizedException("Invalid user principal");
        }

        return (User) principal;
    }
}
