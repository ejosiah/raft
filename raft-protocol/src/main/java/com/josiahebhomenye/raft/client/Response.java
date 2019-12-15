package com.josiahebhomenye.raft.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response {
    private String id;
    private String correlationId;
    private boolean success;
    private byte[] body;
}
