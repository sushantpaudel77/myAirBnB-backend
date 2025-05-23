package com.projects.airbnb.utility;

import com.projects.airbnb.exception.ResourceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
public class EntityFinder {

    public <T, I> T findByIdOrThrow(JpaRepository<T, I> repository, I id, String entityName) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(entityName + " not found with the ID: " + id));
    }
}
