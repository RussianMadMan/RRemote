package ru.rmm.server.models;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.rmm.rremote.comms.Service;

import java.security.Principal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientService {
    public Principal principal;
    public Service[] services;
}
