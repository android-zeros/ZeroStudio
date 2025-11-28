package com.catpuppyapp.puppygit.notification

import com.catpuppyapp.puppygit.notification.base.NotifyBase

class HttpServiceExecuteNotify private constructor(
    override val notifyId:Int
): NotifyBase(
    TAG = "HttpServiceExecuteNotify",
    channelId="http_service_execute_notify",
    channelName = "Http Service Execute",
    channelDesc = "Show Http Service Execute Notifications"
){

    companion object {
        fun create(notifyId:Int):NotifyBase {
            return HttpServiceExecuteNotify(notifyId)
        }

    }

}
