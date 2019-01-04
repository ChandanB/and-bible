/*
 * Copyright (c) 2018 Martin Denham, Tuomas Airaksinen and the And Bible contributors.
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

package net.bible.android.view.activity.base

import android.content.res.Configuration
import android.util.Log
import android.view.Menu

import net.bible.android.control.page.PageControl
import net.bible.android.view.activity.base.actionbar.ActionBarManager
import net.bible.android.view.activity.base.actionbar.DefaultActionBarManager

import javax.inject.Inject
import android.view.animation.DecelerateInterpolator
import kotlinx.android.synthetic.main.main_bible_view.*


/**
 * Base class for activities with a custom title bar
 *
 * @author Martin Denham [mjdenham at gmail dot com]
 */
abstract class CustomTitlebarActivityBase(private val optionsMenuId: Int = NO_OPTIONS_MENU) : ActivityBase() {

    private var actionBarManager: ActionBarManager = DefaultActionBarManager()

    @Inject lateinit var pageControl: PageControl

    /**
     * load the default menu items from xml config
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (optionsMenuId != NO_OPTIONS_MENU) {
            // Inflate the menu
            menuInflater.inflate(optionsMenuId, menu)
        }

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Allow some menu items to be hidden or otherwise altered
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        actionBarManager.prepareOptionsMenu(this, menu, supportActionBar)

        // must return true for menu to be displayed
        return true
    }

    /**
     * Hide/show the actionbar and call base class to hide/show everything else
     */
    override fun toggleFullScreen() {
        super.toggleFullScreen()

        if (!isFullScreen) {
            Log.d(TAG, "Fullscreen off")
            toolbar.translationY = -toolbar.bottom.toFloat()
            supportActionBar?.show()
            toolbar.animate().translationY(0.0F).setInterpolator(DecelerateInterpolator()).start()

        } else {
            Log.d(TAG, "Fullscreen on")
            supportActionBar?.hide()
        }

        getContentView().requestLayout()
    }

    /**
     * Called whenever something like strong preferences have been changed by the user.  Should refresh the screen
     */
    protected open fun preferenceSettingsChanged() {}

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // the title bar has different widths depending on the orientation
        updateActionBarButtons()
    }

    /** update the quick links in the title bar
     */
    open fun updateActionBarButtons() {
        actionBarManager.updateButtons()
    }

    protected fun setActionBarManager(actionBarManager: ActionBarManager) {
        this.actionBarManager = actionBarManager
    }

    companion object {

        protected const val NO_OPTIONS_MENU = 0

        private const val TAG = "CTActivityBase"
    }
}