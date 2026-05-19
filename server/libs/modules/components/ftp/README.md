# FTP Component

ByteChef component for transferring files over FTP (File Transfer Protocol) and SFTP (SSH File Transfer Protocol). Both protocols share the same set of actions and connection configuration — the **Use SFTP** toggle switches the transport layer.

## Connection

| Field | Required | Default | Description |
|---|---|---|---|
| Host | yes | — | Hostname or IP address of the server |
| Port | no | 21 (FTP) / 22 (SFTP) | Server port; defaults based on the SFTP toggle |
| Username | yes | — | Authentication username |
| Password | yes | — | Authentication password |
| Passive Mode | no | `true` | FTP only. Use passive mode for data connections (recommended behind firewalls) |
| Use SFTP | no | `false` | Switch to SFTP (encrypted transfer over SSH) |

**Passive vs active FTP mode:** In passive mode the client opens both connections (command and data) to the server, which works through most firewalls and NAT. In active mode the server opens the data connection back to the client, which often fails when the client is behind a firewall. Leave passive mode enabled unless your server specifically requires active mode.

## Actions

### Upload File

Uploads a file to the server.

**Input**

| Parameter | Required | Description |
|---|---|---|
| File | yes | The file to upload |
| Remote Path | yes | Full destination path on the server including filename, e.g. `/uploads/report.pdf` |
| Create Directories | no | Create the directory tree on the server if it does not exist (default: `false`) |

**Output**

```json
{
  "remotePath": "/uploads/report.pdf",
  "success": true
}
```

---

### Download File

Downloads a file from the server and stores it in ByteChef's file storage.

**Input**

| Parameter | Required | Description |
|---|---|---|
| Remote Path | yes | Path of the file on the server to download, e.g. `/downloads/report.pdf` |

**Output**

A `FileEntry` object that can be passed to subsequent actions (e.g. to parse, transform, or re-upload elsewhere).

---

### List Directory

Lists the contents of a directory.

**Input**

| Parameter | Required | Default | Description |
|---|---|---|---|
| Path | yes | `/` | Directory path to list |
| Recursive | no | `false` | Descend into subdirectories |

**Output**

```json
[
  {
    "name": "report.pdf",
    "path": "/uploads/report.pdf",
    "type": "file",
    "size": 204800,
    "modifiedAt": "2025-04-01T12:00:00Z"
  },
  {
    "name": "archive",
    "path": "/uploads/archive",
    "type": "directory",
    "size": 0,
    "modifiedAt": "2025-03-15T08:30:00Z"
  }
]
```

---

### Delete

Deletes a file or directory from the server.

**Input**

| Parameter | Required | Default | Description |
|---|---|---|---|
| Path | yes | — | Path of the file or directory to delete |
| Recursive | no | `false` | Delete directory contents recursively before removing the directory itself |

**Output**

```json
{
  "deletedPath": "/uploads/old-file.pdf",
  "success": true
}
```

---

### Rename / Move

Renames or moves a file or directory on the server.

**Input**

| Parameter | Required | Default | Description |
|---|---|---|---|
| Source Path | yes | — | Current path of the file or directory |
| Destination Path | yes | — | New path (can be a different directory to achieve a move) |
| Create Directories | no | `false` | Create the destination directory structure if it does not exist |

**Output**

```json
{
  "oldPath": "/uploads/draft.pdf",
  "newPath": "/archive/final.pdf",
  "success": true
}
```

---

## Implementation Notes

### Protocol Abstraction — `RemoteFileClient`

Both FTP and SFTP are hidden behind the `RemoteFileClient` interface. The factory method `RemoteFileClient.of(connectionParameters)` inspects the `sftp` connection parameter and returns either `FtpRemoteFileClient` (Apache Commons Net) or `SftpRemoteFileClient` (sshj). All actions depend only on the interface, so adding a third transport would not require touching any action class.

```
RemoteFileClient (interface)
├── FtpRemoteFileClient   — Apache Commons Net FTPClient
└── SftpRemoteFileClient  — sshj SSHClient + SFTPClient
```

`RemoteFileClient` implements `Closeable`. Every action wraps the client in a try-with-resources block, which guarantees the underlying socket is closed even when an exception occurs mid-transfer.

### SFTP Host Key Verification

`RemoteFileClient` always registers a `PromiscuousVerifier` before connecting via SSH. This accepts any server host key without checking `~/.ssh/known_hosts`. The rationale: ByteChef is a workflow automation platform where connections are operator-configured, not interactive — the standard `known_hosts` trust model does not apply, and silently failing because a container's ephemeral key is not pre-registered would be worse than promiscuous verification for this use case.

### File Download — Pipe-Based Streaming

`FtpDownloadFileAction` uses a `PipedInputStream` / `PipedOutputStream` pair to stream the download directly into `context.file()` without buffering the entire file in heap memory:

```
writer thread                   main thread
─────────────────────────────────────────────────────────
remoteFileClient.retrieveFile   context.file(file ->
  (remotePath,                    file.storeContent(
   pipedOutputStream))              filename,
                                    pipedInputStream))
```

The writer thread writes chunks into `pipedOutputStream`; the main thread reads them out of `pipedInputStream` and passes the stream to the storage backend. `pipedOutputStream.close()` in the writer thread's `finally` block signals EOF to the reader.

---

## Integration Tests

The module includes Testcontainers-based integration tests that exercise the full FTP and SFTP download stack against real server containers.

### Running the Tests

```bash
./gradlew :server:libs:modules:components:ftp:testIntegration
```

