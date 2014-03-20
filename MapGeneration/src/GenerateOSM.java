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

import java.io.*;
import java.util.*;

public class GenerateOSM {
    private MSC nearestMSC[][];
    private List<MSC> topLevelMSCs;
    private List<MSC> midLevelMSCs;
    private Map<String,String> desc;
    private Map<String, MSC> str2msc;

    private Map<Integer, MSC> int2topMSC;
    private Map<MSC, Integer> topMSC2int;

    private int data[][];

    private int resolution;

    private File plotData;
    private File cityData;
    private File descFile;

    private double dataMin = Double.MAX_VALUE;
    private double dataMax = Double.MIN_VALUE;
    private double areaMax = Double.MIN_VALUE;

    private double currMax[][];

    private int[][] graph;
    private int[] degrees;

    private int numColored;
    private int[] coloring;

    private int[] rusinColoring;


    /**
     * matrix storing a label for each "pixel" of the output image. This will be used later in order to decide if
     * a given water region is a lake inside the continent or if it belongs to the surrounding ocean.
     */
    private int[][] labels;

    /** map which remembers which coordinates belong to a given MSC */
    private Map<MSC, HashSet<Coords2D>> msc2coords = new HashMap<MSC, HashSet<Coords2D>>();

    /** set keeping track of all the coordinates making up the coastline */
    private Set<Coords2D> coastLine = new HashSet<Coords2D>();

    /** map which remembers the nearest MSC for a given coastLine coordinate */
    private Map<Coords2D, MSC> coords2MSC = new HashMap<Coords2D, MSC>();

    /** this will write the actual osm map. */
    private OSMFile osmFile;


    /**
     * Constructor
     * @param plotData     				PlotData file
     * @param cityData					cityData file
     * @param descFile					MSC description file, expects format as seen in
     *                     				MapData/Desc_msc2010-final.txt
     * @param resolution 				resolution of the map
     * @throws FileNotFoundException	thrown if one of the files could not be found
     */
    public GenerateOSM(File plotData, File cityData, File descFile, int resolution) throws IOException {
        this.plotData = plotData;
        this.cityData = cityData;
        this.descFile = descFile;
        this.resolution = resolution;

        labels = new int[resolution][resolution];

        BufferedReader br;

        desc = new HashMap<String, String>();

        int2topMSC = new HashMap<Integer, MSC>();
        topMSC2int = new HashMap<MSC, Integer>();

        br = new BufferedReader(new FileReader(descFile));
        String line = br.readLine();
        while (line != null) {
            String[] tokens = line.split("\\*\\*\\*");
            desc.put("MSC" + tokens[0], tokens[1]);

            line = br.readLine();
        }

        br.close();

        str2msc = new HashMap<String, MSC>();
        br = new BufferedReader(new FileReader(plotData));
        topLevelMSCs = new ArrayList<MSC>();
        line = br.readLine();

        int topMSCidx = 0;
        while (line != null) {
            String[] tokens = line.split(",");
            String name = tokens[0];
            String description = desc.get(name);
            double x = toImageSpace(Double.parseDouble(tokens[1]));
            double y = toImageSpace(Double.parseDouble(tokens[2]));
            double area = Double.parseDouble(tokens[3]);

            MSC currMSC = new MSC(name, description, x, y, area);
            str2msc.put(name, currMSC);
            topLevelMSCs.add(currMSC);

            int2topMSC.put(topMSCidx, currMSC);
            topMSC2int.put(currMSC, topMSCidx++);

            areaMax = Math.max(areaMax, area);
            line = br.readLine();
        }

        br.close();

        for (MSC msc: topLevelMSCs) {
            double newArea = (msc.getArea() / areaMax * resolution * resolution / topLevelMSCs.size() * 0.25);
            msc.setArea(newArea);
            str2msc.get(msc.getName()).setArea(newArea);
        }

        midLevelMSCs = new ArrayList<MSC>();

        File dataDir = plotData.getParentFile();
        for (File file: dataDir.listFiles()) {
            if (file.isDirectory() && file.getName().startsWith("MSC")) {
                File thisPlotData = new File(file + "/PlotData.txt");
                if (!thisPlotData.exists()) {
                    continue;
                }
                br = new BufferedReader(new FileReader(file + "/PlotData.txt"));
                MSC parentMSC = str2msc.get(file.getName());

                line = br.readLine();
                double areaSum = 0.0;
                List<MSC> currMidLevelMSCs = new ArrayList<MSC>();

                while (line != null) {
                    String[] tokens = line.split(",");
                    String name = tokens[0];
                    String description = desc.get(name);

                    double parentRadius = Math.sqrt(parentMSC.getArea());
                    double sideLength = Math.sqrt(2.0) * parentRadius;

                    double x = toImageSpace(Double.parseDouble(tokens[1]));
                    double y = toImageSpace(Double.parseDouble(tokens[2]));

                    x *= sideLength / resolution;
                    y *= sideLength / resolution;

                    x += parentMSC.getX();
                    y += parentMSC.getY();

                    double area = Double.parseDouble(tokens[3]);
                    areaSum += area;

                    MSC currMSC = new MSC(name, description, x, y, area);
                    currMidLevelMSCs.add(currMSC);

                    line = br.readLine();
                }

                br.close();

                for (MSC msc: currMidLevelMSCs) {
                    double factor = areaSum / parentMSC.getArea();
                    msc.setArea(msc.getArea() / factor);
                    midLevelMSCs.add(msc);
                    str2msc.put(msc.getName(), msc);
                }
            }
        }
        computeMapData();
    }


