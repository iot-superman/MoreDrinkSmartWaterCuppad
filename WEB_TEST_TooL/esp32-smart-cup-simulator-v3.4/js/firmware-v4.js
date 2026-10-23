window.FirmwareV4=class{
constructor(render,publish){this.render=render;this.publish=publish;this.timer=null;this.reset()}
reset(){this.running=false;this.loopCount=0;this.bootAt=Date.now();this.hx=true;this.wifi=true;this.mqttVirtual=true;this.water=450;this.cup=180;this.onScale=true;this.raw=98406;this.tareOffset=0;this.weight=0;this.filtered=0;this.buf=[];this.lastLoop=0;this.base=0;this.state="EMPTY_IDLE";this.fsm="AUTO";this.lock=false;this.stable=0;this.pickup=false;this.before=0;this.lastLog=0;this.manual=false;this.greenUntil=Date.now()+1000}
physical(){return this.onScale?this.cup+this.water:0}
boot(){
Log.serial("BOOT","--- [ESP32S3 系統啟動] ---");
Log.serial("BLE","初始化完成，藍牙廣播名稱: ESP32S3_SCALE");
Log.serial("HX711","⚖️ 正在進行開機精準去皮歸零 (30次採樣)...");

// v3.4：完全模擬原 Arduino setup() 的 scale.tare(30)。
// 開機時秤面上即使有杯子＋水，也必須被當成 0g。
this.tareOffset=this.physical();
this.weight=0;
this.filtered=0;
this.lastLoop=0;
this.buf=Array(8).fill(0);
this.base=0;
this.state="EMPTY_IDLE";
this.lock=false;
this.stable=0;
this.pickup=false;
this.before=0;

Log.serial("HX711","✅ HX711 開機歸零完成！目前基準重量: 0.00g 狀態: EMPTY_IDLE");
Log.serial("WIFI","🎉 [WiFi 連線成功] IP: 192.168.1.108 (VIRTUAL)");
Log.serial("MQTT","MQTT 成功連線！");

// 立即讓 Dashboard 收到開機後的 0g，而不是先看到 630g。
this.publish("@weight","0.00",true);
this.start()
}
start(){if(this.timer)return;this.running=true;this.timer=setInterval(()=>this.loop(),150);Log.event("EXEC","Virtual ESP32 loop RUNNING @ 150ms")}
pause(){this.running=false;if(this.timer){clearInterval(this.timer);this.timer=null}Log.event("EXEC","Virtual ESP32 PAUSED")}
reboot(){this.pause();this.reset();Log.serial("RESET","rst:0x1 (POWERON_RESET)");setTimeout(()=>this.boot(),350)}
noise(){return (Math.random()-.5)*.08}
loop(){if(!this.running)return;this.loopCount++;if(!this.hx){if(this.loopCount%13===0)Log.serial("HW","⚠️ [硬體錯誤] 找不到 HX711 晶片。正在嘗試重連...");this.render(this);return}
let grossPhysical=this.physical();
let physical=(grossPhysical-this.tareOffset)+this.noise();

// RAW 是 load-cell / HX711 ADC 原始值：tare 只改 offset，不改感測器原始 ADC。
this.raw=Math.round(98406+grossPhysical*387.2+((Math.random()-.5)*8));

// get_units() / 顯示重量則是扣除 tareOffset 後的淨重。
if(Math.abs(physical-this.lastLoop)>15){
    this.buf=Array(8).fill(physical);
    Log.serial("FILTER","⚡【動態加速】偵測到重量變動，瞬間同步濾波緩衝區。");
}
this.buf.push(physical);
if(this.buf.length>8)this.buf.shift();
this.filtered=this.buf.reduce((a,b)=>a+b,0)/this.buf.length;
this.weight=Math.abs(this.filtered)<.1?0:this.filtered;
if(!this.manual&&!this.pickup&&this.state==="CUP_SETTLED"&&(this.base-this.weight)>10){this.before=this.base;this.state="CUP_PICKED_UP";this.pickup=true;Log.event("FSM",`🥛 水杯被拿起！鎖定飲水前重量: ${this.before.toFixed(2)}g`)}
if(!this.manual&&(this.state==="CUP_PICKED_UP")&&this.weight<=7){this.state="CUP_WAIT_RETURN";this.pickup=false}
if(Math.abs(this.weight-this.lastLoop)<.06)this.stable++;else this.stable=0;
if(!this.manual&&this.stable>=12){this.stable=0;this.settle()}else if(this.manual&&this.stable>=12){this.stable=0;this.state="EMPTY_IDLE";this.lock=false}
this.lastLoop=this.weight;
if(this.mqttVirtual&&this.wifi){this.publish("@raw",String(this.raw),true);this.publish("@weight",this.weight.toFixed(2),true)}
if(Date.now()-this.lastLog>1000){this.lastLog=Date.now();let line=`RAW = ${this.raw}    Weight = ${this.weight.toFixed(2)} g    [Base: ${this.base.toFixed(2)} g]   [State: ${this.state}] ${this.fsm} [${this.lock?"LOCKED":"UNLOCKED"}]`;Log.serial("RAW",line);if(this.mqttVirtual&&this.wifi)this.publish("@serialraw",line,true);this.publish("esp/msg/seialraw",line,true)}
this.render(this)}
settle(){let w=this.weight;
if(this.state==="EMPTY_IDLE"){if(Math.abs(w)<10){this.base=0}else if(w>=30){this.state="CUP_SETTLED";this.base=w;this.lock=true;Log.event("FSM",`🔄【單次識別】偵測到水杯！重量: ${w.toFixed(2)}g`)}}
else if(this.state==="CUP_SETTLED"){let d=this.base-w;if(d>=3){Log.event("DRINK",`【智慧紀錄】吸管模式飲水！單次減少: ${d.toFixed(2)} cc`);this.publish("@water/taken",d.toFixed(2),true);this.base=w}else if(w-this.base>8){this.base=w;Log.event("FSM",`🥛【基準校準】水杯就地加水，更新基準: ${w.toFixed(2)}g`)}else if(Math.abs(w-this.base)<1)this.base=w}
else if(this.state==="CUP_WAIT_RETURN"&&w>7){let d=this.before-w;if(Math.abs(d)>3&&d>=3){Log.event("DRINK",`【智慧紀錄】水杯飲水結算！飲用量: ${d.toFixed(2)} cc`);this.publish("@water/taken",d.toFixed(2),true)}this.base=w;this.state="CUP_SETTLED";this.before=0;this.pickup=false}}
command(c){c=c.trim();let x=c.toLowerCase();Log.serial("CMD","📨 [指令接收] "+c);
if(x==="tare"){
    Log.serial("APP","執行去皮歸零 (Tare)...");
    this.tareOffset=this.physical();
    this.buf=Array(8).fill(0);
    this.filtered=0;
    this.weight=0;
    this.lastLoop=0;
    this.base=0;
    this.stable=0;
    this.before=0;
    this.pickup=false;
    this.state="EMPTY_IDLE";
    this.lock=false;
    if(this.manual){this.state="EMPTY_IDLE";this.lock=false;}
    const ack="TARE_DONE:0.00";
    Log.serial("APP","去皮完成。目前狀態: EMPTY_IDLE 基準重: 0.00g");
    Log.serial("APP",ack);
    this.publish("@msg",ack,true);
    this.publish("@serialraw",ack,true);
    this.publish("@weight","0.00",true);
}
else if(x==="getweight"){
    if(!this.hx){
        const reply="WEIGHT_ERROR:HX711_NOT_READY";
        Log.serial("APP",reply);
        this.publish("@msg",reply,true);
        this.publish("@serialraw",reply,true);
    }else{
        // 模擬 readStableWeightOnce()：10 筆讀值，穩定後才回覆。
        // UI 互動完成後重量是靜止值，因此以目前 Virtual HX711 淨重回覆。
        const one=this.weight;
        const reply="WEIGHT_ONCE:"+one.toFixed(2);
        Log.serial("APP",reply);
        this.publish("@msg",reply,true);
        this.publish("@serialraw",reply,true);
    }
}
else if(x==="manualdrink"||x==="appmanual"){
    this.manual=true;
    this.state="EMPTY_IDLE";
    this.fsm="AUTO";
    this.lock=false;
    this.pickup=false;
    this.before=0;
    this.stable=0;
    const reply="DRINK_MODE:MANUAL";
    Log.serial("APP",reply);
    this.publish("@msg",reply,true);
    this.publish("@serialraw",reply,true);
}
else if(x==="legacyauto"){
    this.manual=false;
    this.fsm="AUTO";
    this.lock=false;
    this.pickup=false;
    this.before=0;
    this.stable=0;
    // executeResetAuto()：依目前淨重重新同步狀態。
    if(Math.abs(this.weight)<10)this.state="EMPTY_IDLE";
    else if(this.weight>=30){this.state="CUP_SETTLED";this.base=this.weight;this.lock=true;}
    else if(this.weight>=10){this.state="BOX_SETTLED";this.base=this.weight;this.lock=true;}
    const reply="DRINK_MODE:LEGACY_AUTO";
    Log.serial("APP",reply);
    this.publish("@msg",reply,true);
    this.publish("@serialraw",reply,true);
}
else if(x==="clear"){this.publish("@medication/taken","0",true);this.publish("@water/taken","0",true);Log.serial("APP","吃藥與飲水數據已重置歸零。")}
else if(x==="forcecup"){this.manual=false;this.fsm="FORCE_CUP";this.state="CUP_SETTLED";this.base=this.weight;this.lock=true}
else if(x==="forcebox"){this.manual=false;this.fsm="FORCE_BOX";this.state="BOX_SETTLED";this.base=this.weight;this.lock=true}
else if(["auto","resetauto","unlock","reset"].includes(x)){this.manual=false;this.fsm="AUTO";this.lock=false;this.state=this.weight>=30?"CUP_SETTLED":this.weight>=10?"BOX_SETTLED":"EMPTY_IDLE";Log.serial("APP","🔄 [指令回復] 已重新啟用 智慧單次自動識別。系統現在是 [UNLOCKED] 狀態")}
else if(x==="streamon"||x==="streamoff")Log.serial("APP","WEIGHT_STREAM:"+(x==="streamon"?"ON":"OFF"));
else if(x.startsWith("setuid:")){
    const uid=c.substring(c.indexOf(":")+1).trim();
    const ack="UID_SET:"+uid;
    Log.serial("UID","✅ "+ack);
    this.publish("@msg",ack,true);
    this.publish("@serialraw",ack,true);
}
else if(x==="clearuid"){
    const ack="UID_CLEARED";
    Log.serial("UID","✅ "+ack);
    this.publish("@msg",ack,true);
    this.publish("@serialraw",ack,true);
}
else if(c.includes(":"))Log.serial("WIFI","💾 [NVS 儲存成功] 新 WiFi 寫入！SSID: "+c.split(":")[0]);
else Log.serial("WARN","⚠️ [指令錯誤] 未知指令: "+c)}
};
