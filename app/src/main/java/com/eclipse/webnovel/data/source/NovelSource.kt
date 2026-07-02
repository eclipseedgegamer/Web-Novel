package com.eclipse.webnovel.data.source

import com.eclipse.webnovel.data.model.ChapterContent
import com.eclipse.webnovel.data.model.NovelDetail
import com.eclipse.webnovel.data.model.NovelSummary

/** A pluggable novel site. Sources are resolved by URL host via [SourceRegistry]. */
interface NovelSource {
    val id: String
    val name: String
    val baseUrl: String

    /** URL host fragment used to route a novel/chapter URL back to this source. */
    val host: String

    /** A browsable list for Explore. */
    suspend fun explore(): List<NovelSummary>

    /** Search this source for [query]. */
    suspend fun search(query: String): List<NovelSummary>

    /** Full detail page for a novel. */
    suspend fun detail(novelUrl: String): NovelDetail

    /** Rendered chapter content. */
    suspend fun chapter(chapterUrl: String): ChapterContent
}
