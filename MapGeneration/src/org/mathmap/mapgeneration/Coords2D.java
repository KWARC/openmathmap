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

/**
 * Helper class to represent Coordinates in 2D. It provides overridden equals, hashCode and toString methods,
 * in order to use them in sets and to debug them more easily.
 */
public class Coords2D {
    private double x; /** x-coordinate */
    private double y; /** y-coordinate */

    /** Empty constructor, setting x and y to 0. */
    public Coords2D() {
        this(0.0, 0.0);
    }

    /**
     * Constructor setting x and y.
     * @param x x-coordinate
     * @param y y-coordinate
     */
    public Coords2D(double x, double y) {
        this.x = x;
        this.y = y;
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

    public double distanceTo(Coords2D c) {
        return Math.hypot(x - c.x, y - c.y);
    }

    /**
     * Implementation of equals. Required to be able to use Coords2D in a HashSet
     * @param o Object the current object is compared to
     * @return boolean indicating if the objects are equal()
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Coords2D coords2D = (Coords2D) o;

        if (Double.compare(coords2D.x, x) != 0) return false;
        if (Double.compare(coords2D.y, y) != 0) return false;

        return true;
    }

    /**
     * Implementation of hashCode. Required to be able to use Coords2D in a HashSet
     * @return hash code
     */
    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    /**
     * Default IDEA implementation of toString, used for debugging.
     */
    @Override
    public String toString() {
        return "Coords2D{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}