    /**
     * This function scales a given coordinate value from the range [-0.5, 0.5] to the image space. It adds 0.55 to
     * guarantee a non-negative value and then multiplies with 90 % of the resolution of the window. This makes sure
     * that every point will be visible, i.e. the return value is never bigger than resolution.
     * @param oldValue previous value in [-0.5, 0.5]
     * @return scaled value in [0, 0.99 * resolution]
     */
    private double toImageSpace(double oldValue) {
        return (oldValue + 0.55) * 0.9 * resolution;
    }

    /**
     * This function initializes the underlying data grids required for rendering the map. Also it calculates
     * the influence of MSCs on the surrounding pixels.
     */
    public void computeMapData() {
        data = new int[resolution][resolution];
        nearestMSC = new MSC[resolution][resolution];
        currMax = new double[resolution][resolution];

        List<MSC> mscs = midLevelMSCs.isEmpty() ? topLevelMSCs : midLevelMSCs;

        for (MSC msc: mscs) {
            double k = getScalingFactor(msc);

            double radius = Math.PI * k;
            for (int i = (int) Math.max(0, msc.getX() - radius); i <= Math.min(resolution - 1, msc.getX() + radius); i++) {
                for (int j = (int) Math.max(0, msc.getY() - radius); j <= Math.min(resolution - 1, msc.getY() + radius); j++) {
                    double dist = Math.hypot(msc.getX() - i, msc.getY() - j);
                    if (dist <= radius) {
                        if (currMax[i][j] < getCosine(dist, k)) {
                            currMax[i][j] = getCosine(dist, k);
                            nearestMSC[i][j] = msc;
                        }
                        data[i][j] += getCosine(dist, k);

                        dataMax = Math.max(dataMax, data[i][j]);
                        dataMin = Math.min(dataMin, data[i][j]);
                    }
                }
            }
        }
    }

    public double getScalingFactor(MSC msc) {
 		/**
 		 * This behaves as a scaling factor for the size of the MSC bubbles,
 		 * currently it is still chosen empirically depending on the desired output.
 		 * Needs to be adjusted for different datasets.
 		 * TODO: Figure out on what exactly this depends
 		 */
     	double factor = 10.5;
        return Math.sqrt(1.0e6 / (topLevelMSCs.size() * Math.pow(Math.PI, 3)) * factor * msc.getArea() / areaMax);
    }

    /**
     * This implements a radial basis function.
     * @param dist Distance to the MSC center
     * @param k    scaling factor computed by getScalingFactor()
     * @return     cos(dist / k) + 1.0
     */
    public double getCosine(double dist, double k) {
        if (Math.PI * k < Math.abs(dist)) {
            return 0.0;
        }
        return Math.cos(dist / k) + 1.0;
    }

    /**
     * This method will be used later for detection of lakes. It assigns each contiguous sea region a different
     * label, where only the surrounding ocean will have the label 0 in th  e end.
     */
    private void labelImage() {
        /** initializes the label matrix to infinity  */
        for (int[] row: labels) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }


