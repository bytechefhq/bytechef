# FTP Component - SFTP Support Implementation

## Overview

Added SFTP (SSH File Transfer Protocol) support to the FTP component as a boolean connection option. Users can now choose between FTP and SFTP protocols when configuring their connection.

## Changes Made

### 1. Added sshj Dependency

**File:** `build.gradle.kts`

```kotlin
dependencies {
    implementation("commons-net:commons-net:3.11.1")
    implementation("com.hierynomus:sshj:0.40.0")  // NEW
}
```

### 2. Added SFTP Constant

**File:** `src/main/java/com/bytechef/component/ftp/constant/FtpConstants.java`

```java
public static final String SFTP = "sftp";
```

### 3. Added SFTP Connection Property

**File:** `src/main/java/com/bytechef/component/ftp/connection/FtpConnection.java`

Added new boolean property to connection definition:
```java
bool(SFTP)
    .label("Use SFTP")
    .description("Use SFTP (SSH File Transfer Protocol) instead of FTP. SFTP provides encrypted file transfer over SSH. When enabled, the port defaults to 22 instead of 21.")
    .defaultValue(false)
```

### 4. Created RemoteFileClient Interface

**File:** `src/main/java/com/bytechef/component/ftp/util/RemoteFileClient.java`

Created an abstraction interface for remote file operations supporting both FTP and SFTP:

```java
public interface RemoteFileClient extends Closeable {

    int DEFAULT_FTP_PORT = 21;
    int DEFAULT_SFTP_PORT = 22;

    // Factory method to create appropriate client
    static RemoteFileClient of(Parameters connectionParameters);

    // File operations
    void storeFile(String remotePath, InputStream inputStream) throws IOException;
    void retrieveFile(String remotePath, OutputStream outputStream) throws IOException;
    List<RemoteFileInfo> listFiles(String path) throws IOException;
    void deleteFile(String path) throws IOException;
    void deleteDirectory(String path) throws IOException;
    void rename(String oldPath, String newPath) throws IOException;
    void createDirectoryTree(String path) throws IOException;
    boolean isDirectory(String path) throws IOException;

    record RemoteFileInfo(String name, String path, boolean directory, long size, Instant modifiedAt) {}
}
```

The interface includes:
- Static factory method `of()` that creates either FTP or SFTP client based on connection parameters
- Private static methods `createFtpClient()` and `createSftpClient()` for client instantiation
- Common file operation methods

### 5. Created FtpRemoteFileClient Implementation

**File:** `src/main/java/com/bytechef/component/ftp/util/FtpRemoteFileClient.java`

FTP implementation using Apache Commons Net `FTPClient`:
- Wraps `FTPClient` from commons-net
- Implements all `RemoteFileClient` methods
- Handles FTP-specific operations like passive/active mode

### 6. Created SftpRemoteFileClient Implementation

**File:** `src/main/java/com/bytechef/component/ftp/util/SftpRemoteFileClient.java`

SFTP implementation using sshj library:
- Uses `SSHClient` and `SFTPClient` from sshj
- Implements all `RemoteFileClient` methods
- Handles SSH connection and authentication
- Includes custom `InMemorySourceFile` and `InMemoryDestFile` implementations for stream-based transfers

### 7. Deleted FtpUtils.java

The utility class was removed as its functionality was moved into `RemoteFileClient.of()`.

### 8. Updated All Action Classes

Updated all action classes to use the new `RemoteFileClient` abstraction:

**Files updated:**
- `FtpUploadFileAction.java`
- `FtpDownloadFileAction.java`
- `FtpListAction.java`
- `FtpDeleteAction.java`
- `FtpRenameAction.java`

**Change pattern:**
```java
// Before
FTPClient ftpClient = FtpUtils.getFtpClient(connectionParameters);
try {
    // operations using ftpClient
} finally {
    FtpUtils.closeFtpClient(ftpClient);
}

// After
try (RemoteFileClient remoteFileClient = RemoteFileClient.of(connectionParameters)) {
    // operations using remoteFileClient
}
```

Also updated action descriptions from "FTP server" to "FTP/SFTP server".

## Protocol Comparison

| Feature | FTP | SFTP |
|---------|-----|------|
| Library | Apache Commons Net | sshj |
| Default Port | 21 | 22 |
| Encryption | None (or FTPS for TLS) | SSH-based |
| Passive Mode | Yes | N/A |

## Usage

### FTP Connection (default)
```json
{
  "host": "ftp.example.com",
  "port": 21,
  "username": "user",
  "password": "pass",
  "passiveMode": true,
  "sftp": false
}
```

### SFTP Connection
```json
{
  "host": "sftp.example.com",
  "port": 22,
  "username": "user",
  "password": "pass",
  "sftp": true
}
```

## Testing

- Component definition test regenerated with new SFTP property
- All tests pass with `./gradlew :server:libs:modules:components:ftp:test`
- Spotless formatting applied

## File Structure After Changes

```
server/libs/modules/components/ftp/
├── build.gradle.kts                    # Added sshj dependency
└── src/
    ├── main/java/com/bytechef/component/ftp/
    │   ├── FtpComponentHandler.java
    │   ├── action/
    │   │   ├── FtpDeleteAction.java        # Updated
    │   │   ├── FtpDownloadFileAction.java  # Updated
    │   │   ├── FtpListAction.java          # Updated
    │   │   ├── FtpRenameAction.java        # Updated
    │   │   └── FtpUploadFileAction.java    # Updated
    │   ├── connection/
    │   │   └── FtpConnection.java          # Added SFTP property
    │   ├── constant/
    │   │   └── FtpConstants.java           # Added SFTP constant
    │   └── util/
    │       ├── FtpRemoteFileClient.java    # NEW
    │       ├── RemoteFileClient.java       # NEW
    │       └── SftpRemoteFileClient.java   # NEW
    └── test/resources/definition/
        └── ftp_v1.json                     # Regenerated
```
