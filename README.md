# ILO2RemoteConsole
ILO2 Java Remote console as Standalone version

Tested with Java 8 and Java 11. Hangs for an unknown reason on Java 15.

## What is this?

This tool connects to an HP iLO 2 management controller and opens the remote console, a full hardware KVM over IP. You see whatever is physically on the server's display regardless of OS, including BIOS/UEFI, bootloaders, and graphical desktops.

## Requirements

- Java 8 or Java 11 (not 15+)
- Gradle (or use the included `gradlew`/`gradlew.bat` wrapper, no separate install needed)

## Building

```
.\gradlew.bat jar        # Windows (PowerShell)
./gradlew jar            # Linux / macOS
```

The JAR is output to `build/libs/ILO2RemCon.jar`.

## Usage

Because even on the latest firmware (2.33 as of 2021-04-16),
due to hardware limitations, iLO2 does not support modern TLS (and ciphers).
Therefore, adjusting the JRE's security settings is necessary. Seemingly, this can not be done at runtime,
so a custom security file has to be passed to Java. That is what the `-Djava.security.properties=java.security` part does.

If this still fails with a TLS related error, the certificate in use by your iLO might still rely on pre-2.33 ciphers.
In that case, regenerate or replace it through the iLO web interface.

**Pass host, username, and password as arguments:**

```
java -Djava.security.properties=java.security -jar build/libs/ILO2RemCon.jar <Hostname> <Username> <Password>
```

**Or use a config file** (see `config_template.properties` for the format):

```
java -Djava.security.properties=java.security -jar build/libs/ILO2RemCon.jar -c <Path to config.properties>
```

Running without arguments will try `config.properties` in the current working directory.

### Windows (PowerShell)

`set` is not a valid PowerShell command. Use `$env:` syntax instead:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-11.0.31.11-hotspot"
.\gradlew.bat jar
Remove-Item -Force data.cook -ErrorAction SilentlyContinue
& "$env:JAVA_HOME\bin\java.exe" "-Djava.security.properties=java.security" -jar build\libs\ILO2RemCon.jar 192.168.1.x Administrator yourpassword
```

A `data.cook` file is created to cache the session cookie between runs. Delete it if you change credentials or encounter auth errors.

## Fixes in this fork

The original code had two bugs that caused a crash on startup:

**1. Stage2 was sending a POST instead of a GET**

`setDoOutput(true)` on the `HttpURLConnection` silently switches it to POST mode. The iLO login endpoint is a GET, so sending a POST meant iLO returned 200 with no session cookie and every subsequent authenticated request failed.

**2. The session cookie was never stored for use in Stage3**

Even when the cookie manager captured a cookie, it was never assigned to `supercookie`, so Stage3 always made an unauthenticated request and received a login page instead of the remote console frame data.

**3. Stage3 tried to parse `<PARAM>` tags that don't exist in the raw HTML**

The applet parameters are written by `document.writeln()` in JavaScript, so they only exist in the browser's rendered DOM and not in the raw HTTP response. The fix reads all values directly from the JavaScript variable assignments (`info0="..."`, `info7=30`, etc.) and extracts the JAR name from the `ARCHIVE=` attribute instead.

**4. Cached session cookie failed to reload on subsequent runs**

`data.cook` stores cookies in `name=value` format, but the session cookie value contains `:::` with extra colons and the reload used `split("=")[1]` which truncated anything after the first `=`. The fix uses `indexOf('=')` to split only on the first equals sign.

**5. Selecting the outline mouse cursor crashed the applet**

`createCursor` allocated `int[21*12]` (252 elements) for the dot and outline cursor image buffers, but then indexed into them using `col + row * 32` (row stride for a 32x32 image). At row 8 the index exceeds 252 and throws `ArrayIndexOutOfBoundsException`. The buffer is now correctly sized to `int[32*32]`.
