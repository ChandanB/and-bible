<!--
  - Copyright (c) 2021 Martin Denham, Tuomas Airaksinen and the And Bible contributors.
  -
  - This file is part of And Bible (http://github.com/AndBible/and-bible).
  -
  - And Bible is free software: you can redistribute it and/or modify it under the
  - terms of the GNU General Public License as published by the Free Software Foundation,
  - either version 3 of the License, or (at your option) any later version.
  -
  - And Bible is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  - without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  - See the GNU General Public License for more details.
  -
  - You should have received a copy of the GNU General Public License along with And Bible.
  - If not, see http://www.gnu.org/licenses/.
  -->

<template>
  <div :id="`doc-${document.id}`" class="bible-document" :data-book-initials="bookInitials">
    <OsisFragment :fragment="document.osisFragments[0]" :show-transition="document.showTransition"/>
  </div>
</template>

<script>
import {inject, provide} from "@vue/runtime-core";
import {useBookmarks} from "@/composables/bookmarks";
import {ref} from "@vue/reactivity";
import OsisFragment from "@/components/documents/OsisFragment";

export default {
  name: "BibleDocument",
  components: {OsisFragment},
  props: {
    document: {type: Object, required: true},
  },
  setup(props) {
    // eslint-disable-next-line no-unused-vars,vue/no-setup-props-destructure
    const {id, type, osisFragments, bookInitials, bookAbbreviation, bookName, key, bookmarks, ordinalRange} = props.document;

    provide("bibleDocumentInfo", {ordinalRange})

    const globalBookmarks = inject("globalBookmarks");
    globalBookmarks.updateBookmarks(...bookmarks);

    const config = inject("config");

      // To remove verse 0 from ordinalRange (is always included)
    const realOrdinalRange = ordinalRange ? [ordinalRange[0]+1, ordinalRange[1]]: null;
    useBookmarks(id, realOrdinalRange, globalBookmarks, bookInitials, ref(true), config);

    return {bookInitials}
  }
}
</script>

<style scoped>

</style>
