#!/usr/bin/env bash
#
# R2R end-to-end flow: register -> login -> upload -> poll READY ->
# generate MCQs -> verify +24h due stamp -> review xN -> recommendations.
#
# Usage:
#   ./scripts/e2e.sh [BASE_URL]
#
# Requires: curl, jq, python3 (uuid gen fallback)
set -euo pipefail

BASE_URL="${1:-http://localhost:3000}"

SAMPLE_PDF="${SAMPLE_PDF:-$(dirname "$0")/../backend/samples/LAB2OMIMandPUBMED.pdf}"
REVIEW_COUNT="${REVIEW_COUNT:-10}"
POLL_TRIES=60
POLL_SLEEP=2

PASS=0
FAIL=0

step() { printf '\n== %s\n' "$1"; }

check() {
    local label="$1" condition="$2"
    if eval "$condition"; then
        echo "PASS: $label"
        PASS=$((PASS + 1))
    else
        echo "FAIL: $label"
        FAIL=$((FAIL + 1))
    fi
}

require() {
    command -v "$1" >/dev/null 2>&1 || { echo "missing dependency: $1"; exit 2; }
}

require curl
require jq

[ -f "$SAMPLE_PDF" ] || { echo "sample PDF not found: $SAMPLE_PDF"; exit 2; }

RUN_ID="$(date +%s)"
EMAIL="e2e_${RUN_ID}@example.com"
PASSWORD="E2ePassw0rd!"
SESSION_ID="$(python3 -c 'import uuid; print(uuid.uuid4())')"

step "health"
HTTP="$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/api/v1/health" || true)"
check "backend reachable ($BASE_URL)" "[ \"$HTTP\" = 200 ] || [ \"$HTTP\" = 404 ]"

step "register"
REGISTER="$(curl -s -X POST "$BASE_URL/api/v1/auth/register" \
    -H 'Content-Type: application/json' \
    -d "{\"name\":\"E2E\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")"
TOKEN="$(echo "$REGISTER" | jq -r '.data.accessToken // empty')"
check "register returned accessToken" "[ -n \"$TOKEN\" ]"

step "upload document (multipart)"
UPLOAD="$(curl -s -X POST "$BASE_URL/api/v1/documents" \
    -H "Authorization: Bearer $TOKEN" \
    -F "file=@$SAMPLE_PDF;type=application/pdf")"
DOC_ID="$(echo "$UPLOAD" | jq -r '.data.documentId // empty')"
STATUS="$(echo "$UPLOAD" | jq -r '.data.status // empty')"
check "upload returned documentId" "[ -n \"$DOC_ID\" ]"

step "poll until READY"
for _ in $(seq 1 "$POLL_TRIES"); do
    STATUS="$(curl -s "$BASE_URL/api/v1/documents/$DOC_ID" \
        -H "Authorization: Bearer $TOKEN" | jq -r '.data.status // empty')"
    [ "$STATUS" = "READY" ] && break
    [ "$STATUS" = "FAILED" ] && break
    sleep "$POLL_SLEEP"
done
check "document status == READY (got: ${STATUS:-none})" "[ \"$STATUS\" = \"READY\" ]"

step "generate MCQs from document"
GEN="$(curl -s -X POST "$BASE_URL/api/v1/documents/$DOC_ID/questions" \
    -H "Authorization: Bearer $TOKEN")"
COUNT="$(echo "$GEN" | jq '.data | length')"
check "generated >= 1 study item" "[ \"${COUNT:-0}\" -ge 1 ]"

FIRST_ITEM_ID="$(echo "$GEN" | jq -r '.data[0].id')"
NOW_MS="$(date +%s)"
DUE_AT_MS="$(echo "$GEN" | jq -r ".data[0].nextReviewAt // empty" | awk '{print $1}' | xargs -I{} date -d {} +%s 2>/dev/null || true)"

step "verify initial nextReviewAt ~= created +24h (D10)"
CREATED_MS="$(echo "$GEN" | jq -r '.data[0].createdAt' | awk '{print $1}' | xargs -I{} date -d {} +%s 2>/dev/null || true)"
if [ -n "$CREATED_MS" ] && [ -n "$DUE_AT_MS" ]; then
    DELTA_H=$(( (DUE_AT_MS - CREATED_MS) / 3600 ))
    check "delta hours in [23..25] (got: ${DELTA_H}h)" "[ $DELTA_H -ge 23 ] && [ $DELTA_H -le 25 ]"
