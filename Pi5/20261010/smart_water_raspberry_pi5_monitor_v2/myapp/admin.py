from django.contrib import admin

from .models import DrinkRecord, WaterGoal


@admin.register(DrinkRecord)
class DrinkRecordAdmin(admin.ModelAdmin):
    list_display = ("recorded_at", "amount_ml", "source", "device_uid", "note")
    list_filter = ("source", "recorded_at")
    search_fields = ("device_uid", "note")


admin.site.register(WaterGoal)