Docker must be running. The test pulls `delfer/alpine-ftp-server` and `atmoz/sftp` on first run.

### How the Test Infrastructure Works

#### Containers

```java
// FTP — fixed passive-mode port so PASV advertises the right host port
@Container
static final FixedHostPortGenericContainer<?> ftpContainer =
    new FixedHostPortGenericContainer<>("delfer/alpine-ftp-server:latest")
        .withEnv("USERS", "ftpuser|T3st@Pass123")
        .withEnv("ADDRESS", "127.0.0.1")   // embedded in PASV response
        .withEnv("MIN_PORT", "30121")
        .withEnv("MAX_PORT", "30121")
        .withExposedPorts(21)
        .withFixedExposedPort(30121, 30121);

// SFTP — single SSH port, no passive-mode complexity
@Container
static final GenericContainer<?> sftpContainer =
    new GenericContainer<>("atmoz/sftp:latest")
        .withCommand("ftpuser:T3st@Pass123:::upload")
        .withExposedPorts(22);
```

`static @Container` fields are started once before any test runs and stopped after the last test, so container startup cost is paid only once per test class.

**Why `FixedHostPortGenericContainer` for FTP?**
FTP passive mode (PASV) embeds the data-port number in the server's response. The FTP client connects to that exact port on the host. If Docker maps the container's port 30121 to a random host port, the server's PASV reply still advertises 30121, and the client connects to the wrong port. `withFixedExposedPort(30121, 30121)` pins host and container to the same number so they agree.

#### Test File Seeding

```java
@BeforeAll
static void uploadTestFiles() throws Exception {
    uploadTestFileViaFtp();   // writes file into FTP container
    uploadTestFileViaSftp();  // uploads via real SFTP client
}
```

The FTP fixture is seeded via `execInContainer` (not via FTP upload). This avoids passive-mode complexity for the setup step while still testing the download path — which is the actual code under test:

```java
private static void uploadTestFileViaFtp() throws Exception {
    var result = ftpContainer.execInContainer("sh", "-c",
        "printf '%s' 'Hello from FTP Integration Test!'" +
        " > /ftp/ftpuser/test-document.txt" +
        " && chown ftpuser:ftpuser /ftp/ftpuser/test-document.txt");
    // chown required: execInContainer runs as root;
    // Pure-FTPd only serves files owned by the authenticated user
    ...
}
```

The SFTP fixture is seeded via a real sshj `SFTPClient`, which validates the upload path independently.

#### FTP Path Format

`delfer/alpine-ftp-server` runs Pure-FTPd without per-user chroot. After login the server CWD is the user's home directory (`/ftp/<username>/`). Absolute paths (starting with `/`) resolve against the real filesystem root. The tests therefore use a bare filename — `test-document.txt` — which resolves relative to the CWD.

On a server with per-user chroot enabled the correct path would be `/test-document.txt` (absolute within the chroot). Always verify the path format against the specific server image you are using.

#### Why Mockito in a Container Test?

The integration test mocks only `ActionContext` — specifically its `file()` method. Everything else (FTP/SFTP connection, authentication, data transfer) is real.

`ActionContext.file()` is the boundary between the FTP action and ByteChef's file-storage backend. In production, that backend writes bytes to a database or object store. In a test environment, spinning up a real storage backend would require a full Spring application context. The mock replaces just that boundary and captures the downloaded bytes for assertion:

```java
when(mockFile.storeContent(anyString(), any(InputStream.class))).thenAnswer(invocation -> {
    capturedFilename.set(invocation.getArgument(0));
    capturedContent.set(invocation.<InputStream>getArgument(1).readAllBytes());
    return mockFileEntry;
});
```

`readAllBytes()` blocks until the FTP writer thread closes the pipe — so no extra synchronization is needed and the captured bytes are always complete.

### Adding a New Test Scenario

1. Add a `@Test` method.
2. Build connection parameters with `MockParametersFactory.create(Map.of(...))` using constants from `FtpConstants`.
3. Seed the test file in `@BeforeAll` via `ftpContainer.execInContainer(...)` or `sftpContainer.execInContainer(...)`.
4. Call `buildCapturingContext(capturedFilename, capturedContent, mockFileEntry)` to obtain a pre-wired `ActionContext`.
5. Call the action's `perform()` and assert on `capturedFilename`, `capturedContent`, and the returned `FileEntry`.

**Example: download a file from a subdirectory via FTP**

```java
// in @BeforeAll — seed the file
ftpContainer.execInContainer("sh", "-c",
    "mkdir -p /ftp/ftpuser/reports" +
    " && chown ftpuser:ftpuser /ftp/ftpuser/reports" +
    " && printf '%s' 'report data' > /ftp/ftpuser/reports/q1.txt" +
    " && chown ftpuser:ftpuser /ftp/ftpuser/reports/q1.txt");

// in @Test
Parameters inputParameters = MockParametersFactory.create(Map.of(PATH, "reports/q1.txt"));
```

### Configuring the Test Containers

| What to change | Where |
|---|---|
| FTP username / password | `FTP_USERNAME`, `FTP_PASSWORD` constants + `USERS` env on `ftpContainer` |
| Passive data port | `PASSIVE_DATA_PORT` constant + `MIN_PORT`, `MAX_PORT` env + `withFixedExposedPort` |
| SFTP subdirectory created at startup | `SFTP_UPLOAD_DIR` constant + `--command` argument on `sftpContainer` |
| Test file content | `TEST_CONTENT` constant + re-seed in `@BeforeAll` |
