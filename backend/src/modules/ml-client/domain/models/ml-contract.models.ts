export type MlModelName =
    | 'gradient_boosting'
    | 'hist_gradient_boosting'
    | 'random_forest'
    | 'logistic_regression';

export interface ReviewFeatures {
    total_revisions?: number | null;
    success_rate?: number | null;
    average_confidence?: number | null;
    average_response_time?: number | null;
    average_hesitation?: number | null;

    difficulty: number;
    word_count: number;
    character_count: number;

    session_duration_minutes: number;
    question_position_in_session: number;
    days_since_last_session?: number | null;

    review_interval_days?: number | null;
    repetition_number: number;

    last_review_confidence_score?: number | null;
    last_review_response_time?: number | null;
    last_review_hesitation?: number | null;

    answer_changes: number;

    subject: string;
    topic: string;
    hour_of_day: number;
    day_of_week: number;

    last_review_correct?: boolean | null;

    fsrs_recall_probability?: number | null;
    had_fsrs_estimate?: boolean | null;

    user_success_rate?: number | null;
    user_average_confidence?: number | null;
    user_average_response_time?: number | null;

    question_global_success_rate?: number | null;

    recent_success_rate_5?: number | null;
    recent_confidence_5?: number | null;

    consecutive_correct?: number | null;

    hesitation_response_ratio?: number | null;

    normalized_interval_days?: number | null;
    normalized_repetition_number?: number | null;
    normalized_avg_response_time?: number | null;
    normalized_avg_hesitation?: number | null;
}

export interface MlCandidate {
    question_id: string;
    features: ReviewFeatures;
}

export interface MlPredictRequest {
    model_name?: MlModelName;
    features: ReviewFeatures;
}

export interface MlPredictionResponse {
    recall_probability: number;
    predicted_correct: boolean;
    threshold: number;
    model_name: string;
}

export interface MlRecommendRequest {
    candidates: MlCandidate[];
    top_k?: number;
    model_name?: MlModelName;
}

export interface MlRecommendation {
    rank: number;
    question_id: string;
    recall_probability: number;
    priority: string;
}

export interface MlRecommendResponse {
    recommendations: MlRecommendation[];
    model_name: string;
}

export interface MlHealthResponse {
    status: string;
    model_loaded: boolean;
}
