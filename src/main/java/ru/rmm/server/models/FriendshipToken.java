package ru.rmm.server.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@AllArgsConstructor
public class FriendshipToken {
    public String name;


    public Date validThrough;
}
