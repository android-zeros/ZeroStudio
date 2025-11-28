
AndroidIDE Crash Report
Version : vv20251119 (2025111902)
CI Build : false
Branch : ZeroStudio-devs
Commit : c3df2e0
Variant : arm64-v8a (dev)
Build type : UNOFFICIAL
F-Droid Build : false
F-Droid Version : null
F-Droid Version code : -1
SDK Version : 35
Supported ABIs : [arm64-v8a, armeabi-v7a, armeabi]
Manufacturer : Xiaomi
Device : 2304FPN6DC

Stacktrace:
java.lang.IllegalStateException: Activity has been destroyed
	at com.itsaky.androidide.activities.PreferencesActivity.getBinding(PreferencesActivity.kt:34)
	at com.itsaky.androidide.activities.PreferencesActivity.onApplySystemBarInsets(PreferencesActivity.kt:68)
	at com.itsaky.androidide.app.EdgeToEdgeIDEActivity.applyEdgeToEdge(EdgeToEdgeIDEActivity.kt:155)
	at com.itsaky.androidide.app.EdgeToEdgeIDEActivity.onCreate(EdgeToEdgeIDEActivity.kt:131)
	at com.itsaky.androidide.activities.PreferencesActivity.onCreate(PreferencesActivity.kt:41)
	at android.app.Activity.performCreate(Activity.java:9217)
	at android.app.Activity.performCreate(Activity.java:9173)
	at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1538)
	at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:4410)
 Caused by: java.lang.RuntimeException: Unable to start activity ComponentInfo{com.itsaky.androidide/com.itsaky.androidide.activities.PreferencesActivity}: java.lang.IllegalStateException: Activity has been destroyed
	at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:4428)
	at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:4649)
	at android.app.ActivityThread.handleRelaunchActivityInner(ActivityThread.java:6788)
	at android.app.ActivityThread.handleRelaunchActivity(ActivityThread.java:6679)
	at android.app.servertransaction.ActivityRelaunchItem.execute(ActivityRelaunchItem.java:79)
	at android.app.servertransaction.ActivityTransactionItem.execute(ActivityTransactionItem.java:60)
	at android.app.servertransaction.TransactionExecutor.executeNonLifecycleItem(TransactionExecutor.java:186)
	at android.app.servertransaction.TransactionExecutor.executeTransactionItems(TransactionExecutor.java:112)
	at android.app.servertransaction.TransactionExecutor.execute(TransactionExecutor.java:84)
	at android.app.ClientTransactionHandler.executeTransaction(ClientTransactionHandler.java:72)
	at android.app.ActivityThread.handleRelaunchActivityLocally(ActivityThread.java:6745)
	at android.app.ActivityThread$H.handleMessage(ActivityThread.java:2898)
	at android.os.Handler.dispatchMessage(Handler.java:107)
	at android.os.Looper.loopOnce(Looper.java:249)
	at android.os.Looper.loop(Looper.java:337)
	at android.app.ActivityThread.main(ActivityThread.java:9604)
	at java.lang.reflect.Method.invoke(Native Method)
	at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:615)
	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:936)

    