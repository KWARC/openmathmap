/**
 * Copyright (c) 2013-19 KWARC Group <kwarc.info>
 *
 * This file is part of OpenMathMap.
 *
 * OpenMathMap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * OpenMathMap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenMathMap. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * This class specifies a MSC class with the properties name, description,
 * x and y coordinates and area. In addition xml annotations are provided
 * which define which of those fields will be visible in the JSONP output.
 */
package mathservice;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(propOrder = {"name", "desc", "x", "y"})
public class MSC {
	private String name;
	private String desc;
	private double x;
	private double y;
	private double area;

	public MSC() {
		this(null, null, 0.0, 0.0, 0.0);
	}

	public MSC(String name, String description) {
		this(name, description, 0.0, 0.0, 0.0);
	}

	public MSC(String name, String desc, double x, double y, double area) {
		this.name = name;
		this.desc = desc;
		this.x = x;
		this.y = y;
		this.area = area;
	}

	@XmlElement
	public String getName() {
		return name;
	}

	@XmlElement(name = "description")
	public String getDesc() {
		return desc;
	}

	@XmlElement
	public double getX() {
		return x;
	}

	@XmlElement
	public double getY() {
		return y;
	}

	@XmlTransient
	public double getArea() {
		return area;
	}

	@Override
	public String toString() {
		return "MSC [name=" + name + ", desc=" + desc + "]\n";
	}

}
