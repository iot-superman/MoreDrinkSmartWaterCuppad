from django.db import migrations, models


class Migration(migrations.Migration):
    initial = True
    dependencies = []
    operations = [
        migrations.CreateModel(
            name="DrinkRecord",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("amount_ml", models.PositiveIntegerField(verbose_name="喝水量（ml）")),
                ("recorded_at", models.DateTimeField(verbose_name="喝水時間")),
                ("source", models.CharField(choices=[("manual", "手動輸入"), ("mqtt", "MQTT 智慧杯墊")], default="manual", max_length=10, verbose_name="資料來源")),
                ("device_uid", models.CharField(blank=True, max_length=50, verbose_name="裝置 UID")),
                ("note", models.CharField(blank=True, max_length=100, verbose_name="備註")),
                ("created_at", models.DateTimeField(auto_now_add=True, verbose_name="建立時間")),
            ],
            options={"verbose_name": "喝水紀錄", "verbose_name_plural": "喝水紀錄", "ordering": ["-recorded_at", "-id"]},
        ),
        migrations.CreateModel(
            name="WaterGoal",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("daily_goal_ml", models.PositiveIntegerField(default=2000, verbose_name="每日目標（ml）")),
                ("updated_at", models.DateTimeField(auto_now=True, verbose_name="更新時間")),
            ],
        ),
    ]

