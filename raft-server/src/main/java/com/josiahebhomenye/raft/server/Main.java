package com.josiahebhomenye.raft.server;

import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.core.Node;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Main {

    public static void main(String[] args) {
       ServerConfig config = new ServerConfig(ConfigFactory.load());
       Node node = new Node(config);
       node.start();

       Runtime.getRuntime().addShutdownHook(new Thread(node::stop));
    }
}
