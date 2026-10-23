window.MqttBridge = class {
    constructor() {
        this.client = null;
        this.real = false;

        // 【v3 新增】完全依照已驗證 Dashboard 的 Broker URL。
        this.brokers = {
            mqttgo: {
                label: "MQTTGO",
                url: "wss://mqttgo.io:8084/mqtt"
            },
            emqx: {
                label: "EMQX",
                url: "wss://broker.emqx.io:8084/mqtt"
            }
        };

        this.brokerKey = localStorage.getItem("esp32SimulatorV3Broker") || "mqttgo";
        if (!this.brokers[this.brokerKey]) this.brokerKey = "mqttgo";

        this.deviceUid = localStorage.getItem("esp32SimulatorV3Uid") || "";
        if (!/^[A-Za-z0-9_-]{1,32}$/.test(this.deviceUid)) this.deviceUid = "";

        this.onState = null;
    }

    get topicBase() {
        return this.deviceUid ? `esp32/${this.deviceUid}` : "esp32";
    }

    topic(suffix) {
        return `${this.topicBase}/${suffix}`;
    }

    cmdTopic() {
        return this.topic("cmd");
    }

    getBroker() {
        return this.brokers[this.brokerKey] || this.brokers.mqttgo;
    }

    setBroker(key) {
        if (!this.brokers[key]) return;
        this.brokerKey = key;
        localStorage.setItem("esp32SimulatorV3Broker", key);
    }

    setUid(uid) {
        uid = String(uid || "").trim();

        if (uid !== "" && !/^[A-Za-z0-9_-]{1,32}$/.test(uid)) {
            throw new Error("UID 只允許 1～32 碼英數字、-、_");
        }

        const oldCmd = this.cmdTopic();

        // 【v3 新增】切換 UID 前先取消舊 cmd 訂閱。
        if (this.client && this.client.connected) {
            try { this.client.unsubscribe(oldCmd); } catch (_) {}
        }

        this.deviceUid = uid;

        if (uid) {
            localStorage.setItem("esp32SimulatorV3Uid", uid);
        } else {
            localStorage.removeItem("esp32SimulatorV3Uid");
        }

        if (this.client && this.client.connected) {
            this.subscribeCommand();
        }

        Log.event("TOPIC", `Topic Base 切換為 ${this.topicBase}`);
        if (this.onState) this.onState();
    }

    subscribeCommand() {
        if (!this.client || !this.client.connected) return;

        const cmd = this.cmdTopic();

        // Dashboard 的控制命令會送到這裡。
        this.client.subscribe(cmd, { qos: 1 }, (err) => {
            if (err) {
                Log.mqtt("ERROR", `SUB ${cmd}：${err.message || err}`);
            } else {
                Log.mqtt("SUB", `${cmd} QoS=1`);
            }
        });
    }

    connect(onState) {
        if (onState) this.onState = onState;

        if (!window.mqtt) {
            Log.mqtt("ERROR", "MQTT.js CDN 尚未載入");
            return;
        }

        // 防止重複建立 Client。
        if (this.client) {
            try { this.client.end(true); } catch (_) {}
            this.client = null;
        }

        const broker = this.getBroker();
        const clientId = "ESP32S3_SIM_V3_" +
            Math.random().toString(16).slice(2, 10) + "_" + Date.now();

        Log.mqtt("CONNECT", `${broker.label} ${broker.url}`);
        Log.event("MQTT", `正在連線 ${broker.label}，ClientID=${clientId}`);

        this.client = mqtt.connect(broker.url, {
            clientId,
            clean: true,
            reconnectPeriod: 1000,
            connectTimeout: 10000,
            keepalive: 30
        });

        this.client.on("connect", () => {
            this.real = true;
            Log.mqtt("CONNECTED", `${broker.label} ${broker.url}`);
            Log.event("MQTT", `✅ 已連線 ${broker.label}；Topic Base=${this.topicBase}`);
            this.subscribeCommand();

            // 【v3 新增】模擬真 ESP32 連線成功後綠燈亮 1 秒。
            if (window.fw) window.fw.greenUntil = Date.now() + 1000;

            if (this.onState) this.onState();
        });

        this.client.on("reconnect", () => {
            this.real = false;
            Log.mqtt("RECONNECT", broker.url);
            if (this.onState) this.onState();
        });

        this.client.on("offline", () => {
            this.real = false;
            Log.mqtt("OFFLINE", broker.url);
            if (this.onState) this.onState();
        });

        this.client.on("close", () => {
            this.real = false;
            Log.mqtt("CLOSE", broker.url);
            if (this.onState) this.onState();
        });

        this.client.on("error", (err) => {
            this.real = false;
            Log.mqtt("ERROR", err.message || String(err));
            if (this.onState) this.onState();
        });

        this.client.on("message", (topic, payload) => {
            const message = payload.toString();
            Log.mqtt("RX CMD", `${topic} ${message}`);

            if (!window.fw) return;

            // 【v3 新增】Dashboard 設定 UID：
            // Dashboard 先送 setuid:xxx 到目前 cmd，再立即切換訂閱。
            if (topic === this.cmdTopic()) {
                const lower = message.toLowerCase();

                if (lower.startsWith("setuid:")) {
                    const newUid = message.substring(message.indexOf(":") + 1).trim();

                    // 先交給 Firmware 產生 Serial / ACK，再切 Topic Base。
                    window.fw.command(message);

                    try {
                        this.setUid(newUid);
                    } catch (e) {
                        Log.mqtt("ERROR", e.message);
                    }
                    return;
                }

                if (lower === "clearuid") {
                    window.fw.command(message);
                    this.setUid("");
                    return;
                }

                window.fw.command(message);
            }
        });
    }

    disconnect() {
        if (this.client) {
            try { this.client.end(true); } catch (_) {}
        }
        this.client = null;
        this.real = false;
        Log.event("MQTT", "已手動中斷 REAL MQTT");
        if (this.onState) this.onState();
    }

    publishSuffix(suffix, payload, retain = true) {
        this.publish(this.topic(suffix), payload, retain);
    }

    publish(topic, payload, retain = true) {
        const line = `${topic} ${payload}${retain ? " [retain]" : ""}`;

        if (this.real && this.client && this.client.connected) {
            // Arduino PubSubClient publish 為 QoS 0；保留 retain=true。
            this.client.publish(topic, String(payload), {
                qos: 0,
                retain: !!retain
            }, (err) => {
                if (err) Log.mqtt("ERROR", `PUB ${topic}：${err.message || err}`);
            });

            Log.mqtt("TX REAL", line);
        } else {
            // 未連線時仍保留 Virtual Console，方便離線除錯。
            Log.mqtt("TX SIM", line);
        }
    }
};
