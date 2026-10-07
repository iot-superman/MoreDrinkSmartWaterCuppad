# MoreDrinkSmartWaterCuppad

## Temporary Android compatibility with `dev` firmware

The device-binding flow remains Page3 → Page1 → Page6. Page3 waits for BLE
service discovery and TX notification subscription before opening Wi-Fi setup.
Page1 sends UTF-8 `<ssid>:<password>` (no `WIFISET`, attempt ID, or newline).
Select **開放網路（無密碼）** for an open network; its payload is `<ssid>:`.
This is temporary until the firmware supports attempt-correlated provisioning.

The current `Arduino/BLE_E_Weightm7_fixappbug` firmware notifies:

- `💾 [NVS 儲存成功] 新 WiFi 寫入！SSID: <ssid>，準備重新嘗試連線...`
- `🎉 [WiFi 連線成功] IP: <ip>`

Android requires the NVS receipt followed by the Wi-Fi success message.
A complete receipt must match the submitted SSID. At the default MTU 23, the
firmware may truncate logs to 20 bytes: Android matches their known byte prefixes,
without pretending that the missing SSID/IP was received. Page6 shows “未提供”
when the IP is truncated.
A BLE write acknowledgement or the NVS receipt alone is **not** success. Weight,
MQTT, and `S:<attemptId>` / `E:<attemptId>:<reason>` messages are ignored.
Wi-Fi failures are only printed to Serial by this firmware, so Android reports
an unconfirmed result after 45 seconds, not a specific authentication error.
Timeout, cancellation, or write failure closes BLE; return to Page3 to reconnect
before retrying. Legacy replies cannot reliably correlate attempts, so hardware
verification is still required.

Firmware limitations: SSID cannot contain `:`, leading/trailing whitespace is
trimmed, and the complete UTF-8 payload must fit its 64-byte receive buffer.
Android rejects unsupported input and requests MTU 185. If firmware stays at
MTU 23, credentials are split into sequential acknowledged writes; no newline
is added. If a write takes 400 ms or more, Android aborts before sending another
chunk, because firmware processes accumulated credentials after 500 ms of silence.
Such an abort may leave partial credentials on the device; reconnect and resend.

Hardware acceptance checks:

- Protected and open 2.4 GHz networks: confirm Serial stores the entered SSID,
  never `WIFISET`; Page6 appears only after the actual Wi-Fi success notification.
- Wrong password/unavailable SSID: no success page; timeout offers reconnection.
- Cancel, retry, or switch devices: no old success state or late reply completes
  the new flow. Unrelated weight/MQTT notifications must not complete provisioning.
- Page6 shows the submitted SSID and reported IP; MQTT status remains unconfirmed.
