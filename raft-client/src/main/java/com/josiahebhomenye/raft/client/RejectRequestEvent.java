package com.josiahebhomenye.raft.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@AllArgsConstructor
public class RejectRequestEvent {
    private String id;
    private Exception reason;
}
