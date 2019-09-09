/**
 * Copyright (c) 2013-19 KWARC Group <kwarc.info>
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

/**
 * A class to represent a MSC class. It consists of a name, a description,
 * x and y coordinates and area. Also it saves the level of the MSC, where
 * 0 represents top level, 1 mid level and 2 bottom level.
 */
public class MSC {
    private String name;
    private String description;
    private double x;
    private double y;
    private double area;
    private int level;

    /**
     * Constructor setting the private fields. It sets the level depending on
     * the suffix of the name (top level classes end in -XX, mid level classes
     * in -xx)
     * @param name         name of the MSC
     * @param description  description of the MSC
     * @param x            x-coordinate
     * @param y            y-coordinate
     * @param area         area of the MSC
     */
    public MSC(String name, String description, double x, double y, double area) {
        this.name = name;
        this.description = description;
        this.x = x;
        this.y = y;
        this.area = area;

        if (name.endsWith("XX")) {
            level = 0;
        } else if (name.endsWith("xx")) {
            level = 1;
        } else {
            level = 2;
        }
    }

    /***********************
     * Getters and Setters *
     ***********************/
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    /**
     * IntelliJ IDEA toString() implementation
     */
    public String toString() {
        return "MSC{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", area=" + area +
                ", level=" + level +
                '}';
    }
}