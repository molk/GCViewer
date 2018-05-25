package com.tagtraum.perf.gcviewer.view.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link GCPreferences} class.
 */
public class GCPreferencesTest {

    @Test
    public void test_recent_files() {
		final GCPreferences preferences = new GCPreferences();
		final List<String> files = Arrays.asList("file1", "file2");

		preferences.setRecentFiles(files);

        assertThat(preferences.getRecentFiles(), containsInAnyOrder(files.toArray()));
    }

}
