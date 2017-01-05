package org.granite.rest.service;

import com.google.common.base.Throwables;

import org.granite.log.LogTools;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class RESTService {

    private Channel channel;
    private final RESTChannelInitializer restChannelInitializer;
    private final int port;
    public static final ConcurrentHashMap<String, AtomicLong> COUNTER_MAP = new ConcurrentHashMap<>();

    public RESTService(
            final int port,
            final RESTChannelInitializer restChannelInitializer
    ) {
        checkArgument(port > 0, "port must be a positive integer");
        this.port = port;
        this.restChannelInitializer = checkNotNull(restChannelInitializer,
                                                   "restChannelInitializer");

        for (ServiceCounters serviceCounter : ServiceCounters.values()) {
            COUNTER_MAP.computeIfAbsent(serviceCounter.name(), key -> new AtomicLong());
        }

    }

    public void start() {
        NioEventLoopGroup parentGroup = new NioEventLoopGroup();

        NioEventLoopGroup childGroup = new NioEventLoopGroup();

        try {

            setCounter(
                    ServiceCounters.StartTimestamp,
                    Clock.systemUTC().millis());

            ServerBootstrap serverBootstrap = new ServerBootstrap();

            serverBootstrap
                    .group(parentGroup, childGroup)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .channel(NioServerSocketChannel.class)
                    .childHandler(this.restChannelInitializer);

            channel = serverBootstrap
                    .bind(port)
                    .sync()
                    .channel();

            LogTools.info("Server now listening on port: {0}", String.valueOf(port));

            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        } finally {
            parentGroup.shutdownGracefully();
            childGroup.shutdownGracefully();
        }
    }

    public static void incrementHiddenErrorCount(){
        incrementCounter(ServiceCounters.HiddenErrors);
    }

    public static void incrementRequestCount(){
        incrementCounter(ServiceCounters.Requests);
    }

    public static void incrementResponseCount(){
        incrementCounter(ServiceCounters.Responses);
    }

    public static void setLastRequestTime(){
        setCounter(
                ServiceCounters.LastRequestTimestamp,
                Clock.systemUTC().millis());

    }

    public static void incrementCounter(ServiceCounters serviceCounter) {
        incrementCounter(serviceCounter.name());
    }

    public static void incrementCounter(String counter){
        COUNTER_MAP
                .computeIfAbsent(
                        counter,
                        key -> new AtomicLong(0)
                )
                .getAndIncrement();
    }

    public static void setCounter(ServiceCounters serviceCounter, long value){
        setCounter(serviceCounter.name(), value);
    }
    public static void setCounter(String counter, long value){
        COUNTER_MAP
                .computeIfAbsent(
                        counter,
                        key -> new AtomicLong(0)
                )
                .set(value);
    }

    public void shutdown() {
        LogTools.info("Closing channel...");
        channel.flush();
        channel.close();
    }

    public enum ServiceCounters {
        StartTimestamp,
        Requests,
        Responses,
        HiddenErrors,
        LastRequestTimestamp
    }

}
