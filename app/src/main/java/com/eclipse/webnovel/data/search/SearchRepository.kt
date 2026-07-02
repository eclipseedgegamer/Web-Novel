package com.eclipse.webnovel.data.search

import com.eclipse.webnovel.data.source.NovelSource
import com.eclipse.webnovel.data.source.SourceHealth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** One source's copy of a novel. */
data class SourceHit(val sourceId: String, val sourceName: String, val url: String)

/** A novel deduped across sources; [hits] holds every source that has it. */
data class DedupedNovel(val title: String, val coverUrl: String?, val hits: List<SourceHit>)

/** Parallel multi-source search with title-based dedup and relevance ranking. */
class SearchRepository {

    suspend fun search(query: String, sources: List<NovelSource>): List<DedupedNovel> = coroutineScope {
        val hits = sources.map { source ->
            async(Dispatchers.IO) {
                val result = runCatching { source.search(query) }
                SourceHealth.record(source.id, result.isSuccess)
                result.getOrDefault(emptyList()).map { summary -> source to summary }
            }
        }.awaitAll().flatten()

        hits.groupBy { normalize(it.second.title) }
            .values
            .map { group ->
                val first = group.first().second
                DedupedNovel(
                    title = first.title,
                    coverUrl = group.firstNotNullOfOrNull { it.second.coverUrl },
                    hits = group.map { (source, summary) ->
                        SourceHit(source.id, source.name, summary.url)
                    },
                )
            }
            .sortedByDescending { score(it.title, query) }
    }

    private fun normalize(text: String): String =
        text.lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun score(title: String, query: String): Int {
        val t = normalize(title)
        val q = normalize(query)
        return when {
            q.isEmpty() -> 0
            t == q -> 3
            t.startsWith(q) -> 2
            t.contains(q) -> 1
            else -> 0
        }
    }
}
