package com.opennetwork.secureim.dispatch.io;

import com.opennetwork.secureim.dispatch.redis.PubSubConnection;
import com.opennetwork.secureim.dispatch.redis.PubSubConnection;

public interface RedisPubSubConnectionFactory {

  public PubSubConnection connect();

}
