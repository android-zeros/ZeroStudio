/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.itsaky.androidide.activities

import android.animation.Animator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.airbnb.lottie.LottieAnimationView
import com.itsaky.androidide.R
import com.itsaky.androidide.app.configuration.IJdkDistributionProvider
import com.itsaky.androidide.preferences.internal.GeneralPreferences
import java.io.File
import java.io.FileInputStream



/**About
*version：2.0  Update time：2025.10.25,22:25
* version 1.0：1.0 version SplashActivity Produced by @ author Akash Yadav, SplashActivity provides basic startup interface effects
*version 2.0：Version 2.0 was created by @ author: android_zero, adding Lottie animation JSON loading and customization to make you enjoy it
*Support custom Lottie animation file import and output, so your preferences are no longer restricted
 * @author：android_zero
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val lottieView = findViewById<LottieAnimationView>(R.id.lottie_animation_view)
        
        loadAndPlayAnimation(lottieView)

        lottieView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                goToNextActivity()
            }

            override fun onAnimationCancel(animation: Animator) {
                goToNextActivity()
            }

            override fun onAnimationRepeat(animation: Animator) {}
        })
    }
    
    private fun loadAndPlayAnimation(lottieView: LottieAnimationView) {
        val animationPath = GeneralPreferences.lottieAnimation
        try {
            val animationFile = File(animationPath)
            if (animationFile.exists() && animationFile.isFile) {
                // 从文件加载
                lottieView.setAnimation(FileInputStream(animationFile), animationFile.name)
            } else {
                // 从 assets 加载
                lottieView.setAnimation(animationPath)
            }
            lottieView.playAnimation()
        } catch (e: Exception) {
            // 加载失败时，显示Toast并直接跳转
            Toast.makeText(this, getString(R.string.error_loading_animation, e.message), Toast.LENGTH_LONG).show()
            // 加载默认动画作为后备
            try {
                lottieView.setAnimation(GeneralPreferences.DEFAULT_LOTTIE_ANIMATION)
                lottieView.playAnimation()
            } catch (defaultException: Exception) {
                 // 如果默认动画也失败，则直接跳转
                 goToNextActivity()
            }
        }
    }

    private fun goToNextActivity() {
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }
}