# ILO2RemoteConsole
ILO2 Java Remote console as standalone version

Tested with Java 17, Java 21 and Java 25. The prebuilt exe includes a bundled JRE and does not require Java to be installed separately.

> **Windows security warning:** Windows may flag the downloaded exe as unrecognised because it is not code-signed. This is expected for unsigned open source software. If you see a SmartScreen prompt, click "More info" then "Run anyway" to proceed. If you are not comfortable doing that, you can build the exe yourself from the source code using the instructions in the Building section below. The full source is available in this repository.

## What is this?

This tool connects to an HP iLO 2 management controller and opens the remote console, a full hardware KVM over IP. You see whatever is physically on the server's display regardless of OS, including BIOS/UEFI, bootloaders, and graphical desktops.

## Requirements

- Java 17, Java 21 or Java 25 (building from source only; the prebuilt exe includes a bundled JRE)
- Gradle (or use the included `gradlew`/`gradlew.bat` wrapper, no separate install needed)

## Prebuilt exe

Download the latest release from the [Releases](https://github.com/jaiden-04/ILO2-Standalone-Remote-Console/releases) page. Unzip and run `ILO2RemCon.exe`. No Java installation required.

## Building

```
.\gradlew.bat jar        # Windows (PowerShell)
./gradlew jar            # Linux / macOS
```

The JAR is output to `build/libs/ILO2RemCon.jar`.

## Usage

Because even on the latest firmware (2.33 as of 2021-04-16), due to hardware limitations, iLO2 does not support modern TLS. The included `java.security` file relaxes the necessary restrictions. When running from the JAR directly, pass it with `-Djava.security.properties=java.security`. The prebuilt exe handles this automatically.

If you still get a TLS error, the certificate on your iLO may need to be regenerated through the iLO web interface.

**No arguments (login UI):**

Run the exe or JAR with no arguments to get a login dialog. Enter the host, username, and password and press Connect.

**Pass host, username, and password as arguments:**

```
java -Djava.security.properties=java.security -jar build/libs/ILO2RemCon.jar <Hostname> <Username> <Password>
```

**Use a config file** (see `config_template.properties` for the format):

```
java -Djava.security.properties=java.security -jar build/libs/ILO2RemCon.jar -c <Path to config.properties>
```

Running without arguments will try `config.properties` in the current working directory before showing the login UI.

### Windows (PowerShell)

`set` is not a valid PowerShell command. Use `$env:` syntax instead:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-11.0.31.11-hotspot"
.\gradlew.bat jar
& "$env:JAVA_HOME\bin\java.exe" "-Djava.security.properties=java.security" -jar build\libs\ILO2RemCon.jar
```

A `data.cook` file is created to cache the session cookie between runs. Delete it if you change credentials or encounter auth errors.

## Fixes in this fork

These bugs existed in the upstream code and are fixed in this fork:

**Stage2 was sending a POST instead of a GET**

`setDoOutput(true)` on the `HttpURLConnection` silently switches it to POST mode. The iLO login endpoint is a GET, so sending a POST meant iLO returned 200 with no session cookie and every subsequent request failed unauthenticated.

**The session cookie was never passed to Stage3**

Even when a cookie was obtained, it was never assigned to `supercookie`, so Stage3 always made an unauthenticated request and received a login page instead of the remote console frame data.

**Stage3 tried to parse `<PARAM>` tags that don't exist in the raw HTML**

The applet parameters are written by `document.writeln()` in JavaScript, so they only exist in the browser's rendered DOM. The fix reads values directly from the JavaScript variable assignments and extracts the JAR name from the `ARCHIVE=` attribute instead.

**Cached session cookie failed to reload on subsequent runs**

`data.cook` stores cookies in `name=value` format, but the session cookie value contains `:::` and the reload used `split("=")[1]` which truncated anything after the first `=`. The fix splits only on the first equals sign.

**Selecting the outline mouse cursor crashed the applet**

`createCursor` allocated `int[21*12]` (252 elements) for the cursor image buffer but indexed into it using `col + row * 32` (stride for a 32x32 image). At row 8 the index exceeds 252 and throws `ArrayIndexOutOfBoundsException`. The buffer is now correctly sized to `int[32*32]`.

## Changelog

### v1.2
- Update checker in login UI: shows a notice if a newer release is available on GitHub
- Fields and connect button re-enable after a failed connection attempt so credentials can be corrected without restarting
- Friendlier error messages for connection timeout and unknown host

### v1.1
- Login UI: running with no arguments now shows a login dialog instead of exiting. Credentials can still be passed as arguments or via config file to skip it
- Fixed console window opening alongside the app on Windows
- Fixed TLS handshake error in the prebuilt exe caused by the bundled JRE having TLSv1 disabled

### v1.0
- Initial release with all upstream bug fixes applied
