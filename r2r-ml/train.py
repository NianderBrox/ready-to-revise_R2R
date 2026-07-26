from random import Random

from src.simulator.student import Student

rng = Random(42)

student = Student(
    student_id=1,
    rng=rng
)

student.new_session(1)

student.update_after_review(
    correct=True,
    confidence=3,
    response_time=18
)

student.update_after_review(
    correct=False,
    confidence=1,
    response_time=45
)

student.update_after_review(
    correct=True,
    confidence=2,
    response_time=22
)

print("Student Statistics")
print("-------------------")
print("Total Revisions :", student.total_revisions)
print("Success Rate :", round(student.success_rate, 2))
print("Average Confidence :", round(student.average_confidence, 2))
print("Average Response Time :", round(student.average_response_time, 2))
print("Study Streak :", student.study_streak)
print("Preferred Hour :", student.preferred_study_hour)