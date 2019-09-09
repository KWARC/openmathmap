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
package mathservice;


import java.io.*;
import java.util.*;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.JSONP;

/**
 * This class provides the required methods and APIs for the webservice.
 * It consists of a constructor and two private methods assisting it to
 * parse the required data from the provided text files.
 *
 * In addition it provides two public APIs, mscquery and msclookup.
 *
 * mscquery is used to resolve latitude and longitude coordinates to
 * MSC classes where the response is wrapped in JSONP. Additionally
 * it takes the current zoom level as parameter to decide if it should
 * return a top level or second-level class.
 *
 * msclookup is an experimental feature to allow for name-based search.
 * Currently it is very simple and inefficient, it returns the first MSC
 * where the search string is a substring of either the name or the
 * description.
 */
@Path("/")
public class MathService {

	private static MSC[][][] grid;
	private static int resolution;
	private static int levels;
	private static Map<String, String> name2desc;
	private static List<MSC> mscs;

	public MathService(@Context ServletContext context) throws IOException {
		resolution = 512;
		levels = 2;

		if (name2desc == null) {
			name2desc = new TreeMap<String, String>();
			parseDescriptions(context);
		}

		if (grid == null) {
			grid = new MSC[resolution][resolution][levels];
			parseGrid(context);
		}

		if (mscs == null) {
			mscs = new ArrayList<MSC>();
			parsePlotData(context);
		}
	}

	private void parseDescriptions(ServletContext context) throws IOException {
		File descFile = new File(context.getRealPath("/") + "/data/Descriptions.txt");
		BufferedReader br = new BufferedReader(new FileReader(descFile));
		String line = br.readLine();
		while (line != null) {
			String tokens[] = line.split("\\*\\*\\*");

			String name = "MSC" + tokens[0];
			String desc = tokens[1];
			name2desc.put(name, desc);
			line = br.readLine();
		}

		name2desc.put("null", "null");
		br.close();
	}

	private void parsePlotData(ServletContext context) throws IOException {
		File plotData = new File(context.getRealPath("/") + "/data/MergedPlotData.txt");
		BufferedReader br = new BufferedReader(new FileReader(plotData));
		String line = br.readLine();
		while (line != null) {
			String tokens[] = line.split(",");
			String name = tokens[0];
			String desc = name2desc.get(name);
			double xCoord = Double.parseDouble(tokens[1]);
			double yCoord = Double.parseDouble(tokens[2]);
			double area   = Double.parseDouble(tokens[3]);

			mscs.add(new MSC(name, desc, xCoord, yCoord, area));
			line = br.readLine();
		}

		name2desc.put("null", "null");
		br.close();
	}

	private void parseGrid(ServletContext context) throws IOException {
		for (int level = 0; level < levels; ++level) {
			File gridFile = new File(context.getRealPath("/") + "/data/MSCGrid" + level + ".csv");
			BufferedReader br = new BufferedReader(new FileReader(gridFile));
			for (int i = 0; i < resolution; i++) {
				String line = br.readLine();
				String tokens[] = line.split(";");
				for (int j = 0; j < resolution; j++) {
					String name = tokens[j];
					String desc = name2desc.get(name);
					grid[i][j][level] = new MSC(name, desc);
				}
			}
			br.close();
		}
	}

	private double clamp (double val, double min, double max) {
		return Math.min(max, Math.max(min, val));
	}

	private MSC getMSCByLocation(double lat, double lng, int level) {
		lat = clamp(lat, 0.0, 1.0);
		lng = clamp(lng, 0.0, 1.0);
		int x = (int) (resolution * lat + 0.5);
		int y = (int) (resolution * lng + 0.5);
		return grid[x][y][level];
	}

	private MSC getMSCByString(String str) {
		for (MSC msc: mscs) {
			if ( msc.getName().contains(str) ||
				 msc.getDesc().contains(str) ) {
				return msc;
			}
		}

		return null;
	}

	@GET
	@JSONP(queryParam="callback")
	@Produces({"application/javascript"})
	@Path("/mscquery")
	public MSC getMSCByLocationJSONP(@QueryParam("lat")  double lat,
 						               			 @QueryParam("long") double lng,
 						               			 @QueryParam("zoom")  @DefaultValue("9") int zoom
 						               			 ) {
		int level;
		if (zoom < 13) {
			level = 0;
		} else {
			level = 1;
		}

		return getMSCByLocation(lat, lng, level);
	}


	@GET
	@JSONP(queryParam="callback")
	@Produces({"application/javascript"})
	@Path("/msclookup")
	public MSC getMSCByStringJSONP(@QueryParam("search")  String str) {

		return getMSCByString(str);
	}
}