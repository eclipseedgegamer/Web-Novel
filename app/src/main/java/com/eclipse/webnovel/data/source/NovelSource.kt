package com.eclipse.webnovel.data.source

import com.eclipse.webnovel.data.model.ChapterContent
import com.eclipse.webnovel.data.model.NovelDetail
import com.eclipse.webnovel.data.model.NovelSummary

/** A pluggable novel site. RoyalRoad is the first; the registry fans out in Phase 3. */
interface NovelSource {
    val id: String
    val name: String
    val baseUrl: String

    /** A browsable list for Explore (e.g. Rising Stars). */
    suspend fun explore(): List<NovelSummary>

    /** Full detail page for a novel. */
    suspend fun detail(novelUrl: String): NovelDetail

    /** Rendered chapter content. */
    suspend fun chapter(chapterUrl: String): ChapterContent
}
