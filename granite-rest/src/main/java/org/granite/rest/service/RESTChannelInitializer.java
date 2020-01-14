package org.granite.rest.service;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public abstract class RESTChannelInitializer extends ChannelInitializer<SocketChannel> {

  @Override
  protected void initChannel(final SocketChannel ch) throws Exception {
    ch.config().setKeepAlive(true);

    ch
        .pipeline()
        .addLast(new HttpServerCodec())
        .addLast(new HttpObjectAggregator(1048576))
        .addLast(getInboundRequestHandlerInstance());
  }

  protected abstract InboundRequestHandler getInboundRequestHandlerInstance();
}
