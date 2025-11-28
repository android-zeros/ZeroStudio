package com.itsaky.androidide.preferences

import android.content.Intent
import com.itsaky.androidide.activities.LottieAnimationSelectorActivity
import com.itsaky.androidide.resources.R.string

const val KEY_LOTTIE_ANIMATION = "idepref_lottie_animation"

val lottieAnimationPreference =
    SimpleClickablePreference(
        key = KEY_LOTTIE_ANIMATION,
        title = string.pref_lottie_animation_title,
        summary = string.pref_lottie_animation_summary,
        icon = R.drawable.ic_setting_lottie_animation
    ) {
        it.context.startActivity(Intent(it.context, LottieAnimationSelectorActivity::class.java))
        true
    }