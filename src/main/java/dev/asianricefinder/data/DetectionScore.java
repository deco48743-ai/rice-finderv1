package dev.asianricefinder.data;

public record DetectionScore(int geodes, int clusters, int players, int playerActivity, int total, int confidence, boolean underDeepslate) {
    public String activityLabel() {
        if (playerActivity >= 12) return "HIGH";
        if (playerActivity >= 5) return "MEDIUM";
        return "LOW";
    }
}