else
    check "parse createdAt/nextReviewAt" "false"
fi

step "due listing (should be empty before next day or include older items)"
curl -s "$BASE_URL/api/v1/study-items?type=QUESTION&due=true" \
    -H "Authorization: Bearer $TOKEN" > /dev/null
check "GET study-items?due=true responded" "true"

step "submit $REVIEW_COUNT reviews with raw observables"
ITEMS_JSON="$GEN"
ITEM_TOTAL="$(echo "$ITEMS_JSON" | jq '.data | length')"
[ "$ITEM_TOTAL" -lt "$REVIEW_COUNT" ] && REVIEW_COUNT=$ITEM_TOTAL
i=0
while [ "$i" -lt "$REVIEW_COUNT" ]; do
    ITEM_ID="$(echo "$ITEMS_JSON" | jq -r ".data[$i].id")"
    OPTION_COUNT=$(echo "$ITEMS_JSON" | jq ".data[$i].options | length")
    if [ "${OPTION_COUNT:-0}" -ge 1 ]; then
        PICK=$(( RANDOM % OPTION_COUNT ))
        RT=$(( 3000 + RANDOM % 90000 ))
        HES=$(( 200 + RANDOM % 20000 ))
        REVIEW="$(curl -s -X POST "$BASE_URL/api/v1/reviews" \
            -H "Authorization: Bearer $TOKEN" \
            -H 'Content-Type: application/json' \
            -d "{
                \"studyItemId\": \"$ITEM_ID\",
                \"selectedOptionIndex\": $PICK,
                \"responseTimeMs\": $RT,
                \"hesitationMs\": $HES,
                \"answerChanges\": 0,
                \"sessionId\": \"$SESSION_ID\"
            }")"
        RESULT_OK="$(echo "$REVIEW" | jq -r '.success // false')"
        HAS_RESULT="$(echo "$REVIEW" | jq -r '.data.result // empty')"
        CONF="$(echo "$REVIEW" | jq -r '.data.confidenceScore // empty')"
        NEXT="$(echo "$REVIEW" | jq -r '.data.nextReviewAt // empty')"
        check "review[$i] derived result=$HAS_RESULT confidence=$CONF next=$NEXT" \
              "[ \"$RESULT_OK\" = \"true\" ] && [ -n \"$NEXT\" ]"
    else
        check "review[$i] skipped (item has no options array)" "false"
    fi
    i=$((i + 1))
done

step "self-grade swipe (D12 MEMORIZED x1.5)"
FIRST_ITEM_ID="$(echo "$ITEMS_JSON" | jq -r '.data[0].id')"
SELF_GRADE="$(curl -s -X POST "$BASE_URL/api/v1/reviews/self-grade" \
    -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{
        \"studyItemId\": \"$FIRST_ITEM_ID\",
        \"responseTimeMs\": 2500,
        \"sessionId\": \"$SESSION_ID\"
    }")"
SG_OK="$(echo "$SELF_GRADE" | jq -r '.success // false')"
SG_RESULT="$(echo "$SELF_GRADE" | jq -r '.data.result // empty')"
SG_INTERVAL="$(echo "$SELF_GRADE" | jq -r '.data.intervalDays // 0')"
check "self-grade result=$SG_RESULT interval=${SG_INTERVAL}d (expect >=10)" \
      "[ \"$SG_OK\" = \"true\" ] && [ \"$SG_RESULT\" = \"MEMORIZED\" ] && [ \"${SG_INTERVAL:-0}\" -ge 10 ]"

