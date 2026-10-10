from django.apps import AppConfig


class MyappConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    # V3：App 名稱改為老師課堂慣用的 myapp。
    name = "myapp"
    verbose_name = "喝水管理"
