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

import java.util.Date
import org.slf4j.LoggerFactory
import twitter4j.Paging
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

private var LOGGER = LoggerFactory.getLogger(TwitterClient::class.java)

class TwitterClient(
    oAuthConsumerKey: String,
    oAuthConsumerSecret: String,
    oAuthAccessToken: String,
    oAuthAccessTokenSecret: String,
) {
    private val twitter: Twitter = TwitterFactory(
        ConfigurationBuilder()
            .setDebugEnabled(true)
            .setOAuthConsumerKey(oAuthConsumerKey)
            .setOAuthConsumerSecret(oAuthConsumerSecret)
            .setOAuthAccessToken(oAuthAccessToken)
            .setOAuthAccessTokenSecret(oAuthAccessTokenSecret)
            .setTweetModeExtended(true)
            .build()
    ).instance

    @Throws(TwitterClientException::class)
    fun getTweets(
        listId: Long,
    ): List<Tweet> {
        return try {
            LOGGER.debug("Checking for new tweets in list $listId")
            val statusList = twitter.getUserListStatuses(
                listId,
                Paging(1, 50)
            )
                // Exclude retweets
                .filterNot { it.isRetweet }
                .map { status ->
                    Tweet(
                        id = status.id,
                        url = "https://twitter.com/${status.user.screenName}/status/${status.id}",
                        text = status.text,
                        createdAt = status.createdAt,
                        userName = status.user.screenName,
                    )
                }

            if (statusList.isEmpty()) {
                LOGGER.debug("No tweets")
            }
            statusList
        } catch (e: Exception) {
            LOGGER.warn("Could not retrieve tweets", e)
            throw TwitterClientException(e)
        }
    }
}

class TwitterClientException(cause: Exception) : Throwable(cause.message, cause)

data class Tweet(
    val id: Long,
    val url: String,
    val text: String,
    val createdAt: Date,
    val userName: String,
)