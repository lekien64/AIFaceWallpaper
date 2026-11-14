package com.lekien64.aifacewallpaper.capture

enum class FaceAngle(val label: String) {
    FRONT("Chính diện"),
    LEFT_30("Trái 30°"),
    RIGHT_30("Phải 30°"),
    LEFT_60("Trái 60°"),
    RIGHT_60("Phải 60°"),
    SLIGHTLY_UP("Hơi ngẩng lên"),
    SLIGHTLY_DOWN("Hơi cúi xuống")
}

data class CaptureQuality(
    val sharpness: Boolean,
    val exposure: Boolean,
    val facePosition: Boolean,
    val occlusion: Boolean,
    val identityConsistency: Boolean
) {
    val passed: Boolean
        get() = sharpness &&
            exposure &&
            facePosition &&
            occlusion &&
            identityConsistency
}

data class AngleCapture(
    val angle: FaceAngle,
    val quality: CaptureQuality?,
    val localImageUri: String?
)

data class FaceCoverage(val captures: List<AngleCapture>) {
    val missingOrPoorAngles: List<FaceAngle>
        get() = FaceAngle.values().filter { angle ->
            captures.firstOrNull { it.angle == angle }?.let {
                it.quality?.passed == true &&
                    !it.localImageUri.isNullOrBlank()
            } != true
        }

    val ready: Boolean
        get() = missingOrPoorAngles.isEmpty()
}
