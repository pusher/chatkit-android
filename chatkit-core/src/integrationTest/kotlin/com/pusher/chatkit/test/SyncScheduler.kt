package com.pusher.chatkit.test

import com.pusher.platform.MainThreadScheduler
import com.pusher.platform.ScheduledJob
import com.pusher.platform.network.Futures

class SyncScheduler : MainThreadScheduler {
    override fun schedule(action: () -> Unit): ScheduledJob {
        action()
        return object : ScheduledJob {
            override fun cancel() {}
        }
    }

    override fun schedule(delay: Long, action: () -> Unit): ScheduledJob = schedule(action)
}

class AsyncScheduler : MainThreadScheduler {
    override fun schedule(action: () -> Unit): ScheduledJob = schedule(0, action)

    override fun schedule(delay: Long, action: () -> Unit): ScheduledJob = object : ScheduledJob {

        val pending = Futures.schedule {
            if(delay > 0) Thread.sleep(delay)
            if (!Thread.interrupted()) action()
        }

        override fun cancel() {
            pending.cancel(true)
        }
    }
}
