const $ = (x) => document.getElementById(x);
const bridge = new MqttBridge();

// 【v3 修改】Firmware 用 @suffix 表示「目前 Topic Base」。
// 例如 @weight => esp32/weight
// UID=home01 時 => esp32/home01/weight
function publish(topic, payload, retain) {
    if (String(topic).startsWith("@")) {
        bridge.publishSuffix(String(topic).substring(1), payload, retain);
    } else {
        // 保留原 Arduino v4 舊相容 Topic，例如 esp/msg/seialraw。
        bridge.publish(topic, payload, retain);
    }
}

function render(s) {
    $("scaleDisplay").textContent = s.weight.toFixed(2);
    $("raw").textContent = s.raw;
    $("weight").textContent = s.weight.toFixed(2) + " g";
    $("base").textContent = s.base.toFixed(2) + " g";
    $("state").textContent = s.state;
    $("fsm").textContent = s.fsm;
    $("drinkMode").textContent = s.manual ? "APP MANUAL" : "LEGACY AUTO";
    $("lock").textContent = s.lock ? "LOCKED" : "UNLOCKED";
    $("loopCount").textContent = s.loopCount;
    $("uptime").textContent = Math.floor((Date.now() - s.bootAt) / 1000) + " s";
    $("tareOffset").textContent = s.tareOffset.toFixed(2) + " g";
    $("waterText").textContent = s.water.toFixed(0);
    $("waterAmount").value = s.water;
    $("waterVisual").style.height = (s.water / 700 * 100) + "%";

    $("hx").textContent = s.hx ? "READY" : "ERROR";
    $("wifi").textContent = s.wifi ? "CONNECTED" : "DISCONNECTED";
    $("mqtt").textContent = bridge.real ? "REAL / " + bridge.getBroker().label : "CONNECTING / SIM";
    $("topicBase").textContent = bridge.topicBase;

    let led = $("rgbLed");
    let txt = "OFF";

    if (!s.hx) {
        led.style.background = "white";
        led.style.boxShadow = "0 0 18px white";
        txt = "WHITE / HX711 ERROR";
    } else if (!s.wifi) {
        led.style.background = "red";
        led.style.boxShadow = "0 0 18px red";
        txt = "RED / Wi-Fi ERROR";
    } else if (!bridge.real) {
        led.style.background = "orange";
        led.style.boxShadow = "0 0 18px orange";
        txt = "YELLOW / MQTT ERROR";
    } else if (Date.now() < s.greenUntil) {
        led.style.background = "lime";
        led.style.boxShadow = "0 0 18px lime";
        txt = "GREEN";
    } else {
        led.style.background = "#111";
        led.style.boxShadow = "none";
        txt = "OFF (normal)";
    }

    $("rgbText").textContent = txt;
    $("runBadge").textContent = s.running ? "RUNNING" : "PAUSED";
    $("runBadge").className = "badge " + (s.running ? "text-bg-success" : "text-bg-warning");
}


