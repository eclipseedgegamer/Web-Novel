package com.eclipse.webnovel.data.model

/** A novel as shown in lists (Explore/Search). */
data class NovelSummary(
    val sourceId: String,
    val url: String,
    val title: String,
    val coverUrl: String? = null,
    val tags: List<String> = emptyList(),
)

/** Full novel page: summary + synopsis + chapter list. */
data class NovelDetail(
    val summary: NovelSummary,
    val author: String?,
    val description: String,
    val status: String?,
    val chapters: List<ChapterRef>,
)

/** A chapter entry in a novel's table of contents. */
data class ChapterRef(
    val url: String,
    val title: String,
)

/** Rendered chapter: title + clean paragraphs (paragraph index is the reader's progress anchor). */
data class ChapterContent(
    val title: String,
    val paragraphs: List<String>,
)
