package org.wpfy.carlauncher.feature.onboarding

internal enum class OnboardingStep {
    Setup,
    RailPosition,
}

internal data class SetupCapabilityStatus(
    val preciseLocation: Boolean,
    val notificationAccess: Boolean,
    val defaultHome: Boolean,
) {
    val allSatisfied: Boolean
        get() = preciseLocation && notificationAccess && defaultHome
}

internal fun nextOnboardingStep(current: OnboardingStep): OnboardingStep {
    return when (current) {
        OnboardingStep.Setup -> OnboardingStep.RailPosition
        OnboardingStep.RailPosition -> OnboardingStep.RailPosition
    }
}