        /**
         * forward scan through the image. for each water pixel the 4 backward neighbors are considered and it
         * assumes the minimum value of them
         */
        for (int i = 0; i < resolution; ++i) {
            for (int j = 0; j < resolution; ++j) {
                /** nearestMSC = null means this is a water node */
                if (nearestMSC[i][j] == null) {
                    /**
                     * this is another initialization, only changing water nodes. The previous setting to infinity
                     * was necessary in order to never consider the label of land nodes.
                     */
                    labels[i][j] = i * resolution + j;
                }
            }
        }
        labels[0][0] = labels[resolution-1][0] = 0;

        for (int i = resolution - 1; i >= 0; --i) {
            for (int j = 0; j < resolution; ++j) {
                if (nearestMSC[i][j] == null) {
                    boolean done = false;
                    for (int x = Math.min(i+1, resolution - 1); x >= Math.max(i-1, 0) && !done; --x) {
                        for (int y = Math.max(j-1, 0); y <= Math.min(j+1, resolution-1) && !done; ++y) {
                            if (x == i && y == j) {
                                done = true;
                                continue;
                            }
                            labels[i][j] = Math.min(labels[i][j], labels[x][y]);
                        }
                    }
                }
            }
        }

        /**
         * backward scan through the image. for each water pixel the 4 forward neighbors are  considered and it
         * assumes the minimum value of them. Almost identical to the previous loop except that the initialization is
         * not necessary anymore
         */
        for (int i = 0; i < resolution; ++i) {
            for (int j = resolution -1; j >= 0; --j) {
                if (nearestMSC[i][j] == null) {
                    boolean done = false;
                    for (int x = Math.max(i-1, 0); x <= Math.min(i+1, resolution-1) && !done; ++x) {
                        for (int y = Math.min(j+1, resolution - 1); y >= Math.max(j-1, 0) && !done; --y) {
                            if (x == i && y == j) {
                                done = true;
                                continue;
                            }
                            labels[i][j] = Math.min(labels[i][j], labels[x][y]);
                        }
                    }
                }
            }
        }
    }

    private MSC getParentMSC (MSC child) {
        if (child == null) {
            return null;
        }

        MSC parent;

        switch (child.getLevel()) {
            case 1:
                parent = str2msc.get(child.getName().substring(0, 5) + "-XX");
                break;

            case 2:
                if (child.getName().charAt(5) == '-') {
                    parent = str2msc.get(child.getName().substring(0, 6) + "XX");
                } else {
                    parent = str2msc.get(child.getName().substring(0, 6) + "xx");
                }
                break;

            default:
                parent = null;
                break;
        }

        return parent;
    }

    private void detectImageBorders() {
        /** loop through the whole data set */
        for (int i = 0; i < resolution - 1; i++) {
            for (int j = 0; j < resolution - 1; j++) {
                /** detect borders in x direction */
                if (nearestMSC[i][j] != nearestMSC[i+1][j]) {
                    /** allocate space if not yet existing */
                    if (!msc2coords.containsKey(nearestMSC[i][j])) {
                        msc2coords.put(nearestMSC[i][j], new HashSet<Coords2D>());
                    }

                    /** allocate space if not yet existing */
                    if (!msc2coords.containsKey(nearestMSC[i+1][j])) {
                        msc2coords.put(nearestMSC[i+1][j], new HashSet<Coords2D>());
                    }

                    /**
                     * Add respective coordinate to both lists. We need to multiply both i and j by 2 here,
                     * so that we can take "middle elements" and still work with integers
                     */
                    msc2coords.get(nearestMSC[i][j]).  add(new Coords2D(i+0.5, j));
                    msc2coords.get(nearestMSC[i+1][j]).add(new Coords2D(i+0.5, j));

                    MSC parent1 = getParentMSC(nearestMSC[i][j]);
                    MSC parent2 = getParentMSC(nearestMSC[i+1][j]);

                    if (parent1 != parent2) {
                        /** allocate space if not yet existing */
                        if (!msc2coords.containsKey(parent1)) {
                            msc2coords.put(parent1, new HashSet<Coords2D>());
                        }

                        /** allocate space if not yet existing */
                        if (!msc2coords.containsKey(parent2)) {
                            msc2coords.put(parent2, new HashSet<Coords2D>());
                        }

                        /**
                         * Add respective coordinate to both lists. We need to multiply both i and j by 2 here,
                         * so that we can take "middle elements" and still work with integers
                         */
                        msc2coords.get(parent1).add(new Coords2D(i+0.5, j));
                        msc2coords.get(parent2).add(new Coords2D(i+0.5, j));
                    }


                    /** Coastline detection */
                    if (nearestMSC[i][j] == null || nearestMSC[i+1][j] == null) {
                        coastLine. add(new Coords2D(i+0.5, j));
                        coords2MSC.put(new Coords2D(i+0.5, j),
                                nearestMSC[i][j] != null ? nearestMSC[i][j] : nearestMSC[i+1][j]);
                        if (coords2MSC.get(new Coords2D(i+0.5, j)) == null) {
                            System.err.printf("Could not resolve MSC at (%.1f, %d)\n", i+0.5, j);
                        }
                    }

                    /** printing the node, the id is 2*i+1, 2*j in base 2 * resolution */
                    osmFile.addGridNode(i + 0.5, j);
                }

                /** detect borders in y direction */
                if (nearestMSC[i][j] != nearestMSC[i][j+1]) {
                    /** allocate space if not yet existing */
                    if (!msc2coords.containsKey(nearestMSC[i][j])) {
                        msc2coords.put(nearestMSC[i][j], new HashSet<Coords2D>());
                    }

                    /** allocate space if not yet existing */
                    if (!msc2coords.containsKey(nearestMSC[i][j+1])) {
                        msc2coords.put(nearestMSC[i][j+1], new HashSet<Coords2D>());
                    }

                    /** Add respective coordinate to both lists. */
                    msc2coords.get(nearestMSC[i][j]).  add(new Coords2D(i, j+0.5));
                    msc2coords.get(nearestMSC[i][j+1]).add(new Coords2D(i, j+0.5));

                    MSC parent1 = getParentMSC(nearestMSC[i][j]);
                    MSC parent2 = getParentMSC(nearestMSC[i][j+1]);

                    if (parent1 != parent2) {
                        /** allocate space if not yet existing */
                        if (!msc2coords.containsKey(parent1)) {
                            msc2coords.put(parent1, new HashSet<Coords2D>());
                        }

                        /** allocate space if not yet existing */
                        if (!msc2coords.containsKey(parent2)) {
                            msc2coords.put(parent2, new HashSet<Coords2D>());
                        }

                        /**
                         * Add respective coordinate to both lists. We need to multiply both i and j by 2 here,
                         * so that we can take "middle elements" and still work with integers
                         */
                        msc2coords.get(parent1).add(new Coords2D(i, j+0.5));
                        msc2coords.get(parent2).add(new Coords2D(i, j+0.5));
                    }


                    /** Coastline detection */
                    if (nearestMSC[i][j] == null || nearestMSC[i][j+1] == null) {
                        coastLine. add(new Coords2D(i, j+0.5));
                        coords2MSC.put(new Coords2D(i, j+0.5),
                                nearestMSC[i][j] != null ? nearestMSC[i][j] : nearestMSC[i][j+1]);
                        if (coords2MSC.get(new Coords2D(i, j+0.5)) == null) {
                            System.err.printf("Could not resolve MSC at (%d, %.1f)\n", i, j + 0.5);
                        }
                    }

                    /** printing the node, the id is 2*i, 2*j+1 in base 2 * resolution */
                    osmFile.addGridNode(i, j + 0.5);
                }
            }
        }
    }

    /**
     * The following is a small trick to make Maperitive render a map with boundaries at (-5,5).
     * For that we create tiny islands at each corner which will not be visible in the final
     * output image.
     */
    private void maperitiveExpandOcean() {
        int bboxX[] = {-5,  5, 5, -5};
        int bboxY[] = {-5, -5, 5,  5};

        int bboxEPSX[] = {1, 0, -1, 0};
        int bboxEPSY[] = {0, 1, 0, -1};

        double EPS = 1e-7;

        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                String name = String.format("bbox(%d,%d)%d", bboxX[i], bboxY[i], j);
                osmFile.addSpecialNode(bboxX[i] + bboxEPSX[j] * EPS, bboxY[i] + bboxEPSY[j] * EPS, name);
                osmFile.endSpecialNode();
            }
        }

        for (int i = 0; i < 4; ++i) {
            osmFile.addWay("boundingBox" + i);
            for (int j = 4; j >= 0; --j) {
                String name = String.format("bbox(%d,%d)%d", bboxX[i % 4], bboxY[i % 4], j % 4);
                osmFile.addSpecialNodeReference(name);
            }
            osmFile.addTag("name", "boundingBox" + i);
            osmFile.addTag("natural", "coastline");
            osmFile.endWay();
        }
    }

    private void renderCitiesToMap() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(cityData));
        String line;

        br.readLine();
        line = br.readLine();

        double scalingFactor = resolution / 400.0;
        double maxCityRadius = Math.cbrt(Double.parseDouble(line.split(",")[3]));

        while (line != null) {
            String[] tokens =  line.split(",");
            String cityId = tokens[0];
            double x = toImageSpace(Double.parseDouble(tokens[1]));
            double y = toImageSpace(Double.parseDouble(tokens[2]));
            double currRadius = Math.cbrt(Double.parseDouble(tokens[3])) / maxCityRadius * scalingFactor;

            if (nearestMSC[(int) x][(int) y] != null && currRadius / scalingFactor > 0.3) {
                Set<Coords2D> cityCoords = new LinkedHashSet<Coords2D>();
                for (int i = 0; i < 60; ++i) {
                    double t = Math.PI * 2.0 * i / 60.0;
                    double currX = x + Math.cos(t) * currRadius;
                    double currY = y + Math.sin(t) * currRadius;
                    cityCoords.add(new Coords2D(currX, currY));
                }

                int idx = 0;
                for (Coords2D coords: cityCoords) {
                    osmFile.addSpecialNode(coords.getX() / resolution, coords.getY() / resolution, cityId + "_" + idx++);
                    osmFile.endSpecialNode();
                }

                osmFile.addWay(cityId);

                for (idx = 0; idx < cityCoords.size(); ++idx) {
                    osmFile.addSpecialNodeReference(cityId + "_" + idx++);
                }

                osmFile.addSpecialNodeReference(cityId + "_0");

                osmFile.addTag("name", cityId);
                osmFile.addTag("MSCCity", "0");
                osmFile.endWay();
            }
            line = br.readLine();
        }

        br.close();
    }

    private List<List<Coords2D>> orderSetOfBorderCoords(Set<Coords2D> borderCoords) {
        return orderSetOfBorderCoords(borderCoords, true);
    }

    private List<List<Coords2D>> orderSetOfBorderCoords(Set<Coords2D> borderCoords, boolean isCoastline) {

        List<List<Coords2D>> orderedBorders = new ArrayList<List<Coords2D>>();
        while (!borderCoords.isEmpty()) {
            List<Coords2D> currentBorder = new ArrayList<Coords2D>();
            Coords2D startCoords = borderCoords.iterator().next();
            currentBorder.add(startCoords);
            borderCoords.remove(startCoords);

            /** initialize convenience variables */
            Coords2D currCoords = startCoords;

            /** boolean we need to check if our line is closed already */
            boolean isClosed = false;

            while (!isClosed) {
                /** keep track of possible next steps */
                Coords2D nearestNeighbor = null;
                double minDist = Double.MAX_VALUE;

                if (isCoastline) {
                    for (double i = -2.0; i <= 2.0; i += 0.5) {
                        for (double j = -2.0; j <= 2.0; j += 0.5) {
                            Coords2D coords = new Coords2D(currCoords.getX() + i, currCoords.getY() + j);
                            if (borderCoords.contains(coords)) {
                                if (minDist > currCoords.distanceTo(coords)) {
                                    minDist = currCoords.distanceTo(coords);
                                    nearestNeighbor = coords;
                                }
                            }
                        }
                    }
                } else {
                    for (Coords2D coords: borderCoords) {
                        if (minDist > currCoords.distanceTo(coords)) {
                            minDist = currCoords.distanceTo(coords);
                            nearestNeighbor = coords;
                        }
                    }
                }

                if (minDist >= 5.0 && minDist >= currCoords.distanceTo(startCoords)) {
                    nearestNeighbor = startCoords;
                    isClosed = true;
                }

                borderCoords.remove(nearestNeighbor);
                currentBorder.add(nearestNeighbor);
        //        System.err.printf("(%.5f, %.5f)\n", nearestNeighbor.x / resolution, nearestNeighbor.y / resolution);
                currCoords = nearestNeighbor;


            }
            orderedBorders.add(currentBorder);
        }
        return orderedBorders;
    }

    private void orderAndOrientateCoastline() {
        List<List<Coords2D>> orderedCoastline = orderSetOfBorderCoords(coastLine, true);

        /** serves as a code id */
        int coastCounter = 0;

        for (List<Coords2D> currCoast: orderedCoastline) {
            /**
             * This is required to find out the orientation of the coastline. Idea taken from
             * http://stackoverflow.com/a/1165943
             */
            int signedArea = 0;

            int xCoord = (int) currCoast.get(0).getX();
            int yCoord = (int) currCoast.get(0).getY();

            int minLabel = Integer.MAX_VALUE;

            for (int x = Math.max(0, xCoord-2); x <= Math.min(resolution-1, xCoord+2); x++) {
                for (int y = Math.max(0, yCoord-2); y <= Math.min(resolution-1, yCoord+2); y++) {
                    minLabel = Math.min(minLabel, labels[x][y]);
                }
            }

            boolean isLake = (minLabel != 0);

            for (int i = 1; i < currCoast.size(); ++i) {
                Coords2D currCoords = currCoast.get(i-1);
                Coords2D nextCoords = currCoast.get(i);
                signedArea += (nextCoords.getX() - currCoords.getX()) * (nextCoords.getY() + currCoords.getY());
            }

            /**
             * When we closed our loop we need to look at the sign of our area to check if we need to flip the order
             * of the nodes. Finally we write to the file.
             */
            if (signedArea < 0) {
                Collections.reverse(currCoast);
            }

            /**
             * the following steps are necessary to meet OSM's requirement that a way should not consist of more than
             * 2000 nodes. In order to do so we take the current string buffer and split it into parts of 2000.
             */
            boolean done = false;
            int currentCoastLength = 0;

            while (!done) {
                /** we start a new coast, so increase the counter */
                coastCounter++;
                /** start a new way. action is also a required attribute here */

                osmFile.addWay("coast" + coastCounter);
                /** process the next batch of nodes */
                for (int i = currentCoastLength; i < Math.min(currentCoastLength + 2000, currCoast.size()); ++i) {
                    osmFile.addGridNodeReference(currCoast.get(i).getX(), currCoast.get(i).getY());
                }

                /** print appropriate key-value pairs, make sure to distinguish lakes and the ocean */
                osmFile.addTag("name", "coast" + coastCounter);
                if (isLake) {
                    osmFile.addTag("natural", "water");
                    osmFile.addTag("water", "lake");
                } else {
                    osmFile.addTag("natural", "coastline");
                }
                osmFile.endWay();

                /**
                 * we are not incrementing by 2000 because the last node needs to be the starting node in the next
                 * way. Then check if we are done processing
                 */
                currentCoastLength += 1999;
                if (currCoast.size() <= currentCoastLength) {
                    done = true;
                }
            }

        }
    }

    private void renderCountryBorders() {
        /**
         * This part is responsible to render the countries and corresponding borders. Of course,
         * we need to loop through all MSCs for this.
         */
        for (MSC msc: msc2coords.keySet()) {

            /** we disregard null, i.e. the ocean */
            if (msc != null) {

                /**
                 * We need to consider the nodes in the appropriate order, that is walking around the circle counter
                 * clockwise. To do so we at first need to find a point inside a given country. Since they have a
                 * somewhat convex shape, taking the average is a good starting point.
                 */
                Coords2D avg = new Coords2D();
                for (Coords2D foo: msc2coords.get(msc)) {
                    avg.setX(avg.getX() + foo.getX());
                    avg.setY(avg.getY() + foo.getY());
                }

                avg.setX(avg.getX() / msc2coords.get(msc).size());
                avg.setY(avg.getY() / msc2coords.get(msc).size());

                /**
                 * Then we proceed to sort the list according to our own comparison criteria. It will look at the
                 * angle a line through a given point and the "origin" has with respect to the x-axis and sorts in
                 * increasing order.
                 */
                HashSet<Coords2D> currSet = msc2coords.get(msc);
                List<List<Coords2D>> orderedBorders = orderSetOfBorderCoords(currSet);

                /**
                 * Finally we print the whole way to the file. It includes all necessary nodes and the required key
                 * value pairs to identify it as a country.
                 */

                int counter = 0;

                for (List<Coords2D> currList: orderedBorders) {
                    osmFile.addWay(msc.getName() + ++counter);

                    for (Coords2D cursor: currList) {
                        osmFile.addGridNodeReference(cursor.getX(), cursor.getY());
                    }

                    osmFile.addTag("name", msc.getName() + "_" + counter);
                    osmFile.addTag("MSCBorder", String.valueOf(msc.getLevel()));
                    if (msc.getLevel() == 0) {
                        osmFile.addTag("MSCColor",    String.valueOf(coloring[topMSC2int.get(msc)]));
                        int mscId = Integer.parseInt(msc.getName().substring(3, 5));
                        osmFile.addTag("RusinColor",  String.valueOf(rusinColoring[mscId]));
                    }
                    osmFile.endWay();
                }
            }
        }
    }

    /**
     * This method creates a graph in adjacency matrix form.
     * In addition it computes the degree of every node in the graph.
     * This is relevant for the coloring later on.
     * @param isMidLevelGrid indicates whether the underlying grid consists of mid-level MSCs
     */
    private void createGraph(boolean isMidLevelGrid) {
        int N = topLevelMSCs.size();
        graph = new int[N][N];
        degrees = new int[N];

        for (MSC[] aNearestMSC : nearestMSC) {
            for (int j = 0; j + 1 < aNearestMSC.length; ++j) {
                MSC msc1 = isMidLevelGrid ? getParentMSC(aNearestMSC[j])   : aNearestMSC[j];
                MSC msc2 = isMidLevelGrid ? getParentMSC(aNearestMSC[j+1]) : aNearestMSC[j+1];

                if (msc1 != null && msc2 != null && msc1 != msc2) {
                    int idx1 = topMSC2int.get(msc1);
                    int idx2 = topMSC2int.get(msc2);

                    if (graph[idx1][idx2] == 0) {
                        degrees[idx1]++;
                        degrees[idx2]++;
                    }

                    graph[idx1][idx2] = graph[idx2][idx1] = 1;
                }
            }
        }
    }

    /**
     * This method colors a given graph in a very naive way. At first it finds out the maximum
     * degree in the graph. Then it goes through each node and tries to allocate it the color
     * with the least id after removing the colors from its neighbors. Unallocated nodes have
     * a color id of 0 until they are considered. The result is stored in the coloring[] array.
     */
    private void colorGraph() {
        int maxDegree = 0;
        for (int deg: degrees) {
            maxDegree = Math.max(maxDegree, deg);
        }

        int N = topLevelMSCs.size();

        for (int i = 0; i < N; ++i) {
            List<Integer> neighbors = new ArrayList<Integer>();
            for (int j = 0; j < N; ++j) {
                if (graph[i][j] == 1 && coloring[j] != 0) {
                    neighbors.add(j);
                }
            }

            Set<Integer> colors = new TreeSet<Integer>();
            for (int c = 1; c <= maxDegree + 1; ++c) {
                colors.add(c);
            }

            for (int idx: neighbors) {
                colors.remove(coloring[idx]);
            }

            coloring[i] = colors.iterator().next();
        }
    }

    private void initRusinColoring() {
        /**
         * Colors (source http://www.math.niu.edu/~rusin/known-math/index/mathmap.html):
         *  1 - Foundational Branches of Mathematics
         *  2 - Combinatorics
         *  3 - Number Theory
         *  4 - Algebraic Areas of Mathematics
         *  5 - Geometry
         *  6 - Topology
         *  7 - Functional Analysis
         *  8 - Real Analysis
         *  9 - Complex Analysis
         * 10 - Numerical Analysis
         * 11 - Differential Equations
         * 12 - Physics
         * 13 - Sciences and Engineering
         * 14 - Computers
         * 15 - Probabilities
         * 16 - Statistics
         * 17 - History and General
         */
        rusinColoring = new int[] {
            //  0   1   2   3   4   5   6   7   8   9
               17, 17,  0,  1,  0,  2,  2,  0,  1,  0,  // 00
                0,  3,  4,  4,  4,  4,  4,  4,  1,  4,  // 10
                4,  0,  4,  0,  0,  0,  8,  0,  8,  0,  // 20
                9,  9,  9,  8, 11, 11,  0, 11,  0,  8,  // 30
                8, 10,  7,  7,  7, 11,  7,  7,  0, 11,  // 40
                0,  5,  5,  5,  6,  6,  0,  6, 11,  0,  // 50
               15,  0, 16,  0,  0, 10,  0,  0, 14,  0,  // 60
               12,  0,  0,  0, 12,  0, 12,  0, 12,  0,  // 70
               12, 12, 12, 12,  0, 12, 12,  0,  0,  0,  // 80
               10, 12, 13, 13, 14,  0,  0, 17,  0,  0   // 90
        };
    }


    public void exportToOSM(File osmOutputFile) throws IOException {
        osmFile = new OSMFile(osmOutputFile, resolution);

        labelImage();

        detectImageBorders();

        /**
         * Add capitals for each MSC to the map. id, lat and lon are required for obvious reasons,
         * visible and version were added to be able to analyze the map in JOSM
         */
        for (MSC msc: topLevelMSCs) {
            osmFile.addSpecialNode(msc.getX() / resolution, msc.getY() / resolution, msc.getName());
            osmFile.addTag("name", msc.getName());
            osmFile.addTag("description", msc.getDescription());
            osmFile.addTag("MSCInfo", "0");
            osmFile.endSpecialNode();
        }


        for (MSC msc: midLevelMSCs) {
            osmFile.addSpecialNode(msc.getX() / resolution, msc.getY() / resolution, msc.getName());
            osmFile.addTag("name", msc.getName());
            osmFile.addTag("description", msc.getDescription());
            osmFile.addTag("MSCInfo", "1");
            osmFile.endSpecialNode();
        }

        maperitiveExpandOcean();


        if (cityData != null) {
            renderCitiesToMap();
        }

        createGraph(false);
        coloring = new int[topLevelMSCs.size()];
        colorGraph();

        initRusinColoring();

        orderAndOrientateCoastline();

        renderCountryBorders();

        osmFile.close();
    }

    /**
     * No longer used
     */
    // public MSC getMSC (double latitude, double longitude) {
    //     MSC maxMSC = null;
    //     double currMax = Double.MIN_VALUE;

    //     int x = (int) (latitude  * resolution);
    //     int y = (int) (longitude * resolution);

    //     for (MSC msc: topLevelMSCs) {
    //         double k = getScalingFactor(msc);
    //         double radius = Math.PI * k;

    //         double distance = Math.hypot(msc.getX() - x, msc.getY() - y);
    //         if (distance <= radius) {
    //             if (currMax < getCosine(distance, k) * msc.getArea()) {
    //                 currMax = getCosine(distance, k) * msc.getArea();
    //                 maxMSC = msc;
    //             }
    //         }
    //     }

    //     return maxMSC;
    // }

    /**
     * This method was needed to dump the grid into a CSV. Currently the WebService makes
     * still use of this file.
     * @param  msc                   output file
     * @param  level                 0 indicates top-level, 1 mid-level MSCs
     * @throws FileNotFoundException
     */
    public void exportMSCGrid (File msc, int level) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(msc);
        for (int i = 0; i < resolution; ++i) {
            for (int j = 0; j < resolution; ++j) {
                MSC currMSC = nearestMSC[i][j];
                if (level == 0) {
                    currMSC = getParentMSC(currMSC);
                }
                pw.printf("%s%c", currMSC == null ? "null" : currMSC.getName(), j+1 == resolution ? '\n' : ';');
            }
        }

        pw.close();
    }

    /**
     * This method was used to debug the labeling algorithm, no longer used.
     * @param  labelsF                OutputFile
     * @throws FileNotFoundException  Thrown in case labelsF is not found
     */
    // public void dumpLabels (File labelsF) throws FileNotFoundException {
    //     labelImage();
    //     OSMFile file = new OSMFile(labelsF, resolution);
    //     for (int i = 0; i < resolution; ++i) {
    //         for (int j = 0; j < resolution; ++j) {
    //             if (i == 0 && j == 0) continue;
    //             if (labels[i][j] == 0) {
    //                 file.addGridNode(i, j);
    //             }
    //         }
    //     }

    //     file.close();
    // }

    public static void main(String[] args) throws IOException {
        File dataDir = new File("/home/jdoerrie/Desktop/ZBMath/");
        if (dataDir.listFiles() != null) {
            for (File file: dataDir.listFiles()) {
                if (file.isDirectory() && file.getName().startsWith("cuml1986")) {
                    File plotData = new File(file + "/PlotData.txt");
                    if (plotData.exists()) {
                        File cityData = new File(file + "/cities.csv");
                        cityData = null;
                        GenerateOSM osm = new GenerateOSM(plotData, cityData,
                                new File("project/myData/Desc_msc2010-final.txt"), 1024);
                        // System.out.println(osm.getMSC(0.5, 0.5));
                        osm.exportToOSM(new File(file + "/" + file.getName() + "Map.osm"));

//                        osm.dumpLabels(new File(file + "/" + file.getName() + "Labels.osm"));
//                        osm.exportMSCGrid(new File(file + "/MSCGrid1.csv"), 1);
                    }
                }
            }
        }
    }
}
