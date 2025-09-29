package com.sportfd.healthapp.model;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class PkceStore {
    private static class Item {
        String verifier;
        Instant exp;
    }

    private final Map<String, Item> map = new ConcurrentHashMap<>();

    public void put(String state, String codeVerifier) {
        Item it = new Item();
        it.verifier = codeVerifier;
        it.exp = Instant.now().plusSeconds(600); // живёт 10 минут
        map.put(state, it);
    }

    /** достаём и удаляем */
    public String take(String state) {
        Item it = map.remove(state);
        if (it == null) return null;
        if (it.exp.isBefore(Instant.now())) return null;
        return it.verifier;
    }
}
