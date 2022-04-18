package ru.rmm.server;

public enum ClientRoles {
    ROLE_UNKNOWN("0"), ROLE_USER("user"), ROLE_DEVICE("device"), ROLE_CODE("signer");
    public String role;
    ClientRoles(String role){
        this.role = role;
    }

}
