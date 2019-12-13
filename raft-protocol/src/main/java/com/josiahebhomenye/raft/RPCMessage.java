package com.josiahebhomenye.raft;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RPCMessage {
    private Long id;
    private Long correlationId;
}
