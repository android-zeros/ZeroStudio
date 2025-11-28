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

package com.itsaky.androidide.templates

/**
 * Android API versions.
 
 * It can be viewed here Android api levels：https://apilevels.com/
 *
 * @author Akash Yadav (Historical contributors)
 *
 *更新日志：  @author android_zero
 *2025.10.3：
 1.新增VANILLA_ICE_CREAM（API36）和BAKLAVA（API36）。
 2.预留CinnamonBun，待2026年启用
 *
 */
 
// API Version     dessert code                  release date
// 15       香草冰淇淋 Vanilla Ice Cream	2024
// 16       巴克拉瓦 Baklava	               2025
// 17	    肉桂卷 Cinnamon Bun	     2026


enum class Sdk(val codename: String, val version: String, val api: Int) {

  JellyBean("Jelly Bean", "4.1", 16),
  JellyBeanMR1("Jelly Bean", "4.2", 17),
  JellyBeanMR2("Jelly Bean", "4.3", 18),
  KitKat("KitKat", "4.4", 19),
  KitKatWatch("KitKat Watch", "4.4W", 20),
  Lollipop("Lollipop", "5.0", 21),
  LollipopMR1("Lollipop", "5.1", 22),
  Marshmallow("Marshmallow", "6.0", 23),
  Naughat("Naughat", "7.0", 24),
  NaughtMR1("Naughat", "7.1", 25),
  Oreo("Oreo", "8.0", 26),
  OreoMR1("Oreo", "8.1", 27),
  Pie("Pie", "9.0", 28),
  QuinceTart("Q", "10", 29),
  RedVelvetCake("R", "11", 30),
  SnowCone("SnowCone", "12", 31),
  SnowCodeV2("SnowCone", "12L", 32),
  Tiramisu("Tiramisu", "13", 33),
  UpsideDownCake("UpsideDownCake", "14", 34),
  VANILLA_ICE_CREAM("VanillaIceCream", "15" , 35),
  BAKLAVA("BAKLAVA", "16", 36);
  // CinnamonBun("CinnamonBun", "17", 37); // will be launched around June 2026

  /**
   * Get the display name for this Sdk version.
   *
   * @return The display name.
   */
  fun displayName() : String = "API ${api}: Android $version (${codename})"
}