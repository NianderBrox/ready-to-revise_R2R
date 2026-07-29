from src.scheduler.schedule_service import schedule_question_review

result = schedule_question_review(
    correct=True,
    confidence="HIGH",
)

print(result)
