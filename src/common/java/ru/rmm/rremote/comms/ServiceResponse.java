package ru.rmm.rremote.comms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceResponse implements Serializable {
    public String messageId;
    public String response;
}
