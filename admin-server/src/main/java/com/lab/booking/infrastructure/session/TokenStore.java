package com.lab.booking.infrastructure.session;

import java.util.Optional;

public interface TokenStore {

    String createToken(Long userId);

    Optional<Long> findUserIdByToken(String token);

    String storageType();
}
