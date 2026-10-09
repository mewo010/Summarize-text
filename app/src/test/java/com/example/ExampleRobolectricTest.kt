package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GeminiSummarizerService
import com.example.data.SummaryLength
import com.example.data.SummaryMode
import com.example.data.SummaryTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Brevity", appName)
    }

    @Test
    fun `extractive summarizer condenses long sentences`() {
        val service = GeminiSummarizerService()
        val longPassage =
            "It is important to note that contemporary large language models utilize transformer architectures in order to process long-range dependencies. " +
                "Due to the fact that memory scaling grows rapidly, engineers deploy sparse attention to reduce latency. " +
                "At this point in time, mobile devices can run distilled models efficiently."

        val output = service.localExtractiveSynthesize(
            text = longPassage,
            mode = SummaryMode.EXECUTIVE,
            length = SummaryLength.CONCISE,
            tone = SummaryTone.DIRECT
        )

        assertTrue(output.summary.isNotBlank())
        assertTrue(output.keyTakeaways.isNotEmpty())
        assertTrue(output.simplifiedSentences.isNotEmpty())
        assertTrue(output.keywords.isNotEmpty())
    }
}
