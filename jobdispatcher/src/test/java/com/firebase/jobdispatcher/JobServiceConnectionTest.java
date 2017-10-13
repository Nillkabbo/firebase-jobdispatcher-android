// Copyright 2016 Google, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.firebase.jobdispatcher;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.IBinder;
import android.os.Message;
import com.firebase.jobdispatcher.JobInvocation.Builder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Test for {@link JobServiceConnection}. */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 23)
public class JobServiceConnectionTest {

  JobInvocation job =
      new Builder()
          .setTag("tag")
          .setService(TestJobService.class.getName())
          .setTrigger(Trigger.NOW)
          .build();

  @Mock Message messageMock;
  @Mock JobService.LocalBinder binderMock;
  @Mock JobService jobServiceMock;

  @Mock Context contextMock;
  JobServiceConnection connection;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(binderMock.getService()).thenReturn(jobServiceMock);
    connection = new JobServiceConnection(job, messageMock, contextMock);
  }

  @Test
  public void fullConnectionCycle() {
    assertFalse(connection.wasUnbound());
    connection.onServiceConnected(null, binderMock);
    verify(jobServiceMock).start(job, messageMock);
    assertFalse(connection.wasUnbound());

    connection.onStop(true);
    verify(jobServiceMock).stop(job, true);
    assertTrue(connection.wasUnbound());

    connection.onServiceDisconnected(null);
    assertTrue(connection.wasUnbound());
  }

  @Test
  public void onServiceDisconnected() {
    connection.onServiceConnected(null, binderMock);
    verify(jobServiceMock).start(job, messageMock);
    assertFalse(connection.wasUnbound());

    connection.onServiceDisconnected(null);
    assertTrue(connection.wasUnbound());
  }

  @Test
  public void onServiceConnected_shouldNotSendExecutionRequestTwice() {
    assertFalse(connection.wasUnbound());

    connection.onServiceConnected(null, binderMock);
    verify(jobServiceMock).start(job, messageMock);
    assertFalse(connection.wasUnbound());
    reset(jobServiceMock);

    connection.onServiceConnected(null, binderMock);
    verify(jobServiceMock, never()).start(job, messageMock); // start should not be called again

    connection.onStop(true);
    verify(jobServiceMock).stop(job, true);
    assertTrue(connection.wasUnbound());

    connection.onServiceDisconnected(null);
    assertTrue(connection.wasUnbound());
  }

  @Test
  public void stopOnUnboundConnection_nothingHappens() {
    assertFalse(connection.wasUnbound());

    connection.onStop(true);

    assertTrue(connection.wasUnbound());
    verify(contextMock).unbindService(connection);
  }

  @Test
  public void onServiceConnectedWrongBinder_doesNotThrow() {
    IBinder binder = mock(IBinder.class);
    connection.onServiceConnected(null, binder);
  }

  @Test
  public void onStop_doNotSendResult() {
    connection.onServiceConnected(null, binderMock);
    verify(jobServiceMock).start(job, messageMock);
    assertFalse(connection.wasUnbound());

    connection.onStop(false);
    verify(jobServiceMock).stop(job, false);
    assertTrue(connection.wasUnbound());
  }

  @Test
  public void unbind() {
    connection.onServiceConnected(null, binderMock);
    verify(jobServiceMock).start(job, messageMock);
    assertFalse(connection.wasUnbound());

    connection.unbind();

    assertTrue(connection.wasUnbound());
    verify(contextMock).unbindService(connection);
  }

  @Test
  public void unbind_throws_noException() {
    connection.onServiceConnected(null, binderMock);
    verify(jobServiceMock).start(job, messageMock);
    assertFalse(connection.wasUnbound());

    doThrow(IllegalArgumentException.class).when(contextMock).unbindService(connection);

    connection.unbind();

    assertTrue(connection.wasUnbound());
    verify(contextMock).unbindService(connection);
  }
}
