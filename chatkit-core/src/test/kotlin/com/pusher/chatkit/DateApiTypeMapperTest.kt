package com.pusher.chatkit

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.pusher.chatkit.util.DateApiTypeMapper
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class DateApiTypeMapperTest : Spek({

    describe("given a valid api type date") {
        val dateApiTypeString = "2017-04-13T14:10:38Z"

        describe("when parsed") {
            val epochTime = DateApiTypeMapper().mapToEpochTime(dateApiTypeString)

            it("then the epochTime will be correct") {
                assertThat(epochTime).isEqualTo(1492092638000L)
            }
        }
    }
})
