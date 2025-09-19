package com.sportfd.healthapp.integration;

import com.sportfd.healthapp.model.enums.Provider;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ProviderClients {
    private final Map<Provider, ProviderClient> map = new EnumMap<>(Provider.class);

    public ProviderClients(List<ProviderClient> clients) {
        for (var c : clients) map.put(c.provider(), c);
    }
    public ProviderClient get(Provider p) {
        var c = map.get(p);
        if (c == null) throw new IllegalArgumentException("Provider not supported: " + p);
        return c;
    }
}