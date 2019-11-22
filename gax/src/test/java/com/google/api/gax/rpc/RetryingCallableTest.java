/*
 * Copyright 2019 Google LLC
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
 *     * Neither the name of Google LLC nor the names of its
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

import static com.google.common.truth.Truth.assertThat;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.CurrentMillisClock;
import com.google.api.gax.retrying.ExponentialRetryAlgorithm;
import com.google.api.gax.retrying.RetryAlgorithm;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.retrying.ScheduledRetryingExecutor;
import com.google.api.gax.rpc.StatusCode.Code;
import com.google.api.gax.rpc.testing.FakeCallContext;
import com.google.api.gax.rpc.testing.FakeStatusCode;
import io.grpc.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.threeten.bp.Duration;

public class RetryingCallableTest {
  @Rule
  public final MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  private UnaryCallable<String, String> innerCallable;

  private ScheduledExecutorService scheduler;
  private ScheduledRetryingExecutor<String> retryingExecutor;
  private RetryingCallable<String, String> retryingCallable;

  @Before
  public void setUp() throws Exception {
    scheduler = Executors.newSingleThreadScheduledExecutor();
    retryingExecutor = new ScheduledRetryingExecutor<>(
        new RetryAlgorithm<>(
            new ApiResultRetryAlgorithm<String>(),
            new ExponentialRetryAlgorithm(
                RetrySettings.newBuilder()
                    .setMaxAttempts(5)
                    .setInitialRetryDelay(Duration.ofMillis(5))
                    .setMaxRetryDelay(Duration.ofMillis(5))
                    .setRpcTimeoutMultiplier(1.0)
                    .setTotalTimeout(Duration.ofSeconds(1))
                    .setInitialRetryDelay(Duration.ofMillis(1))
                    .setMaxRetryDelay(Duration.ofMillis(10))
                    .setRetryDelayMultiplier(2.0)
                    .build(),
                CurrentMillisClock.getDefaultClock()
            )
        ),
        scheduler
    );

    retryingCallable = new RetryingCallable<>(
        FakeCallContext.createDefault(),
        innerCallable,
        retryingExecutor
    );
  }

  @Test
  public void testContextIsPropagated() {
    final Context.Key<String> key = Context.key("RetryingCallableTest.testContextIsPropagated");
    String expectedValue = "expected-value";
    final List<String> actualValues = Collections.synchronizedList(new ArrayList<String>());

    Mockito.when(innerCallable.futureCall(Mockito.anyString(), Mockito.any(ApiCallContext.class)))
        .thenAnswer(new Answer<ApiFuture<String>>() {
          @Override
          public ApiFuture<String> answer(InvocationOnMock invocationOnMock) {
            actualValues.add(key.get());
            return ApiFutures.immediateFailedFuture(
                new UnavailableException("transient error", null, FakeStatusCode.of(Code.UNAVAILABLE), true)
            );
          }
        })
        .thenAnswer(new Answer<ApiFuture<String>>() {
          @Override
          public ApiFuture<String> answer(InvocationOnMock invocationOnMock) throws Throwable {
            actualValues.add(key.get());
            return ApiFutures.immediateFuture("result");
          }
        });

    Context prevContext = Context.current()
        .withValue(key, expectedValue)
        .attach();
    try {
      retryingCallable.call("request", FakeCallContext.createDefault());
    } finally {
      prevContext.attach();
    }

    assertThat(actualValues).containsExactly(expectedValue, expectedValue);
  }
}