step "docx upload (Track 3: mammoth text extraction)"
DOCX_PATH="$(mktemp /tmp/r2r_e2e_XXXXXX.docx)"
python3 - "$DOCX_PATH" <<'PY'
import sys, zipfile
paras = [
    "E2E Word Document",
    "",
    "Spaced repetition schedules reviews at increasing intervals to improve "
    "long-term retention of studied material.",
    "",
    "The testing effect shows that actively recalling information strengthens "
    "memory more than passive re-reading.",
]
body = "".join(f'<w:p><w:r><w:t>{p}</w:t></w:r></w:p>' for p in paras)
document = (
    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
    '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">'
    f'<w:body>{body}</w:body></w:document>'
)
content_types = (
    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
    '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
    '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
    '<Default Extension="xml" ContentType="application/xml"/>'
    '<Override PartName="/word/document.xml" '
    'ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>'
    '</Types>'
)
rels = (
    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
    '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
    '<Relationship Id="rId1" '
    'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" '
    'Target="word/document.xml"/></Relationships>'
)
with zipfile.ZipFile(sys.argv[1], "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("[Content_Types].xml", content_types)
    z.writestr("_rels/.rels", rels)
    z.writestr("word/document.xml", document)
PY
DOCX_UPLOAD="$(curl -s -X POST "$BASE_URL/api/v1/documents" \
    -H "Authorization: Bearer $TOKEN" \
    -F "file=@$DOCX_PATH;type=application/vnd.openxmlformats-officedocument.wordprocessingml.document")"
rm -f "$DOCX_PATH"
DOCX_ID="$(echo "$DOCX_UPLOAD" | jq -r '.data.documentId // empty')"
DOCX_STATUS="$(echo "$DOCX_UPLOAD" | jq -r '.data.status // empty')"
if [ -n "$DOCX_ID" ] && [ "$DOCX_STATUS" != "READY" ] && [ "$DOCX_STATUS" != "FAILED" ]; then
    for _ in $(seq 1 "$POLL_TRIES"); do
        DOCX_STATUS="$(curl -s "$BASE_URL/api/v1/documents/$DOCX_ID" \
            -H "Authorization: Bearer $TOKEN" | jq -r '.data.status // empty')"
        [ "$DOCX_STATUS" = "READY" ] && break
        [ "$DOCX_STATUS" = "FAILED" ] && break
        sleep "$POLL_SLEEP"
    done
fi
check "docx status == READY (got: ${DOCX_STATUS:-none})" "[ \"$DOCX_STATUS\" = \"READY\" ]"

step "generate MCQs from docx"
DOCX_GEN="$(curl -s -X POST "$BASE_URL/api/v1/documents/$DOCX_ID/questions" \
    -H "Authorization: Bearer $TOKEN")"
DOCX_COUNT="$(echo "$DOCX_GEN" | jq '.data | length')"
check "docx generated >= 1 study item (got: ${DOCX_COUNT:-0})" "[ \"${DOCX_COUNT:-0}\" -ge 1 ]"

step "recommendations v2 (slipping-soon: day-one + options + forget-date)"
REC="$(curl -s "$BASE_URL/api/v1/recommendations?limit=20" \
    -H "Authorization: Bearer $TOKEN")"
SOURCE="$(echo "$REC" | jq -r '.data.source // empty')"
RANKED="$(echo "$REC" | jq '.data.items | length')"
check "recommendations source=$SOURCE count=${RANKED}" "[ -n \"$SOURCE\" ] && [ \"${RANKED:-0}\" -ge 1 ]"

FIRST_ID="$(echo "$REC" | jq -r '.data.items[0].studyItemId // empty')"
OPT_COUNT="$(echo "$REC" | jq '.data.items[0].options | length // 0')"
FORGET="$(echo "$REC" | jq -r '.data.items[0].expectedForgetDate // empty')"
check "E1 day-one item has options(${OPT_COUNT}) + forget-date" \
      "[ -n \"$FIRST_ID\" ] && [ \"${OPT_COUNT:-0}\" -ge 2 ] && [ -n \"$FORGET\" ]"

step "E2 answer via recommended id"
REC_REVIEW="$(curl -s -X POST "$BASE_URL/api/v1/reviews" \
    -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{
        \"studyItemId\": \"$FIRST_ID\",
        \"selectedOptionIndex\": 0,
        \"responseTimeMs\": 8000,
        \"hesitationMs\": 900,
        \"answerChanges\": 0,
        \"sessionId\": \"$SESSION_ID\"
    }")"
REC_OK="$(echo "$REC_REVIEW" | jq -r '.success // false')"
check "review via recommended id accepted" "[ \"$REC_OK\" = \"true\" ]"

step "E3 refractory excludes just-answered id"
sleep 1
REC2="$(curl -s "$BASE_URL/api/v1/recommendations?limit=20" \
    -H "Authorization: Bearer $TOKEN")"
STILL_THERE="$(echo "$REC2" | jq --arg id "$FIRST_ID" \
    '[.data.items[] | select(.studyItemId == $id)] | length')"
check "answered id absent from immediate re-call" "[ \"${STILL_THERE:-1}\" = \"0\" ]"

printf '\n===== RESULT: %d passed, %d failed =====\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
