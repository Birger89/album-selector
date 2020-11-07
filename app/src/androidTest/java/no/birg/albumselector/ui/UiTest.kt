package no.birg.albumselector.ui

import android.provider.Settings.Global.*
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import no.birg.albumselector.R
import no.birg.albumselector.TestMainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class UiTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<TestMainActivity>
            = ActivityScenarioRule(TestMainActivity::class.java)


    init {
        setAnimations(false)
    }

    @After
    fun after() {
        setAnimations(true)
    }

    private fun setAnimations(enabled: Boolean) {
        val value = if (enabled) "1.0" else "0.0"

        InstrumentationRegistry.getInstrumentation().uiAutomation.run {
            this.executeShellCommand("settings put global $WINDOW_ANIMATION_SCALE $value")
            this.executeShellCommand("settings put global $TRANSITION_ANIMATION_SCALE $value")
            this.executeShellCommand("settings put global $ANIMATOR_DURATION_SCALE $value")
        }
    }


    @Test
    fun addAlbum_AlbumShouldBeViewable() {
        /** Add an album **/
        onView(withId(R.id.search_button)).perform(click())
        onView(withId(R.id.search_field)).perform(typeText("text"))
        onView(withId(R.id.search_button)).perform(click())
        onView(withId(R.id.add_button)).perform(click())

        /** Go to album **/
        onView(withId(R.id.home_button)).perform(click())
        onView(withId(R.id.album_cover)).perform(click())

        onView(withId(R.id.album_title)).check(matches(isDisplayed()))
    }

    @Test
    fun addAlbumFromDetailView_AlbumShouldBeInLibrary() {
        /** Add album **/
        onView(withId(R.id.search_button)).perform(click())
        onView(withId(R.id.search_field)).perform(typeText("test"))
        onView(withId(R.id.search_button)).perform(click())
        onView(withId(R.id.album_title)).perform(click())
        onView(withId(R.id.add_button)).perform(click())

        /** Go back to library **/
        onView(isRoot()).perform(pressBack())
        onView(withId(R.id.library_button)).perform(click())

        onView(withId(R.id.album_title)).check(matches(isDisplayed()))
    }
}
