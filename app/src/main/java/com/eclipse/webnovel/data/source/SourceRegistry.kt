package com.eclipse.webnovel.data.source

/**
 * The set of available sources. New sources are added here; everything routes a
 * novel/chapter URL back to its source via [sourceFor].
 *
 * Only sources that serve plain HTML are included — Cloudflare-JS-walled sites
 * (ScribbleHub, LightNovelWorld, …) need a headless-WebView fetcher, a later step.
 */
object SourceRegistry {

    val all: List<NovelSource> = listOf(RoyalRoadSource(), NovelFireSource())

    fun sourceFor(url: String): NovelSource =
        all.firstOrNull { url.contains(it.host) } ?: all.first()

    fun byId(id: String): NovelSource? = all.firstOrNull { it.id == id }
}
