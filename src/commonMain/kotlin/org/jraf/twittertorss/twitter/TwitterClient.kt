/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2022-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package org.jraf.twittertorss.twitter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.jraf.twittertorss.util.logd
import org.jraf.twittertorss.util.logw

class TwitterClient(
  private val bearerToken: String,
) {
  private val httpClient by lazy {
    HttpClient {
      install(ContentNegotiation) {
        json(Json {
          ignoreUnknownKeys = true
        })
      }
    }
  }

  suspend fun getPosts(listId: String): List<Post> {
    return try {
      logd("Checking for new posts in list $listId")
      val timelineListResponse: TimelineListResponse =
        httpClient.get("https://api.twitter.com/2/lists/$listId/tweets?tweet.fields=author_id,created_at,referenced_tweets&expansions=author_id&user.fields=id,name,username") {
          bearerAuth(bearerToken)
          accept(ContentType.Application.Json)
        }.body()

      val usersById = timelineListResponse.includes.users.associateBy { it.id }

      timelineListResponse.data.map { status ->
        val user = usersById[status.author_id]!!
        Post(
          id = status.id,
          url = "https://twitter.com/${user.username}/status/${status.id}",
          createdAt = status.created_at,
          isReblogOrReply = status.referenced_tweets != null
        )
      }
    } catch (t: Throwable) {
      logw(t, "Could not retrieve posts")
      throw TwitterClientException(t)
    }
  }
}

class TwitterClientException(cause: Throwable) : Throwable(cause.message, cause)

@Serializable
@Suppress("PropertyName")
private data class TimelineListResponse(
  val data: List<TwitterStatus>,
  val includes: TwitterStatusIncludes,
)

@Serializable
@Suppress("PropertyName")
private data class TwitterStatusIncludes(
  val users: List<TwitterUser>,
)

@Serializable
@Suppress("PropertyName")
private data class TwitterStatus(
  val id: String,
  val created_at: String,
  val author_id: String,
  val referenced_tweets: JsonArray? = null,
)

@Serializable
@Suppress("PropertyName")
private data class TwitterUser(
  val id: String,
  val username: String,
)

data class Post(
  val id: String,
  val url: String,
  val createdAt: String,
  val isReblogOrReply: Boolean,
)
