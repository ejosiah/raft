package com.josiahebhomenye.raft.client;

import com.josiahebhomenye.raft.client.config.ClientConfig;
import com.josiahebhomenye.raft.client.support.ServerMock;
import com.josiahebhomenye.raft.client.support.StringEntrySerializer;
import com.josiahebhomenye.raft.rpc.Redirect;
import com.typesafe.config.ConfigFactory;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.ConnectionPendingException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class RaftClientTest {


    ServerMock serverMock;
    ServerMock serverMock1;
    ClientConfig config;
    RaftClient<String> raftClient;

    @Before
    public void setup() throws Exception{
        config = new ClientConfig(ConfigFactory.load()).withEntrySerializerClass(StringEntrySerializer.class);
        serverMock = new ServerMock(config.servers.get(0));
        raftClient = new RaftClient<>(config);
        serverMock.start();
        raftClient.start();
    }

    @After
    public void tearDown() throws Exception{
        raftClient.stop().get();
        serverMock.stop();
        if(serverMock1 != null){
            serverMock1.stop();
        }
    }

    @Test
    public void upstream_should_Successfully_respond_when_client_sends_a_request() throws Exception{
        serverMock.whenRequest((ctx, msg) -> {
            Response response = new Response(UUID.randomUUID().toString(), msg.getId(), true, "ROGER, OVER AND OUT".getBytes());
            ctx.channel().writeAndFlush(response);
        });

        CompletableFuture<Response> future = raftClient.send("RADIO CHECK");

        String expected = "ROGER, OVER AND OUT";
        String actual = new String(future.get().getBody());

        assertEquals("server did not respond as expected",expected, actual);
    }

    @Test
    public void timeout_when_server_does_not_respond() throws Exception{

        try {
            raftClient.send("RADIO CHECK").get(5000, TimeUnit.MILLISECONDS);
        }catch(ExecutionException ex){
            assertEquals("did not receive response from server after " + config.requestTimeout + " ms", ex.getCause().getMessage());
        }
    }

    @Test
    public void try_all_servers_until_connection_found() throws Exception{
        tearDown();
        serverMock = new ServerMock(config.servers.get(4));
        raftClient = new RaftClient<>(config);
        serverMock.start();
        raftClient.start();
        upstream_should_Successfully_respond_when_client_sends_a_request();
    }

    @Test
    public void redirect_request_to_leader_when_redirect_msg_received(){
        serverMock1 = new ServerMock(config.servers.get(1));
        serverMock1.start();

        serverMock.whenRequest((ctx, req) -> {
            ctx.channel().writeAndFlush(new Redirect(serverMock1.address, req));
        });

        assertResponseFromServer(serverMock1);
    }

    @Test(expected = ClosedChannelException.class)
    public void throw_connection_exception_when_channel_is_offline() throws Throwable{
        serverMock.stop();
        try {
            raftClient.send("RADIO CHECK").get(5000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw e.getCause();
        }
    }

    @Test
    public void try_another_server_when_current_connection_lost() throws Exception{
        assertNotEquals(config.servers.get(0), config.servers.get(1));
        assertEquals(config.servers.get(0), raftClient.channel().remoteAddress());

        serverMock1 = new ServerMock(config.servers.get(1));
        serverMock1.start();

        serverMock.stop();
        Thread.sleep(2000);

        assertEquals(config.servers.get(1), raftClient.channel().remoteAddress());
        assertResponseFromServer(serverMock1);
    }

    @Test(expected = ClosedChannelException.class)
    public void fail_redirect_message_when_leader_is_offline() throws Throwable{
        serverMock.whenRequest((ctx, req) -> {
            ctx.channel().writeAndFlush(new Redirect(config.servers.get(1), req));
        });

        try {
            raftClient.send("RADIO CHECK").get(5000, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw e.getCause();
        }

        assertResponseFromServer(serverMock);
    }

    @SneakyThrows
    private void assertResponseFromServer(ServerMock serverMock){
        serverMock.whenRequest((ctx, msg) -> {
            Response response = new Response(UUID.randomUUID().toString(), msg.getId(), true, "ROGER, OVER AND OUT".getBytes());
            ctx.channel().writeAndFlush(response);
        });

        CompletableFuture<Response> future = raftClient.send("RADIO CHECK");

        String expected = "ROGER, OVER AND OUT";
        String actual = new String(future.get().getBody());

        assertEquals("server did not respond as expected",expected, actual);
    }
}