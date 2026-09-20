# SSH Component

ByteChef component for running shell commands on a remote host over SSH. It uses the
[sshj](https://github.com/hierynomus/sshj) library, the same library the FTP component uses for its SFTP transport.

## Connection

The connection offers two authentication methods.

### Username and Password

| Field | Required | Default | Description |
|---|---|---|---|
| Host | yes | — | Hostname or IP address of the SSH server |
| Port | no | 22 | Port the SSH server listens on |
| Username | yes | — | Username used to log in |
| Password | yes | — | Password of that user |
| Host Key Fingerprint | no | — | Expected fingerprint of the server's host key |

### Private Key

| Field | Required | Default | Description |
|---|---|---|---|
| Host | yes | — | Hostname or IP address of the SSH server |
| Port | no | 22 | Port the SSH server listens on |
| Username | yes | — | Username used to log in |
| Private Key | yes | — | Private key in OpenSSH or PEM format, header and footer line included |
| Passphrase | no | — | Passphrase protecting the private key, when the key is encrypted |
| Host Key Fingerprint | no | — | Expected fingerprint of the server's host key |

**Host key verification:** when the fingerprint is left empty, any host key the server presents is accepted, which
leaves the connection open to man-in-the-middle attacks. Fill it in for production hosts. Both the
`SHA256:<base64>` form printed by `ssh-keygen -lf <host key>` and the colon-separated MD5 form are accepted, and the
fingerprint has to belong to the host key algorithm the server negotiates.

## Actions

### Execute Commands

Runs shell commands on the remote host, in the order they are listed, over a single SSH connection.

**Input**

| Parameter | Required | Description |
|---|---|---|
| Commands | yes | Array of shell commands to execute |
| Continue On Error | no | Keep going when a command exits with a non-zero exit status (default: `false`) |
| Timeout | no | Seconds to wait for a single command to finish (default: `60`) |

**Output**

An array with one object per executed command:

```json
[
  {
    "command": "sudo systemctl start tomcat",
    "result": "started",
    "error": "",
    "exitStatus": 0
  }
]
```

- `result` is the standard output of the command, `error` its standard error output. Trailing whitespace and the
  trailing newline are stripped from both.
- `exitStatus` is the exit status of the command, `0` on success.

**Each command runs in its own session.** A working directory or an environment variable set by one command is not
visible to the next one, because every command gets a fresh SSH session on the shared connection. Chain such commands
instead:

```text
cd /var/log && ls -la
```

**Error handling**

- A command that exits with a non-zero status stops the execution, and the action returns the results collected so
  far, the failed command included. Enable **Continue On Error** to run the remaining commands anyway.
- A command that does not finish within the timeout, and a connection or authentication failure, fail the action.
