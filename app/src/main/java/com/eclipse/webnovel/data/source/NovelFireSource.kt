package com.eclipse.webnovel.data.source

import com.eclipse.webnovel.data.model.ChapterContent
import com.eclipse.webnovel.data.model.NovelDetail
import com.eclipse.webnovel.data.model.NovelSummary
import java.net.URLEncoder

/** NovelFire — second source. Detail combines the book page with its /chapters TOC. */
class NovelFireSource : NovelSource {
    override val id: String = NovelFireParser.SOURCE_ID
    override val name: String = "NovelFire"
    override val baseUrl: String = "https://novelfire.net"
    override val host: String = "novelfire.net"

    override suspend fun explore(): List<NovelSummary> =
        NovelFireParser.parseList(Http.document("$baseUrl/genre-all/sort-popular/status-all/all-novel"), baseUrl)

    override suspend fun search(query: String): List<NovelSummary> {
        val q = URLEncoder.encode(query, "UTF-8")
        return NovelFireParser.parseList(Http.document("$baseUrl/search?keyword=$q"), baseUrl)
    }

    override suspend fun detail(novelUrl: String): NovelDetail {
        val bookDoc = Http.document(novelUrl)
        val tocDoc = Http.document(novelUrl.trimEnd('/') + "/chapters")
        return NovelFireParser.parseDetail(bookDoc, tocDoc, baseUrl)
    }

    override suspend fun chapter(chapterUrl: String): ChapterContent =
        NovelFireParser.parseChapter(Http.document(chapterUrl))
}
