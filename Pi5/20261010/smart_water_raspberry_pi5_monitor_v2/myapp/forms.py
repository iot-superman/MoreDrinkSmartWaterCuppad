from django import forms
from django.utils import timezone

from .models import DrinkRecord


class DrinkRecordForm(forms.ModelForm):
    class Meta:
        model = DrinkRecord
        fields = ["amount_ml", "recorded_at", "note"]
        widgets = {
            "amount_ml": forms.NumberInput(
                attrs={"class": "form-control", "min": 1, "max": 3000}
            ),
            "recorded_at": forms.DateTimeInput(
                attrs={"class": "form-control", "type": "datetime-local"},
                format="%Y-%m-%dT%H:%M",
            ),
            "note": forms.TextInput(
                attrs={"class": "form-control", "placeholder": "例如：早餐後"}
            ),
        }

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields["recorded_at"].input_formats = ["%Y-%m-%dT%H:%M"]
        if not self.is_bound:
            self.initial["recorded_at"] = timezone.localtime().strftime(
                "%Y-%m-%dT%H:%M"
            )

    def clean_amount_ml(self):
        amount = self.cleaned_data["amount_ml"]
        if amount > 3000:
            raise forms.ValidationError("單次喝水量不可超過 3000 ml。")
        return amount