// v3.3：可拖曳水杯。只改 Simulator UI，仍使用 fw.onScale 餵給既有 Firmware。
function initCupDrag() {
    const cup = $("cupWrap");
    const stage = $("cupStage");
    const scaleZone = $("scaleZone");
    const status = $("dragStatus");
    if (!cup || !stage || !scaleZone) return;

    let dragging = false;
    let pid = null;
    let dx = 0, dy = 0;
    let home = null;

    const inside = (r, x, y, pad=0) =>
        x >= r.left-pad && x <= r.right+pad &&
        y >= r.top-pad && y <= r.bottom+pad;

    function setStatus(t) {
        if (status) status.textContent = t;
    }

    function rememberHome() {
        const sr = stage.getBoundingClientRect();
        const cr = cup.getBoundingClientRect();
        home = { left: cr.left-sr.left, top: cr.top-sr.top };
        snapHome(false);
    }

    function snapHome(updatePhysics=true) {
        if (!home) return;
        cup.style.left = home.left+"px";
        cup.style.top = home.top+"px";
        cup.style.right = "auto";
        cup.style.bottom = "auto";
        cup.style.transform = "none";
        cup.classList.remove("off-scale");

        if (updatePhysics) {
            fw.onScale = true;
            Log.event("3D DRAG","水杯放回秤盤");
        }
        setStatus("目前：水杯在秤上");
    }

    function setPickedUp() {
        if (fw.onScale) {
            fw.onScale = false;
            Log.event("3D DRAG","水杯離開秤盤 → 已拿起");
        }
        cup.classList.add("off-scale");
        setStatus("目前：水杯已拿起（秤外任何位置皆算拿起）");
    }

    function cupIsOnScale() {
        const cr = cup.getBoundingClientRect();
        const zr = scaleZone.getBoundingClientRect();

        // 使用杯子底部中央點判定是否仍落在實體秤盤有效區。
        const x = cr.left + cr.width/2;
        const y = cr.bottom - 8;

        // 稍微縮小有效區，避免杯子只擦到邊緣仍被誤認在秤上。
        return x >= zr.left+28 && x <= zr.right-28 &&
               y >= zr.top+45 && y <= zr.bottom-8;
    }

    requestAnimationFrame(rememberHome);

    cup.addEventListener("pointerdown", e => {
        if (e.button !== undefined && e.button !== 0) return;
        if (!home) rememberHome();

        const cr = cup.getBoundingClientRect();
        dragging = true;
        pid = e.pointerId;
        dx = e.clientX-cr.left;
        dy = e.clientY-cr.top;

        cup.setPointerCapture?.(pid);
        cup.classList.add("dragging");
        setStatus("拖曳中：只要離開秤盤就算拿起");
        e.preventDefault();
    });

    cup.addEventListener("pointermove", e => {
        if (!dragging || e.pointerId !== pid) return;

        const sr = stage.getBoundingClientRect();
        let left = e.clientX-sr.left-dx;
        let top  = e.clientY-sr.top-dy;

        left = Math.max(0, Math.min(sr.width-cup.offsetWidth, left));
        top  = Math.max(42, Math.min(sr.height-cup.offsetHeight, top));

        cup.style.left = left+"px";
        cup.style.top = top+"px";

        const onScale = cupIsOnScale();
        scaleZone.classList.toggle("hot", onScale);

        // v3.4 核心：拖曳途中只要杯子離開秤盤，立即切成拿起。
        if (!onScale) setPickedUp();

        render(fw);
    });

    function finish(e) {
        if (!dragging || (e.pointerId !== undefined && e.pointerId !== pid)) return;

        dragging = false;
        cup.classList.remove("dragging");
        scaleZone.classList.remove("hot");

        // 放開時只有真正落在秤盤有效區才算放回。
        if (cupIsOnScale()) {
            snapHome(true);
        } else {
            setPickedUp();
        }

        render(fw);
        try { cup.releasePointerCapture?.(pid); } catch (_) {}
        pid = null;
    }

    cup.addEventListener("pointerup", finish);
    cup.addEventListener("pointercancel", finish);

    cup.addEventListener("keydown", e => {
        if (e.key !== "Enter" && e.key !== " ") return;
        e.preventDefault();

        if (fw.onScale) {
            setPickedUp();
            cup.style.left = Math.max(12, stage.clientWidth-cup.offsetWidth-35)+"px";
            cup.style.top = "75px";
        } else {
            snapHome(true);
        }
        render(fw);
    });

    window.cupDragUI = {
        pickup: () => {
            setPickedUp();
            cup.style.left = Math.max(12, stage.clientWidth-cup.offsetWidth-35)+"px";
            cup.style.top = "75px";
            render(fw);
        },
        putback: () => {
            snapHome(true);
            render(fw);
        },
        resetHome: () => {
            // REBOOT 時杯子視覺與 Virtual Hardware 一起回到開機位置。
            snapHome(false);
            setStatus("重新啟動：水杯在秤上，正在開機 TARE…");
        }
    };
}

// 建立 Virtual Firmware。
window.fw = new FirmwareV4(render, publish);
fw.boot();
initCupDrag();

