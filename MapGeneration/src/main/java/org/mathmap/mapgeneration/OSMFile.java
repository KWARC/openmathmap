/**
 * Copyright (c) 2013 KWARC Group <kwarc.info>
 *
 * This file is part of OpenMathMap.
 *
 * OpenMathMap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMathMap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMathMap. If not, see <http://www.gnu.org/licenses/>.
 */

package org.mathmap.mapgeneration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * OSMFile is a class providing a simple API to write a *.osm XML file.
 * It provides methods to write different kind of nodes and ways.
 */
public class OSMFile {
    private PrintWriter pw; /** PrintWriter responsible for writing the file */
    private int resolution; /** resolution of the map */

    /**
     * Constructor setting resolution and initializing pw.
     * Also it calls the printHeader() function.
     * @param osmFile    File representing the file to be written to.
     * @param resolution resolution of the map
     * @throws FileNotFoundException thrown in case osmFile could not be found
     */
    public OSMFile(File osmFile, int resolution) throws FileNotFoundException {
        this.resolution = resolution;
        pw = new PrintWriter(osmFile);
        printHeader();
    }

    /**
     * Prints the header of the XML.
     */
    public void printHeader() {
        pw.println("<?xml version='1.0' encoding='UTF-8'?>");
        pw.println("<osm version='0.6' generator='kwarc.info'>");
    }

    /**
     * A grid node is defined by its position on the resolution sized grid.
     * The id is generated by computing lat and long in base 2 * resolution.
     * 2 * resolution is necessary in order to allow for non-integer values
     * for latitude and longitude (we support n/2, n in 0...resolution).
     * @param latitude  latitude
     * @param longitude longitude
     * @return computed id
     */
    public int getGridNodeID(double latitude, double longitude) {
        return 2 * resolution * (int)(2.0 * latitude) + (int)(2.0 * longitude);
    }

    /**
     * Adds a grid node at the given latitude and longitude position.
     * @param latitude  latitude of the node
     * @param longitude longitude of the node
     */
    public void addGridNode(double latitude, double longitude) {
        pw.printf("<node id='%d' lat='%.7f' lon='%.7f' visible='true' version='1' />\n",
                getGridNodeID(latitude, longitude), latitude / resolution, longitude / resolution);
    }

    /**
     * Adds a "special" node, which in addition to LatLong also has a name whose hashCode serves as the id.
     * Note how the node tag is not closed in this case, which allows for additional tags but also requires
     * a call to endSpecialNode() when done.
     * @param latitude  latitude of the node
     * @param longitude longitude of the node
     * @param name      name of the node
     */
    public void addSpecialNode(double latitude, double longitude, String name) {
        pw.printf("<node id='%d' lat='%.7f' lon='%.7f' visible='true' version='1'>",
                name.hashCode(), latitude, longitude);
    }

    /**
     * Adds a tag to either a special node or a way.
     * @param key   key of the tag
     * @param value value of the tag
     */
    public void addTag(String key, String value) {
        pw.printf("<tag k='%s' v='%s' />\n", key, value);
    }

    /**
     * Ends a special node by writing the node closing tag.
     */
    public void endSpecialNode() {
        pw.println("</node>");
    }

    /**
     * Adds a way, very similar to specialNode it uses the hashCode of the name as the id.
     * @param name name of the way
     */
    public void addWay(String name) {
        pw.printf("<way id='%d' action='modify' visible='true' version='1'>\n", name.hashCode());
    }

    /**
     * Adds a reference to a grid node. It requires the node to be added previously through
     * addGridNode().
     * @param latitude  latitude of the referenced grid node
     * @param longitude longitude of the referenced grid node
     */
    public void addGridNodeReference(double latitude, double longitude) {
        pw.printf("<nd ref='%d' />\n", getGridNodeID(latitude, longitude));
    }

    /**
     * Adds a reference to a special node. It requires the node to be added previously through
     * addSpecialNode().
     * @param name name of the special node
     */
    public void addSpecialNodeReference(String name) {
        pw.printf("<nd ref='%d' />\n", name.hashCode());
    }

    /**
     * Ends a way by writing the way closing tag.
     */
    public void endWay() {
        pw.println("</way>");
    }

    /**
     * Closes the file by writing the osm closing tag and closing pw.
     */
    public void close() {
        pw.println("</osm>");
        pw.close();
    }
}