package org.granite.rest.service;

import com.google.common.base.Throwables;

import org.granite.log.LogTools;

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

    public RESTService(
            final int port,
            final RESTChannelInitializer restChannelInitializer
    ){
        checkArgument(port > 0, "port must be a positive integer");
        this.port = port;
        this.restChannelInitializer = checkNotNull(restChannelInitializer, "restChannelInitializer");
    }

    public void start(){
        NioEventLoopGroup parentGroup = new NioEventLoopGroup();

        NioEventLoopGroup childGroup = new NioEventLoopGroup();

        try {

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

    public void shutdown(){
        LogTools.info("Closing channel...");
        channel.flush();
        channel.close();
    }
}
