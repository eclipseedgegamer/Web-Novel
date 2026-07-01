package com.eclipse.webnovel.data.source

import com.eclipse.webnovel.data.model.ChapterContent
import com.eclipse.webnovel.data.model.NovelDetail
import com.eclipse.webnovel.data.model.NovelSummary

/** RoyalRoad — the first proven source. */
class RoyalRoadSource : NovelSource {
    override val id: String = RoyalRoadParser.SOURCE_ID
    override val name: String = "RoyalRoad"
    override val baseUrl: String = "https://www.royalroad.com"

    override suspend fun explore(): List<NovelSummary> =
        RoyalRoadParser.parseList(Http.document("$baseUrl/fictions/rising-stars"), baseUrl)

    override suspend fun detail(novelUrl: String): NovelDetail =
        RoyalRoadParser.parseDetail(Http.document(novelUrl), baseUrl)

    override suspend fun chapter(chapterUrl: String): ChapterContent =
        RoyalRoadParser.parseChapter(Http.document(chapterUrl))
}
