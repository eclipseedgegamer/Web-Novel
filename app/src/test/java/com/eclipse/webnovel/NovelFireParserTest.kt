package com.eclipse.webnovel

import com.eclipse.webnovel.data.source.NovelFireParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** NovelFire parser against recorded HTML fixtures (no live network). */
class NovelFireParserTest {

    private val base = "https://novelfire.net"

    private fun fixture(name: String, baseUri: String): Document {
        val stream = requireNotNull(javaClass.classLoader?.getResourceAsStream("novelfire/$name")) {
            "missing fixture novelfire/$name"
        }
        return Jsoup.parse(stream, "UTF-8", baseUri)
    }

    @Test
    fun parsesSearchResults() {
        val list = NovelFireParser.parseList(fixture("search.html", "$base/search"), base)
        assertTrue("expected results, got ${list.size}", list.size >= 3)
        assertTrue(list.first().url.contains("/book/"))
        assertTrue(list.first().title.isNotBlank())
    }

    @Test
    fun parsesDetail() {
        val detail = NovelFireParser.parseDetail(
            fixture("book.html", "$base/book/chrysalis"),
            fixture("toc.html", "$base/book/chrysalis/chapters"),
            base,
        )
        assertEquals("Chrysalis", detail.summary.title)
        assertTrue(detail.author?.contains("Rinoz", ignoreCase = true) == true)
        assertTrue("expected >=100 chapters, got ${detail.chapters.size}", detail.chapters.size >= 100)
        assertTrue(detail.chapters.first().url.contains("/chapter-"))
    }

    @Test
    fun parsesChapter() {
        val chapter = NovelFireParser.parseChapter(fixture("chapter.html", "$base/chapter"))
        assertTrue(chapter.title.contains("Anthony Reborn"))
        assertTrue("expected several paragraphs, got ${chapter.paragraphs.size}", chapter.paragraphs.size >= 5)
    }
}
