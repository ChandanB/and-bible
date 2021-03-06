/*
 * Copyright (c) 2021 Martin Denham, Tuomas Airaksinen and the And Bible contributors.
 *
 * This file is part of And Bible (http://github.com/AndBible/and-bible).
 *
 * And Bible is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * And Bible is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with And Bible.
 * If not, see http://www.gnu.org/licenses/.
 *
 */

package net.bible.android.control.page

import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import net.bible.android.control.bookmark.LABEL_UNLABELED_ID
import net.bible.android.control.versification.toV11n
import net.bible.android.database.bookmarks.BookmarkEntities
import net.bible.android.database.json
import net.bible.service.sword.BookAndKey
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.FeatureType
import org.crosswire.jsword.book.sword.SwordBook
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.VerseRange
import org.crosswire.jsword.versification.Versification
import java.util.UUID.randomUUID

/*
 * Serializable classes and utils that are used when transferring stuff to JS side
 */


// Unique identifier that can be used as ID in DOM
val Key.uniqueId: String get() {
    return if (this is VerseRange) {
        "ordinal-${start.ordinal}-${end.ordinal}"
    } else {
        this.osisID.replace(".", "-")
    }
}

fun mapToJson(map: Map<String, String>) =
    map.map {(key, value) -> "'$key': $value"}
       .joinToString(",", "{", "}")

fun listToJson(list: List<String>) = list.joinToString(",", "[", "]")
fun wrapString(str: String): String = "\"$str\""

interface Document {
    val asJson: String get() {
        return asHashMap.map {(key, value) -> "'$key': $value"}.joinToString(",", "{", "}")
    }
    val asHashMap: Map<String, Any>
}

class ErrorDocument(private val errorMessage: String?): Document {
    override val asHashMap: Map<String, String> get() =
        mapOf(
            "id" to wrapString(randomUUID().toString()),
            "type" to wrapString("error"),
            "errorMessage" to wrapString(errorMessage?:"")
        )
}


open class OsisDocument(
    val osisFragments: List<OsisFragment>,
    val book: Book,
    val key: Key,
): Document {
    override val asHashMap: Map<String, String> get () {
        return mapOf(
            "id" to wrapString("${book.initials}-${key.uniqueId}"),
            "type" to wrapString("osis"),
            "osisFragments" to listToJson(osisFragments.map { mapToJson(it.toHashMap) }),
            "bookInitials" to wrapString(book.initials),
            "bookAbbreviation" to wrapString(book.abbreviation),
            "bookName" to wrapString(book.name),
            "key" to wrapString(key.uniqueId),
        )
    }
}

class BibleDocument(
    val bookmarks: List<BookmarkEntities.Bookmark>,
    val verseRange: VerseRange,
    osisFragments: List<OsisFragment>,
    val swordBook: SwordBook
): OsisDocument(osisFragments, swordBook, verseRange) {
    override val asHashMap: Map<String, String> get () {
        val bookmarks = bookmarks.map { ClientBookmark(it, it.labelIds!!, swordBook.versification) }
        val vrInV11n = verseRange.toV11n(swordBook.versification)
        return super.asHashMap.toMutableMap().apply {
            put("bookmarks", json.encodeToString(serializer(), bookmarks))
            put("type", wrapString("bible"))
            put("ordinalRange", json.encodeToString(serializer(), listOf(vrInV11n.start.ordinal, vrInV11n.end.ordinal)))
        }
    }
}

class OsisFragment(
    val xml: String,
    val key: Key?,
    private val bookId: String
) {
    private val keyStr: String get () = "$bookId--${key?.uniqueId ?: "error-${randomUUID()}"} }"
    val features: Map<String, String> get () =
        if(key is BookAndKey) {
            val type = when {
                key.document.hasFeature(FeatureType.HEBREW_DEFINITIONS) -> "hebrew"
                key.document.hasFeature(FeatureType.GREEK_DEFINITIONS) -> "greek"
                else -> null
            }
            if (type != null) {
                hashMapOf("type" to type, "keyName" to key.key.name)
            } else emptyMap()
        } else emptyMap()

    val toHashMap: Map<String, String> get() {
        val ordinalRangeStr = json.encodeToString(
            serializer(),
            if(key is VerseRange) listOf(key.start.ordinal, key.end.ordinal) else null
        )
        return mapOf(
            "xml" to "`${xml.replace("`", "\\`")}`",
            "key" to wrapString(keyStr),
            "features" to json.encodeToString(serializer(), features),
            "ordinalRange" to ordinalRangeStr
        )
    }
}

@Serializable
data class ClientBookmark(val id: Long,
                          val ordinalRange: List<Int>,
                          val offsetRange: List<Int>?,
                          val labels: List<Long>,
                          val bookInitials: String?,
                          val bookAbbreviation: String?,
                          val bookName: String?,
                          val createdAt: Long,
                          val lastUpdatedOn: Long,
                          val notes: String?
) {
    constructor(bookmark: BookmarkEntities.Bookmark, labels: List<Long>, v11n: Versification) :
        this(id = bookmark.id,
            ordinalRange = listOf(bookmark.verseRange.toV11n(v11n).start.ordinal, bookmark.verseRange.toV11n(v11n).end.ordinal),
            offsetRange = bookmark.textRange?.clientList,
            labels = labels.toMutableList().also {
                if(it.isEmpty()) it.add(LABEL_UNLABELED_ID)
            },
            bookInitials = bookmark.book?.initials,
            bookName = bookmark.book?.name,
            bookAbbreviation = bookmark.book?.abbreviation,
            createdAt = bookmark.createdAt.time,
            lastUpdatedOn = bookmark.lastUpdatedOn.time,
            notes = bookmark.notes
        )
}

@Serializable
data class ClientBookmarkStyle(val color: Int)

@Serializable
data class ClientBookmarkLabel(val id: Long, val name: String, val style: ClientBookmarkStyle)

