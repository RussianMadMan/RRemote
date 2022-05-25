package ru.rmm.rremote.comms;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceMessage implements Serializable {
    public String serviceName;
    public String serviceCommand;
    public String messageId;
    public @Nullable HashMap<String, String> params;
}
