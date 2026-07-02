package com.eclipse.webnovel.data.source

import com.eclipse.webnovel.data.model.ChapterContent
import com.eclipse.webnovel.data.model.NovelDetail
import com.eclipse.webnovel.data.model.NovelSummary
import java.net.URLEncoder

/** RoyalRoad — the first proven source (no Cloudflare). */
class RoyalRoadSource : NovelSource {
    override val id: String = RoyalRoadParser.SOURCE_ID
    override val name: String = "RoyalRoad"
    override val baseUrl: String = "https://www.royalroad.com"
    override val host: String = "royalroad.com"

    override suspend fun explore(): List<NovelSummary> =
        RoyalRoadParser.parseList(Http.document("$baseUrl/fictions/rising-stars"), baseUrl)

    override suspend fun search(query: String): List<NovelSummary> {
        val q = URLEncoder.encode(query, "UTF-8")
        return RoyalRoadParser.parseList(Http.document("$baseUrl/fictions/search?title=$q"), baseUrl)
    }

    override suspend fun detail(novelUrl: String): NovelDetail =
        RoyalRoadParser.parseDetail(Http.document(novelUrl), baseUrl)

    override suspend fun chapter(chapterUrl: String): ChapterContent =
        RoyalRoadParser.parseChapter(Http.document(chapterUrl))
}
