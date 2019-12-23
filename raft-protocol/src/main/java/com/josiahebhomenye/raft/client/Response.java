package com.josiahebhomenye.raft.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response {
    private String id;
    private String correlationId;
    private boolean success;
    private byte[] body;

    public static Response empty(String correlationId, boolean success){
        return empty(UUID.randomUUID().toString(), correlationId, success);
    }

    public static Response empty(String id, String correlationId, boolean success){
        return new Response(id, correlationId, success, new byte[0]);
    }

    public static Response fail(String correlationId, byte[] message) {
        return new Response(UUID.randomUUID().toString(), correlationId, false, message);
    }
}
