package com.eclipse.webnovel.data.source

import com.eclipse.webnovel.data.model.ChapterContent
import com.eclipse.webnovel.data.model.ChapterRef
import com.eclipse.webnovel.data.model.NovelDetail
import com.eclipse.webnovel.data.model.NovelSummary
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Pure Jsoup parsing for NovelFire — Android/network-free so JVM tests exercise it
 * against recorded fixtures (see NovelFireParserTest).
 */
object NovelFireParser {

    const val SOURCE_ID = "novelfire"

    /** Search results and browse lists share the `.novel-item` card markup. */
    fun parseList(doc: Document, baseUrl: String): List<NovelSummary> =
        doc.select(".novel-item").mapNotNull { item ->
            val link = item.selectFirst("a[href*=/book/]") ?: return@mapNotNull null
            val href = link.attr("href").ifBlank { return@mapNotNull null }
            val title = (item.selectFirst(".novel-title")?.text()?.trim()
                ?: link.attr("title").trim()).ifBlank { return@mapNotNull null }
            val img = item.selectFirst("img")
            val cover = img?.absUrl("src")?.ifBlank { img.absUrl("data-src") }?.takeIf { it.isNotBlank() }
            NovelSummary(SOURCE_ID, absolute(baseUrl, href), title, cover)
        }

    /** Detail combines the book page (metadata) with the full chapters page. */
    fun parseDetail(bookDoc: Document, tocDoc: Document, baseUrl: String): NovelDetail {
        val title = bookDoc.selectFirst("h1.novel-title")?.text()?.trim().orEmpty()
        val author = bookDoc.selectFirst("div.author a[title]")?.attr("title")?.trim()?.takeIf { it.isNotEmpty() }
            ?: bookDoc.selectFirst("div.author a")?.text()?.trim()?.takeIf { it.isNotEmpty() }
        val cover = bookDoc.selectFirst("meta[property=og:image]")?.attr("content")?.takeIf { it.isNotBlank() }
        val canonical = bookDoc.selectFirst("meta[property=og:url]")?.attr("content")
            ?.replace("http://", "https://")?.takeIf { it.isNotBlank() } ?: baseUrl
        val description = bookDoc.selectFirst("div.summary")?.let { blockText(it) }.orEmpty()
        val status = bookDoc.select("div.header-stats span, div.property-item span")
            .map { it.text().trim() }.firstOrNull { it.uppercase() in STATUSES }
        val chapters = tocDoc.select("ul.chapter-list li a").mapNotNull { a ->
            val href = a.attr("href").ifBlank { return@mapNotNull null }
            val chTitle = (a.selectFirst(".chapter-title")?.text()?.trim()
                ?: a.attr("title").trim().ifBlank { a.text().trim() }).ifBlank { return@mapNotNull null }
            ChapterRef(absolute(baseUrl, href), chTitle)
        }
        return NovelDetail(
            summary = NovelSummary(SOURCE_ID, canonical, title, cover),
            author = author,
            description = description,
            status = status,
            chapters = chapters,
        )
    }

    fun parseChapter(doc: Document): ChapterContent {
        val title = doc.selectFirst("span.chapter-title")?.text()?.trim()
            ?: doc.selectFirst("h1")?.text()?.trim().orEmpty()
        val content = doc.selectFirst("div#content, div.chapter-content")
        val paragraphs = content?.select("p")
            ?.map { it.text().trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        return ChapterContent(title = title.orEmpty(), paragraphs = paragraphs)
    }

    private fun blockText(el: Element): String {
        val paras = el.select("p").map { it.text().trim() }.filter { it.isNotEmpty() }
        return if (paras.isNotEmpty()) paras.joinToString("\n\n") else el.text().trim()
    }

    private fun absolute(baseUrl: String, href: String): String =
        if (href.startsWith("http")) href else baseUrl.trimEnd('/') + "/" + href.trimStart('/')

    private val STATUSES = setOf("ONGOING", "COMPLETED", "HIATUS", "DROPPED", "PAUSED")
}
