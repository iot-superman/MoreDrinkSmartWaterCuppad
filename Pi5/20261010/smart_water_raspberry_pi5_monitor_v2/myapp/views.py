import json
from datetime import timedelta

from django.contrib import messages
from django.db.models import Sum
from django.db.models.functions import TruncDate
from django.http import JsonResponse
from django.shortcuts import get_object_or_404, redirect, render
from django.utils import timezone
from django.utils.dateparse import parse_datetime
from django.views.decorators.http import require_POST
from django.views.decorators.csrf import csrf_exempt

from .forms import DrinkRecordForm
from .models import DrinkRecord, WaterGoal


def dashboard(request):
    """顯示今日統計、近七日圖表及最近喝水紀錄。"""
    now = timezone.localtime()
    today = now.date()
    goal, _ = WaterGoal.objects.get_or_create(pk=1, defaults={"daily_goal_ml": 2000})

    if request.method == "POST":
        form = DrinkRecordForm(request.POST)
        if form.is_valid():
            record = form.save(commit=False)
            record.source = "manual"
            record.save()
            messages.success(request, f"已新增 {record.amount_ml} ml 喝水紀錄。")
            return redirect("dashboard")
    else:
        form = DrinkRecordForm()

    today_records = DrinkRecord.objects.filter(recorded_at__date=today)
    today_total = today_records.aggregate(total=Sum("amount_ml"))["total"] or 0
    progress = min(round(today_total / goal.daily_goal_ml * 100), 100)
    remaining = max(goal.daily_goal_ml - today_total, 0)

    # 建立固定七天序列，沒有資料的日期也會顯示 0。
    start_date = today - timedelta(days=6)
    totals = {
        item["day"]: item["total"]
        for item in (
            DrinkRecord.objects.filter(recorded_at__date__gte=start_date)
            .annotate(day=TruncDate("recorded_at"))
            .values("day")
            .annotate(total=Sum("amount_ml"))
            .order_by("day")
        )
    }
    days = [start_date + timedelta(days=index) for index in range(7)]
    chart_labels = [day.strftime("%m/%d") for day in days]
    chart_values = [totals.get(day, 0) for day in days]

    # 今日每小時喝水量：固定建立 0～23 時，沒有資料的時段填入 0。
    hourly_values = [0] * 24
    for record in today_records.only("amount_ml", "recorded_at"):
        local_recorded_at = timezone.localtime(record.recorded_at)
        hourly_values[local_recorded_at.hour] += record.amount_ml

    context = {
        "form": form,
        "today_total": today_total,
        "goal": goal,
        "progress": progress,
        "remaining": remaining,
        "record_count": today_records.count(),
        "recent_records": DrinkRecord.objects.all()[:20],
        "chart_labels": json.dumps(chart_labels, ensure_ascii=False),
        "chart_values": json.dumps(chart_values),
        "hourly_labels": json.dumps([f"{hour:02d}:00" for hour in range(24)]),
        "hourly_values": json.dumps(hourly_values),
    }
    return render(request, "myapp/dashboard.html", context)


@require_POST
def update_goal(request):
    """更新每日喝水目標，限制在合理範圍內。"""
    try:
        amount = int(request.POST.get("daily_goal_ml", "0"))
    except ValueError:
        amount = 0
    if not 500 <= amount <= 10000:
        messages.error(request, "每日目標必須介於 500～10000 ml。")
    else:
        goal, _ = WaterGoal.objects.get_or_create(pk=1)
        goal.daily_goal_ml = amount
        goal.save()
        messages.success(request, f"每日目標已更新為 {amount} ml。")
    return redirect("dashboard")


@require_POST
def delete_record(request, pk):
    record = get_object_or_404(DrinkRecord, pk=pk)
    record.delete()
    messages.success(request, "喝水紀錄已刪除。")
    return redirect("dashboard")


@csrf_exempt
@require_POST
def mqtt_ingest(request):
    """預留給 MQTT 訂閱程式呼叫的本地 JSON API。

    本機裝置不會持有瀏覽器 CSRF Token，因此此單一路徑豁免 CSRF。
    若未來部署公網，必須改加 API Key 或 Token 驗證。
    """
    try:
        payload = json.loads(request.body)
        amount = int(round(abs(float(payload.get("amount_ml", payload.get("weight_diff_g"))))))
        if not 1 <= amount <= 3000:
            raise ValueError("喝水量超出範圍")
        # ESP32 可傳 ISO 8601 時間籤；沒傳時才使用 Django 收到資料的時間。
        recorded_at = timezone.now()
        timestamp_text = payload.get("timestamp")
        if timestamp_text:
            parsed_timestamp = parse_datetime(str(timestamp_text))
            if parsed_timestamp is None:
                raise ValueError("timestamp 必須是 ISO 8601 格式")
            if timezone.is_naive(parsed_timestamp):
                parsed_timestamp = timezone.make_aware(
                    parsed_timestamp, timezone.get_current_timezone()
                )
            recorded_at = parsed_timestamp

        record = DrinkRecord.objects.create(
            amount_ml=amount,
            recorded_at=recorded_at,
            source="mqtt",
            device_uid=str(payload.get("uid", ""))[:50],
            note="MQTT 自動記錄",
        )
        return JsonResponse({"ok": True, "id": record.id, "amount_ml": amount}, status=201)
    except (TypeError, ValueError, json.JSONDecodeError) as error:
        return JsonResponse({"ok": False, "error": str(error)}, status=400)
