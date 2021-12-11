/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2021 Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jraf.twittertorss

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.withCharset
import io.ktor.request.host
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml

private const val DEFAULT_PORT = 8042

private const val ENV_PORT = "PORT"

private const val PATH_LIST_ID = "listId"

private const val PARAM_OAUTH_CONSUMER_KEY = "oAuthConsumerKey"
private const val PARAM_OAUTH_CONSUMER_SECRET = "oAuthConsumerSecret"
private const val PARAM_OAUTH_ACCESS_TOKEN = "oAuthAccessToken"
private const val PARAM_OAUTH_ACCESS_TOKEN_SECRET = "oAuthAccessTokenSecret"

private const val APP_URL = "https://bod-twitter-to-rss.herokuapp.com"

private val PUB_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss 'Z'", Locale.US)

fun main() {
    val listenPort = System.getenv(ENV_PORT)?.toInt() ?: DEFAULT_PORT
    embeddedServer(Netty, listenPort) {
        install(DefaultHeaders)

        install(StatusPages) {
            status(HttpStatusCode.NotFound) {
                call.respondText(
                    text = "Usage: $APP_URL/<$PATH_LIST_ID>",
                    status = it
                )
            }

            exception<IllegalArgumentException> { exception ->
                call.respond(HttpStatusCode.BadRequest, exception.message ?: "Bad request")
            }
            exception<TwitterClientException> { exception ->
                call.respond(
                    HttpStatusCode.BadRequest, exception.message ?: "Could not retrieve the list's tweets"
                )
            }
        }

        routing {
            get("{$PATH_LIST_ID}") {
                val listId = call.parameters[PATH_LIST_ID]?.toLongOrNull() ?: throw IllegalArgumentException("Invalid list ID")

                val oAuthConsumerKey = call.request.queryParameters[PARAM_OAUTH_CONSUMER_KEY] ?: throw IllegalArgumentException("Missing oAuthConsumerKey")
                val oAuthConsumerSecret =
                    call.request.queryParameters[PARAM_OAUTH_CONSUMER_SECRET] ?: throw IllegalArgumentException("Missing oAuthConsumerSecret")
                val oAuthAccessToken = call.request.queryParameters[PARAM_OAUTH_ACCESS_TOKEN] ?: throw IllegalArgumentException("Missing oAuthAccessToken")
                val oAuthAccessTokenSecret =
                    call.request.queryParameters[PARAM_OAUTH_ACCESS_TOKEN_SECRET] ?: throw IllegalArgumentException("Missing oAuthAccessTokenSecret")

                val selfLink = URLBuilder("${call.request.origin.scheme}://${call.request.host()}${call.request.uri}").apply {
                    parameters.append(PARAM_OAUTH_CONSUMER_KEY, oAuthConsumerKey)
                    parameters.append(PARAM_OAUTH_CONSUMER_SECRET, oAuthConsumerSecret)
                    parameters.append(PARAM_OAUTH_ACCESS_TOKEN, oAuthAccessToken)
                    parameters.append(PARAM_OAUTH_ACCESS_TOKEN_SECRET, oAuthAccessTokenSecret)
                }.buildString()
                call.respondText(
                    getRss(
                        selfLink = selfLink,
                        oAuthConsumerKey = oAuthConsumerKey,
                        oAuthConsumerSecret = oAuthConsumerSecret,
                        oAuthAccessToken = oAuthAccessToken,
                        oAuthAccessTokenSecret = oAuthAccessTokenSecret,
                        listId = listId,
                    ),
                    ContentType.Application.Rss.withCharset(Charsets.UTF_8))
            }
        }
    }.start(wait = true)
}

private fun getRss(
    selfLink: String,
    oAuthConsumerKey: String,
    oAuthConsumerSecret: String,
    oAuthAccessToken: String,
    oAuthAccessTokenSecret: String,
    listId: Long,
): String {
    val twitterClient = TwitterClient(
        oAuthConsumerKey = oAuthConsumerKey,
        oAuthConsumerSecret = oAuthConsumerSecret,
        oAuthAccessToken = oAuthAccessToken,
        oAuthAccessTokenSecret = oAuthAccessTokenSecret
    )
    val tweets = twitterClient.getTweets(listId)

    return xml("rss") {
        includeXmlProlog = true
        attribute("version", "2.0")
        "channel" {
            "title" { -"Tweets for list $listId" }
            "description" { -"Tweets for list $listId" }
            "link" { -selfLink }
            "ttl" { -"60" }
            for (tweet in tweets) {
                "item" {
                    "link" { -tweet.url }
                    "guid" {
                        attribute("isPermaLink", "true")
                        -tweet.url
                    }
                    "pubDate" { -formatPubDate(tweet.createdAt) }
                }
            }
        }
    }.toString(PrintOptions(singleLineTextElements = true, indent = "  "))
}

private fun formatPubDate(date: Date): String = PUB_DATE_FORMAT.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("GMT")))
