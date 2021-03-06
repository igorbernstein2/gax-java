/*
 * Copyright 2017, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.google.api.gax.rpc;

import com.google.api.core.ApiClock;
import com.google.api.core.NanoClock;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.rpc.testing.FakeClientSettings;
import com.google.auth.Credentials;
import com.google.common.truth.Truth;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public class ClientSettingsTest {

  @Test
  public void testEmptyBuilder() throws Exception {
    FakeClientSettings.Builder builder = new FakeClientSettings.Builder();
    Truth.assertThat(builder.getExecutorProvider())
        .isInstanceOf(InstantiatingExecutorProvider.class);
    Truth.assertThat(builder.getTransportProvider()).isNull();
    Truth.assertThat(builder.getCredentialsProvider()).isInstanceOf(NoCredentialsProvider.class);
    Truth.assertThat(builder.getClock()).isInstanceOf(NanoClock.class);

    FakeClientSettings settings = builder.build();
    Truth.assertThat(settings.getExecutorProvider()).isSameAs(builder.getExecutorProvider());
    Truth.assertThat(settings.getTransportProvider()).isSameAs(builder.getTransportProvider());
    Truth.assertThat(settings.getCredentialsProvider()).isSameAs(builder.getCredentialsProvider());
    Truth.assertThat(settings.getClock()).isSameAs(builder.getClock());

    String settingsString = settings.toString();
    Truth.assertThat(settingsString).contains("executorProvider");
    Truth.assertThat(settingsString).contains("transportProvider");
    Truth.assertThat(settingsString).contains("credentialsProvider");
    Truth.assertThat(settingsString).contains("clock");
  }

  @Test
  public void testBuilder() throws Exception {
    FakeClientSettings.Builder builder = new FakeClientSettings.Builder();

    ExecutorProvider executorProvider = Mockito.mock(ExecutorProvider.class);
    TransportProvider transportProvider = Mockito.mock(TransportProvider.class);
    CredentialsProvider credentialsProvider = Mockito.mock(CredentialsProvider.class);
    ApiClock clock = Mockito.mock(ApiClock.class);

    builder.setExecutorProvider(executorProvider);
    builder.setTransportProvider(transportProvider);
    builder.setCredentialsProvider(credentialsProvider);
    builder.setClock(clock);

    Truth.assertThat(builder.getExecutorProvider()).isSameAs(executorProvider);
    Truth.assertThat(builder.getTransportProvider()).isSameAs(transportProvider);
    Truth.assertThat(builder.getCredentialsProvider()).isSameAs(credentialsProvider);
    Truth.assertThat(builder.getClock()).isSameAs(clock);

    String builderString = builder.toString();
    Truth.assertThat(builderString).contains("executorProvider");
    Truth.assertThat(builderString).contains("transportProvider");
    Truth.assertThat(builderString).contains("credentialsProvider");
    Truth.assertThat(builderString).contains("clock");
  }

  @Test
  public void testBuilderFromClientContext() throws Exception {
    ApiClock clock = Mockito.mock(ApiClock.class);

    ClientContext clientContext =
        ClientContext.newBuilder()
            .setExecutor(Mockito.mock(ScheduledExecutorService.class))
            .setTransportContext(Mockito.mock(Transport.class))
            .setCredentials(Mockito.mock(Credentials.class))
            .setClock(clock)
            .build();

    FakeClientSettings.Builder builder = new FakeClientSettings.Builder(clientContext);

    Truth.assertThat(builder.getExecutorProvider()).isInstanceOf(FixedExecutorProvider.class);
    Truth.assertThat(builder.getTransportProvider())
        .isInstanceOf(FixedContextTransportProvider.class);
    Truth.assertThat(builder.getCredentialsProvider()).isInstanceOf(FixedCredentialsProvider.class);
    Truth.assertThat(builder.getClock()).isSameAs(clock);
  }

  @Test
  public void testBuilderFromSettings() throws Exception {
    FakeClientSettings.Builder builder = new FakeClientSettings.Builder();

    ExecutorProvider executorProvider = Mockito.mock(ExecutorProvider.class);
    TransportProvider transportProvider = Mockito.mock(TransportProvider.class);
    CredentialsProvider credentialsProvider = Mockito.mock(CredentialsProvider.class);
    ApiClock clock = Mockito.mock(ApiClock.class);

    builder.setExecutorProvider(executorProvider);
    builder.setTransportProvider(transportProvider);
    builder.setCredentialsProvider(credentialsProvider);
    builder.setClock(clock);

    FakeClientSettings settings = builder.build();
    FakeClientSettings.Builder newBuilder = new FakeClientSettings.Builder(settings);

    Truth.assertThat(newBuilder.getExecutorProvider()).isSameAs(executorProvider);
    Truth.assertThat(newBuilder.getTransportProvider()).isSameAs(transportProvider);
    Truth.assertThat(newBuilder.getCredentialsProvider()).isSameAs(credentialsProvider);
    Truth.assertThat(newBuilder.getClock()).isSameAs(clock);
  }
}
