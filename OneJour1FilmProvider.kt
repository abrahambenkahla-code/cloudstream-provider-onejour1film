// OneJour1FilmProvider.kt
// CloudStream provider (metadata-only) for https://1jour1film1125.site/

package com.example.cloudstream.providers.onejour1film

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.networking.Http
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class OneJour1FilmProvider : MainAPI() {
    override val mainUrl = "https://1jour1film1125.site"
    override val name = "1Jour1Film (metadata only)"
    override val lang = "fr"
    override val hasMainPage = true

    private suspend fun fetchPage(url: String): Document {
        val html = Http.get(url).text
        return Jsoup.parse(html, url)
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) mainUrl else "$mainUrl/page/$page"
        val doc = fetchPage(url)
        val items = ArrayList<SearchResponse>()
        val cards = doc.select("article, .post, .movie-card")
        for (c in cards) {
            val a = c.selectFirst("a[href]") ?: continue
            val link = a.absUrl("href")
            val title = c.selectFirst("h2, .title")?.text()?.trim() ?: a.text().trim()
            val img = c.selectFirst("img")?.absUrl("src")
            val sr = SearchResponse()
            sr.name = title
            sr.url = link
            sr.posterUrl = img
            items.add(sr)
        }
        return HomePageResponse(items, items.isNotEmpty())
    }

    override suspend fun search(query: String, filters: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/?s=${java.net.URLEncoder.encode(query, "utf-8")}"
        val doc = fetchPage(searchUrl)
        val results = ArrayList<SearchResponse>()
        val cards = doc.select("article, .post, .result, .movie-card")
        for (c in cards) {
            val a = c.selectFirst("a[href]") ?: continue
            val link = a.absUrl("href")
            val title = c.selectFirst("h2, .title, .entry-title")?.text()?.trim() ?: a.text().trim()
            val img = c.selectFirst("img")?.absUrl("src")
            val sr = SearchResponse()
            sr.name = title
            sr.url = link
            sr.posterUrl = img
            results.add(sr)
        }
        return results
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = fetchPage(url)
        val title = doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?: doc.selectFirst("h1.entry-title, h1.post-title, h1")?.text()?.trim()
            ?: "Unknown title"
        val description = doc.selectFirst("meta[name=description]")?.attr("content")
            ?: doc.selectFirst("meta[property=og:description]")?.attr("content")
            ?: doc.selectFirst(".summary, .desc, .post-content")?.text()?.trim()
        val poster = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: doc.selectFirst("img.wp-post-image, .post-thumbnail img")?.absUrl("src")
        val yearText = doc.selectFirst(".year, .meta-year")?.text()
        val tags = ArrayList<String>()
        doc.select(".tags a, .genres a, .post-tags a").forEach { t ->
            val txt = t.text().trim()
            if (txt.isNotBlank()) tags.add(txt)
        }
        val load = LoadResponse()
        load.name = title
        load.url = url
        load.posterUrl = poster
        load.backgroundUrl = poster
        load.description = description
        load.year = yearText
        load.tags = tags.distinct()
        load.videos = emptyList()
        return load
    }

    override suspend fun fetchVideoLinks(url: String): List<Video> {
        return emptyList()
    }
}
