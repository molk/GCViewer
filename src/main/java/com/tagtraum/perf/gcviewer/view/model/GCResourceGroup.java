package com.tagtraum.perf.gcviewer.view.model;

import com.tagtraum.perf.gcviewer.model.GCResource;
import com.tagtraum.perf.gcviewer.model.GcResourceFile;
import com.tagtraum.perf.gcviewer.model.GcResourceSeries;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * <p>Holds a group of resource names (those displayed in the same GCDocument).</p>
 * <p>This class was refactored from "URLSet".</p>
 *
 * @author <a href="mailto:gcviewer@gmx.ch">Joerg Wuethrich</a>
 *         <p>created on: 05.03.2014</p>
 */
public class GCResourceGroup {
    private static final Logger logger = Logger.getLogger(GCResourceGroup.class.getName());
    public static final String RESOURCE_SEPARATOR = ";";
    public static final String SERIES_SEPARATOR = ">";
    private List<String> gcResourceList;

    public GCResourceGroup(List<GCResource> gcResourceList) {
        this.gcResourceList = gcResourceList.stream().map(this::getResourceUrlString).collect(toList());
    }

    /**
     * Initialise a group from a single string consisting of {@link GcResourceFile}s separated by "{@value RESOURCE_SEPARATOR}"
     * and contents of a {@link GcResourceSeries} separated by "&gt;".
     *
     * @param resourceNameGroup resource names separated by ";"
     */
    public GCResourceGroup(String resourceNameGroup) {
        String[] resources = resourceNameGroup.split(RESOURCE_SEPARATOR);
        gcResourceList = getResourceUrlString(resources);
    }

    private static List<String> getResourceUrlString(String[] resources) {
		return stream(resources)
			.map(r -> getResourceUrlString(r))
			.filter(Objects::nonNull)
			.collect(toList());
    }

    private static String getResourceUrlString(String resource) {
        try {
            return resource.startsWith("http") || resource.startsWith("file")
                ? new URL(resource).toString()
                : new File(resource).toURI().toURL().toString();

        }
        catch (MalformedURLException ex) {
            logger.log(Level.WARNING, "Failed to determine URL of " + resource + ". Reason: " + ex.getMessage());
            logger.log(Level.FINER, "Details: ", ex);

            return null;
        }
    }

    private String getResourceUrlString(GCResource gcResource) {
        if (gcResource instanceof GcResourceFile) {
            return ((GcResourceFile) gcResource).getResourceNameAsUrlString();
		}

        if (gcResource instanceof GcResourceSeries) {
			return ((GcResourceSeries) gcResource).getResourcesInOrder().stream()
				.map(GcResourceFile.class::cast)
				.map(GcResourceFile::getResourceNameAsUrlString)
				.collect(Collectors.joining(SERIES_SEPARATOR));
        }

		throw new IllegalArgumentException("Unknown GCResource type!");
    }

    /**
     * Get all resources names as an array of strings.
     *
     * @return resource names as array of strings
     */
    public List<GCResource> getGCResourceList() {
		return gcResourceList.stream()
			.map(GCResourceGroup::getGcResource)
			.collect(toList());
    }

    private static GCResource getGcResource(String entry) {
        return entry.contains(SERIES_SEPARATOR)
            ? getGcResourceSeries(entry)
			: new GcResourceFile(entry);
    }

    private static GCResource getGcResourceSeries(String entry) {
		return new GcResourceSeries(
			stream(entry.split(SERIES_SEPARATOR))
				.map(GcResourceFile::new)
				.collect(toList()));
    }

    /**
     * Get all resource names of the group formatted as URLs separated by a ";"
     *
     * @return single string with all resource names separated by a ";"
     */
    public String getUrlGroupString() {
		return String.join(RESOURCE_SEPARATOR, gcResourceList) + RESOURCE_SEPARATOR;
    }

    /**
     * Get short version of resource names.<br>
     * If more than one resource is in this group, returns only file name without path.
     * {@link GcResourceSeries} are abbreviated by showing the name of the first file and how many files are following.
     *
     * @return get short group name (only file name without path), if there is more than one
     * resource
     */
    public String getGroupStringShort() {
        if (gcResourceList.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (String resourceName : gcResourceList) {
                resourceName = shortenGroupStringForSeries(resourceName);
                // test for "/" and "\\" because in Windows you have a "/" in a http url
                // but "\\" in file strings
                int lastIndexOfPathSeparator = resourceName.lastIndexOf("/");
                if (lastIndexOfPathSeparator < 0) {
                    lastIndexOfPathSeparator = resourceName.lastIndexOf("\\");
                }
                sb.append(resourceName.substring(lastIndexOfPathSeparator + 1)).append(";");
            }
            return sb.toString();
        }
        else {
            String resourceName = gcResourceList.get(0);
            return shortenGroupStringForSeries(resourceName);
        }
    }

    private String shortenGroupStringForSeries(String resourceName) {
        String[] splitBySeriesSeparator = resourceName.split(SERIES_SEPARATOR);
        if(splitBySeriesSeparator.length > 1)
        {
            // Series: Shorten description by showing first entry only + number of remaining files
            resourceName = splitBySeriesSeparator[0] + " (series, " + (splitBySeriesSeparator.length -1) +" more files)";
        }
        return resourceName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GCResourceGroup other = (GCResourceGroup) obj;
        if (gcResourceList == null) {
            if (other.gcResourceList != null)
                return false;
        }
        else if (!gcResourceList.equals(other.gcResourceList))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((gcResourceList == null) ? 0 : gcResourceList.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "RecentGCResourceGroup [gcResourceList=" + gcResourceList + "]";
    }
}
