package com.opennetwork.secureim.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opennetwork.secureim.server.configuration.ApnConfiguration;
import com.opennetwork.secureim.server.configuration.FederationConfiguration;
import com.opennetwork.secureim.server.configuration.GcmConfiguration;
import com.opennetwork.secureim.server.configuration.MaxDeviceConfiguration;
import com.opennetwork.secureim.server.configuration.ProfilesConfiguration;
import com.opennetwork.secureim.server.configuration.PushConfiguration;
import com.opennetwork.secureim.server.configuration.RateLimitsConfiguration;
import com.opennetwork.secureim.server.configuration.RedPhoneConfiguration;
import com.opennetwork.secureim.server.configuration.RedisConfiguration;
import com.opennetwork.secureim.server.configuration.AttachmentsConfiguration;
import com.opennetwork.secureim.server.configuration.TestDeviceConfiguration;
import com.opennetwork.secureim.server.configuration.TurnConfiguration;
import com.opennetwork.secureim.server.configuration.TwilioConfiguration;
import com.opennetwork.secureim.websocket.configuration.WebSocketConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.opennetwork.secureim.server.configuration.*;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.db.DataSourceFactory;

public class SecureImServerConfiguration extends Configuration {

  @NotNull
  @Valid
  @JsonProperty
  private TwilioConfiguration twilio;

  @NotNull
  @Valid
  @JsonProperty
  private PushConfiguration push;

  @NotNull
  @Valid
  @JsonProperty
  private AttachmentsConfiguration attachments;

  @NotNull
  @Valid
  @JsonProperty
  private ProfilesConfiguration profiles;

  @NotNull
  @Valid
  @JsonProperty
  private RedisConfiguration cache;

  @NotNull
  @Valid
  @JsonProperty
  private RedisConfiguration directory;

  @Valid
  @NotNull
  @JsonProperty
  private DataSourceFactory messageStore;

  @Valid
  @NotNull
  @JsonProperty
  private List<TestDeviceConfiguration> testDevices = new LinkedList<>();

  @Valid
  @NotNull
  @JsonProperty
  private List<MaxDeviceConfiguration> maxDevices = new LinkedList<>();

  @Valid
  @JsonProperty
  private FederationConfiguration federation = new FederationConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private DataSourceFactory database = new DataSourceFactory();

  @JsonProperty
  private DataSourceFactory read_database;

  @Valid
  @NotNull
  @JsonProperty
  private RateLimitsConfiguration limits = new RateLimitsConfiguration();

  @JsonProperty
  private RedPhoneConfiguration redphone = new RedPhoneConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private JerseyClientConfiguration httpClient = new JerseyClientConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private WebSocketConfiguration webSocket = new WebSocketConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private TurnConfiguration turn;

  @Valid
  @NotNull
  @JsonProperty
  private GcmConfiguration gcm;

  @Valid
  @NotNull
  @JsonProperty
  private ApnConfiguration apn;

  public WebSocketConfiguration getWebSocketConfiguration() {
    return webSocket;
  }

  public TwilioConfiguration getTwilioConfiguration() {
    return twilio;
  }

  public PushConfiguration getPushConfiguration() {
    return push;
  }

  public JerseyClientConfiguration getJerseyClientConfiguration() {
    return httpClient;
  }

  public AttachmentsConfiguration getAttachmentsConfiguration() {
    return attachments;
  }

  public RedisConfiguration getCacheConfiguration() {
    return cache;
  }

  public RedisConfiguration getDirectoryConfiguration() {
    return directory;
  }

  public DataSourceFactory getMessageStoreConfiguration() {
    return messageStore;
  }

  public DataSourceFactory getDataSourceFactory() {
    return database;
  }

  public DataSourceFactory getReadDataSourceFactory() {
    return read_database;
  }

  public RateLimitsConfiguration getLimitsConfiguration() {
    return limits;
  }

  public FederationConfiguration getFederationConfiguration() {
    return federation;
  }

  public RedPhoneConfiguration getRedphoneConfiguration() {
    return redphone;
  }

  public TurnConfiguration getTurnConfiguration() {
    return turn;
  }

  public GcmConfiguration getGcmConfiguration() {
    return gcm;
  }

  public ApnConfiguration getApnConfiguration() {
    return apn;
  }

  public ProfilesConfiguration getProfilesConfiguration() {
    return profiles;
  }

  public Map<String, Integer> getTestDevices() {
    Map<String, Integer> results = new HashMap<>();

    for (TestDeviceConfiguration testDeviceConfiguration : testDevices) {
      results.put(testDeviceConfiguration.getNumber(),
                  testDeviceConfiguration.getCode());
    }

    return results;
  }

  public Map<String, Integer> getMaxDevices() {
    Map<String, Integer> results = new HashMap<>();

    for (MaxDeviceConfiguration maxDeviceConfiguration : maxDevices) {
      results.put(maxDeviceConfiguration.getNumber(),
                  maxDeviceConfiguration.getCount());
    }

    return results;
  }

}
