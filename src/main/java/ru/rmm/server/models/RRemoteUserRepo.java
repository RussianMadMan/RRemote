package ru.rmm.server.models;

import org.springframework.data.repository.CrudRepository;

public interface RRemoteUserRepo extends CrudRepository<RRemoteUser, Long> {

    RRemoteUser findByUsername(String username);

}
