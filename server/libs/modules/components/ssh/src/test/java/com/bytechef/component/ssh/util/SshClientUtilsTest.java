/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ssh.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.method.AuthKeyboardInteractive;
import net.schmizz.sshj.userauth.method.AuthPassword;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SshClientUtilsTest {

    private static final String PASSWORD = "secret";
    private static final String USERNAME = "user";

    @Test
    void testKeyboardInteractiveIsTriedWhenThePasswordIsRejected() throws Exception {
        SSHClient sshClient = mock(SSHClient.class);

        when(sshClient.isConnected()).thenReturn(true);
        doThrow(new UserAuthException("Exhausted available authentication methods"))
            .when(sshClient)
            .auth(eq(USERNAME), isA(AuthPassword.class));

        assertDoesNotThrow(() -> SshClientUtils.authenticateWithPassword(sshClient, USERNAME, PASSWORD));

        verify(sshClient).auth(eq(USERNAME), isA(AuthKeyboardInteractive.class));
    }

    @Test
    void testKeyboardInteractiveIsNotTriedAfterTheServerClosesTheConnection() throws Exception {
        SSHClient sshClient = mock(SSHClient.class);

        when(sshClient.isConnected()).thenReturn(true);
        doThrow(
            new UserAuthException(
                "Exhausted available authentication methods",
                new UserAuthException(new TransportException("Broken transport; encountered EOF"))))
                    .when(sshClient)
                    .auth(eq(USERNAME), isA(AuthPassword.class));

        assertThrows(
            UserAuthException.class, () -> SshClientUtils.authenticateWithPassword(sshClient, USERNAME, PASSWORD));

        verify(sshClient, never()).auth(eq(USERNAME), isA(AuthKeyboardInteractive.class));
    }

    @Test
    void testKeyboardInteractiveIsNotTriedOnceTheClientIsDisconnected() throws Exception {
        SSHClient sshClient = mock(SSHClient.class);

        when(sshClient.isConnected()).thenReturn(false);
        doThrow(new UserAuthException("Exhausted available authentication methods"))
            .when(sshClient)
            .auth(eq(USERNAME), isA(AuthPassword.class));

        assertThrows(
            UserAuthException.class, () -> SshClientUtils.authenticateWithPassword(sshClient, USERNAME, PASSWORD));

        verify(sshClient, never()).auth(eq(USERNAME), isA(AuthKeyboardInteractive.class));
    }
}
