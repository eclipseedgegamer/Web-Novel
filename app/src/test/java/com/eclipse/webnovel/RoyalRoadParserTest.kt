package com.eclipse.webnovel

import com.eclipse.webnovel.data.source.RoyalRoadParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parser is exercised against recorded RoyalRoad HTML fixtures (no live network). */
class RoyalRoadParserTest {

    private val base = "https://www.royalroad.com"

    private fun fixture(name: String, baseUri: String): Document {
        val stream = requireNotNull(javaClass.classLoader?.getResourceAsStream("royalroad/$name")) {
            "missing fixture royalroad/$name"
        }
        return Jsoup.parse(stream, "UTF-8", baseUri)
    }

    @Test
    fun parsesRisingStarsList() {
        val list = RoyalRoadParser.parseList(
            fixture("rising-stars.html", "$base/fictions/rising-stars"),
            base,
        )
        assertTrue("expected many items, got ${list.size}", list.size >= 10)
        val first = list.first()
        assertTrue(first.title.isNotBlank())
        assertTrue(first.url.startsWith("$base/fiction/"))
        assertTrue(first.coverUrl?.startsWith("https://") == true)
    }

    @Test
    fun parsesFictionDetail() {
        val detail = RoyalRoadParser.parseDetail(
            fixture("fiction.html", "$base/fiction/21220/mother-of-learning"),
            base,
        )
        assertEquals("Mother of Learning", detail.summary.title)
        assertEquals("nobody103", detail.author)
        assertTrue(detail.description.length > 100)
        assertTrue("expected >=100 chapters, got ${detail.chapters.size}", detail.chapters.size >= 100)
        assertTrue(detail.chapters.first().url.contains("/chapter/"))
    }

    @Test
    fun parsesChapterContent() {
        val chapter = RoyalRoadParser.parseChapter(fixture("chapter.html", "$base/chapter"))
        assertTrue(chapter.title.contains("Good Morning Brother"))
        assertTrue("expected several paragraphs, got ${chapter.paragraphs.size}", chapter.paragraphs.size >= 5)
        assertTrue(chapter.paragraphs.sumOf { it.length } > 500)
    }
}
