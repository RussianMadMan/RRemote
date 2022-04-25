package ru.rmm.rremote.comms;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceMessage {
    public String serviceName;
    public String serviceCommand;
    public @Nullable Map<String, String> params;
}
