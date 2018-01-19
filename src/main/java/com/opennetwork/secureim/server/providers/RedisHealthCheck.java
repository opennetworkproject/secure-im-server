package com.opennetwork.secureim.server.providers;

import com.codahale.metrics.health.HealthCheck;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisHealthCheck extends HealthCheck {

  private final JedisPool clientPool;

  public RedisHealthCheck(JedisPool clientPool) {
    this.clientPool = clientPool;
  }

  @Override
  protected Result check() throws Exception {
    try (Jedis client = clientPool.getResource()) {
      client.set("HEALTH", "test");

      if (!"test".equals(client.get("HEALTH"))) {
        return Result.unhealthy("fetch failed");
      }

      return Result.healthy();
    }
  }
}
