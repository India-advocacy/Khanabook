package com.khanabook.saas.repository;

import com.khanabook.saas.entity.User;
import com.khanabook.saas.sync.repository.SyncRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends SyncRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
