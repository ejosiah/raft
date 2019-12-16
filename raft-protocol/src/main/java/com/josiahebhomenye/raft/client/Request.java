package com.josiahebhomenye.raft.client;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Request {
    private String id = UUID.randomUUID().toString();
    byte[] body;

    public Request(byte[] body){
        this.body = body;
    }
}
