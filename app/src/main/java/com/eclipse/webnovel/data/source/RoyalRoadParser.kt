package com.eclipse.webnovel.data.source

import com.eclipse.webnovel.data.model.ChapterContent
import com.eclipse.webnovel.data.model.ChapterRef
import com.eclipse.webnovel.data.model.NovelDetail
import com.eclipse.webnovel.data.model.NovelSummary
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Pure Jsoup parsing for RoyalRoad — no Android or network deps, so it is exercised
 * directly by JVM unit tests against recorded HTML fixtures (see RoyalRoadParserTest).
 */
object RoyalRoadParser {

    const val SOURCE_ID = "royalroad"

    fun parseList(doc: Document, baseUrl: String): List<NovelSummary> =
        doc.select("div.fiction-list-item").mapNotNull { item ->
            val link = item.selectFirst(".fiction-title a") ?: return@mapNotNull null
            val href = link.attr("href").ifBlank { return@mapNotNull null }
            val title = link.text().trim().ifBlank { return@mapNotNull null }
            val cover = item.selectFirst("img[data-type=cover], img.img-responsive, img")
                ?.absUrl("src")?.takeIf { it.isNotBlank() }
            val tags = item.select("a.fiction-tag, span.tags a").map { it.text().trim() }
                .filter { it.isNotEmpty() }
            NovelSummary(
                sourceId = SOURCE_ID,
                url = absolute(baseUrl, href),
                title = title,
                coverUrl = cover,
                tags = tags,
            )
        }

    fun parseDetail(doc: Document, baseUrl: String): NovelDetail {
        val title = doc.selectFirst("h1.font-white, div.fic-title h1")?.text()?.trim().orEmpty()
        val author = doc.selectFirst("meta[property=books:author]")?.attr("content")?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: doc.selectFirst("h4 span a[href*=/profile/]")?.text()?.trim()
        val cover = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?.takeIf { it.isNotBlank() }
        val canonical = doc.selectFirst("meta[property=og:url]")?.attr("content")
            ?.takeIf { it.isNotBlank() } ?: baseUrl
        val description = doc.selectFirst("div.description")?.let { blockText(it) }.orEmpty()
        val status = doc.select("span.label, span.font-red-sunglo").map { it.text().trim() }
            .firstOrNull { it.uppercase() in STATUSES }
        val chapters = doc.select("#chapters tr.chapter-row").mapNotNull { row ->
            val a = row.selectFirst("a[href*=/chapter/]") ?: return@mapNotNull null
            val curl = absolute(baseUrl, a.attr("href")).ifBlank { return@mapNotNull null }
            val ctitle = a.text().trim().ifBlank { return@mapNotNull null }
            ChapterRef(url = curl, title = ctitle)
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
        val title = doc.selectFirst("h1.font-white, h1.break-word")?.text()?.trim().orEmpty()
        val content = doc.selectFirst("div.chapter-content")
        val paragraphs = content?.select("p")
            ?.filterNot { it.attr("style").replace(" ", "").contains("display:none") }
            ?.map { it.text().trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        return ChapterContent(title = title, paragraphs = paragraphs)
    }

    private fun blockText(el: Element): String {
        val paras = el.select("p").map { it.text().trim() }.filter { it.isNotEmpty() }
        return if (paras.isNotEmpty()) paras.joinToString("\n\n") else el.text().trim()
    }

    private fun absolute(baseUrl: String, href: String): String =
        if (href.startsWith("http")) href else baseUrl.trimEnd('/') + "/" + href.trimStart('/')

    private val STATUSES = setOf("ONGOING", "COMPLETED", "HIATUS", "STUB", "DROPPED")
}
