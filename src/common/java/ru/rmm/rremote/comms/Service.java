package ru.rmm.rremote.comms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Service implements Serializable {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServiceCommand implements Serializable{
        public String name;
        public String[] parameters;
    }
    public String name;
    public ServiceCommand[] commands;
}
