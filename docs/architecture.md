# ADR-001

Title: Standard API Response Format

Status: Accepted

Decision

Every successful API response shall follow:

{
    success,
    message,
    data
}

Reason

- Consistency
- Easier Android parsing
- Easy pagination
- Cleaner frontend