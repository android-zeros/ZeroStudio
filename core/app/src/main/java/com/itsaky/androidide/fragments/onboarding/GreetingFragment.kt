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

package com.itsaky.androidide.fragments.onboarding

import android.os.Bundle
import android.view.View
import com.itsaky.androidide.databinding.FragmentOnboardingGreetingBinding
import com.itsaky.androidide.fragments.FragmentWithBinding

/**
 * @author Akash Yadav (Original Author)
 * @author android_zero (Modifier)
 *
 * This fragment displays a greeting screen with a Lottie animation.
 * The animation is loaded programmatically from the assets folder.
 */
class GreetingFragment :
  FragmentWithBinding<FragmentOnboardingGreetingBinding>(FragmentOnboardingGreetingBinding::inflate) {

  /**
   * Called when the fragment's view has been created.
   *
   * @param view The View returned by onCreateView().
   * @param savedInstanceState If non-null, this fragment is being re-constructed
   * from a previous saved state as given here.
   */
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    
    binding.lottieAnimationview?.setAnimation("LottieAnimation/code.json")
  }
}