/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.distributedlog.stream.tests.integration;

import static org.apache.distributedlog.stream.protocol.ProtocolConstants.ROOT_STORAGE_CONTAINER_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.bookkeeper.common.util.OrderedScheduler;
import org.apache.bookkeeper.common.util.Revisioned;
import org.apache.distributedlog.clients.config.StorageClientSettings;
import org.apache.distributedlog.clients.impl.internal.LocationClientImpl;
import org.apache.distributedlog.clients.impl.internal.api.LocationClient;
import org.apache.distributedlog.stream.proto.common.Endpoint;
import org.apache.distributedlog.stream.proto.storage.OneStorageContainerEndpointResponse;
import org.apache.distributedlog.stream.proto.storage.StatusCode;
import org.junit.Test;

/**
 * Integration test for location test.
 */
@Slf4j
public class LocationClientTest extends StorageServerTestBase {

  private OrderedScheduler scheduler;
  private LocationClient client;

  @Override
  protected void doSetup() throws Exception {
    scheduler = OrderedScheduler.newSchedulerBuilder()
      .name("location-client-test")
      .numThreads(1)
      .build();
    StorageClientSettings settings = StorageClientSettings.newBuilder()
      .addEndpoints(cluster.getRpcEndpoints().toArray(new Endpoint[cluster.getRpcEndpoints().size()]))
      .usePlaintext(true)
      .build();
    client = new LocationClientImpl(
      settings,
      scheduler);
  }

  @Override
  protected void doTeardown() throws Exception {
    if (null != client) {
      client.close();
    }
    if (null != scheduler) {
      scheduler.shutdown();
    }
  }

  @Test
  public void testLocateStorageContainers() throws Exception {
    List<OneStorageContainerEndpointResponse> responses = client.locateStorageContainers(
      Lists.newArrayList(
        Revisioned.of(ROOT_STORAGE_CONTAINER_ID, -1L))
    ).get();
    assertEquals(1, responses.size());
    OneStorageContainerEndpointResponse oneResponse = responses.get(0);
    assertEquals(StatusCode.SUCCESS, oneResponse.getStatusCode());

    Endpoint endpoint = oneResponse.getEndpoint().getRwEndpoint();
    log.info("Current cluster endpoints = {}", cluster.getRpcEndpoints());
    log.info("Response : rw endpoint = {}", endpoint);
    assertTrue(cluster.getRpcEndpoints().contains(endpoint));

    assertEquals(1, oneResponse.getEndpoint().getRoEndpointCount());
    endpoint = oneResponse.getEndpoint().getRoEndpoint(0);
    log.info("Response : ro endpoint = {}", endpoint);
    assertTrue(cluster.getRpcEndpoints().contains(endpoint));
  }
}
