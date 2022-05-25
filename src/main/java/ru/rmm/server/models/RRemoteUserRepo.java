package ru.rmm.server.models;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RRemoteUserRepo extends CrudRepository<RRemoteUser, Long> {

    RRemoteUser findByUsername(String username);

    List<RRemoteUser> findRRemoteUsersByFriendsContains(RRemoteUser user);

}