// 3D 杯子操作。
$("pickup").onclick = () => {
    if (window.cupDragUI) window.cupDragUI.pickup();
    else fw.onScale = false;
    Log.event("3D", "杯子從秤面拿起");
};

$("returnCup").onclick = () => {
    if (window.cupDragUI) window.cupDragUI.putback();
    else fw.onScale = true;
    Log.event("3D", "杯子放回秤面");
};

$("drink").onclick = () => {
    if (fw.onScale) {
        Log.event("WARN", "請先拿起杯子");
        return;
    }

    const n = +$("drinkAmount").value;
    const actual = Math.min(n, fw.water);
    fw.water -= actual;
    Log.event("3D", `喝水 ${actual} cc，剩餘 ${fw.water} cc`);
};

$("addWater").onclick = () => {
    fw.water = Math.min(700, fw.water + 50);
    Log.event("3D", "加水 50 cc");
};

$("drinkAmount").oninput = (e) => $("drinkText").textContent = e.target.value;
$("waterAmount").oninput = (e) => { fw.water = +e.target.value; };

$("run").onclick = () => fw.start();
$("pause").onclick = () => fw.pause();
$("reboot").onclick = () => {
    if (window.cupDragUI) window.cupDragUI.resetHome();
    fw.onScale = true;
    fw.reboot();
};

$("wifiFault").onclick = () => {
    fw.wifi = !fw.wifi;
    fw.greenUntil = Date.now() + 1000;
    Log.event("FAULT", "Wi-Fi " + (fw.wifi ? "RECOVERED" : "DISCONNECTED"));
};

$("mqttFault").onclick = () => {
    if (bridge.real) {
        bridge.disconnect();
    } else {
        bridge.connect(() => render(fw));
    }
};

$("hxFault").onclick = () => {
    fw.hx = !fw.hx;
    fw.greenUntil = Date.now() + 1000;
    Log.event("FAULT", "HX711 " + (fw.hx ? "RECOVERED" : "DISCONNECTED"));
};

// 模擬器自己的 Command Console。
// 注意：這裡直接執行 Firmware，不必經過 Broker 繞一圈。
$("sendCmd").onclick = () => {
    const c = $("command").value;
    Log.mqtt("RX LOCAL", `${bridge.cmdTopic()} ${c}`);
    fw.command(c);
};

// Broker 選擇：與 Dashboard 相同。
$("brokerSelect").value = bridge.brokerKey;
$("brokerSelect").onchange = (e) => {
    bridge.setBroker(e.target.value);
    bridge.connect(() => render(fw));
};

// UID：可手動與 Dashboard 保持相同。
// Dashboard 若透過 setuid 指令設定，v3 也會自動切換。
$("deviceUid").value = bridge.deviceUid;
$("applyUid").onclick = () => {
    try {
        bridge.setUid($("deviceUid").value);
        $("deviceUid").value = bridge.deviceUid;
        render(fw);
    } catch (e) {
        alert(e.message);
    }
};

$("mqttConnect").onclick = () => bridge.connect(() => render(fw));
$("mqttDisconnect").onclick = () => bridge.disconnect();

$("downloadLog").onclick = () => Log.download();

document.querySelectorAll(".clear").forEach((b) => {
    b.onclick = () => $(b.dataset.id).textContent = "";
});

[
    "tare", "getweight", "manualdrink", "legacyauto", "clear",
    "forcecup", "forcebox", "auto", "streamon", "streamoff"
].forEach((c) => {
    const b = document.createElement("button");
    b.className = "btn btn-sm btn-outline-secondary";
    b.textContent = c;
    b.onclick = () => {
        $("command").value = c;
        $("sendCmd").click();
    };
    $("quickCommands").appendChild(b);
});

// 【v3 關鍵】頁面載入後直接連到 Dashboard 已驗證可用的 MQTTGO。
// 不再要求開發者自己猜 WebSocket URL。
setTimeout(() => {
    bridge.connect(() => render(fw));
}, 300);
