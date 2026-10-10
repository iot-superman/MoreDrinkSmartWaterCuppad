from django.urls import path

from . import views

urlpatterns = [
    path("", views.dashboard, name="dashboard"),
    path("goal/", views.update_goal, name="update_goal"),
    path("records/<int:pk>/delete/", views.delete_record, name="delete_record"),
    path("api/drinks/mqtt/", views.mqtt_ingest, name="mqtt_ingest"),
]

