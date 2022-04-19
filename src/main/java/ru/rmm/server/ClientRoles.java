package ru.rmm.server;

public enum ClientRoles {
    ROLE_USER("USER", false), ROLE_DEVICE("DEVICE", false), ROLE_SIGNER("SIGNER", true), ROLE_ADMIN("ADMIN", true);
    public String role;
    public boolean privileged;
    ClientRoles(String role, boolean privileged){
        this.role = role;
        this.privileged = privileged;
    }

}
