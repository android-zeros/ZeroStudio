package com.catpuppyapp.puppygit.notification

import com.catpuppyapp.puppygit.notification.base.NotifyBase

/**
 * 执行自动化行为的通知，就是app打开、关闭时发送通知的渠道
 */
class AutomationNotify private constructor(
    override val notifyId:Int
): NotifyBase(
    TAG = "AutomationNotify",
    channelId="automation_notify",
    channelName = "Automation",
    channelDesc = "Show Automation Notifications",
) {
    companion object {
        fun create(notifyId:Int):NotifyBase {
            return AutomationNotify(notifyId)
        }
    }
}
