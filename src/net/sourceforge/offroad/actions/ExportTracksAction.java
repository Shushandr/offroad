/** 
   OffRoad
   Copyright (C) 2016 Christian Foltin

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/

package net.sourceforge.offroad.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 28.07.2016
 */
public class ExportTracksAction extends OffRoadAction {

	public ExportTracksAction(OsmWindow pContext) {
		super(pContext);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
		File selectedFile = getSaveFile();
		if(selectedFile!=null){
			PrintWriter writer;
			try {
				writer = new PrintWriter(selectedFile, StandardCharsets.UTF_8);
				writer.println(getHtmlFile(selectedFile));
				writer.close();
				mContext.openDocument(selectedFile.toURI().toURL());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private String getHtmlFile(File pSelectedFile) {
		List<SelectedGpxFile> selectedGPXFiles = mContext.getSelectedGpxHelper().getSelectedGPXFiles();
		if(selectedGPXFiles.isEmpty()){
			return "<html><title>no tracks selected</title></html>";
		}
		Map<SelectedGpxFile, String> localList = new HashMap<>();
		String firstPath = null;
		for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
			GPXFile gpxFile = selectedGpxFile.getGpxFile();
			String lname = new File(gpxFile.path).getName();
			File dest = new File(pSelectedFile.getParentFile(), lname);
			log.info("Copying track " + gpxFile.path + " -> " + dest.getAbsolutePath());
			String result = GPXUtilities.writeGpxFile(dest, gpxFile, mContext);
			if(result != null){
				mContext.showToastMessage(result);
				return "<html><title>Error copying tracks</title></html>";
			}
			localList.put(selectedGpxFile, lname);
			if(firstPath == null){
				firstPath = lname;
			}
		}

		String out = "<meta charset=\"UTF-8\"> \n"
	+"<html>\n"
	+"<head>\n"
	+"  <!-- Source: http://wiki.openstreetmap.org/wiki/Openlayers_Track_example -->\n"
	+"  <title>Export from OffRoad</title>\n"
	+"  <!-- bring in the OpenLayers javascript library\n"
	+"        (here we bring it from the remote site, but you could\n"
	+"         easily serve up this javascript yourself) -->\n"
	+"  <script src=\"http://www.openlayers.org/api/OpenLayers.js\"></script>\n"
	+"  <!-- bring in the OpenStreetMap OpenLayers layers.\n"
	+"        Using this hosted file will make sure we are kept up\n"
	+"         to date with any necessary changes -->\n"
	+"  <script src=\"http://www.openstreetmap.org/openlayers/OpenStreetMap.js\"></script>\n"
	+" \n"
	+"  <script type=\"text/javascript\">\n"
	+"    function loadXMLDoc(dname) \n"
	+"{\n"
	+"    if (window.XMLHttpRequest)\n"
	+"    {\n"
	+"        xhttp=new XMLHttpRequest();\n"
	+"    }\n"
	+"    else\n"
	+"    {\n"
	+"        xhttp=new ActiveXObject(\"Microsoft.XMLHTTP\");\n"
	+"    }\n"
	+"    xhttp.open(\"GET\",dname,false);\n"
	+"    xhttp.send();\n"
	+"    return xhttp.responseXML;\n"
	+"}\n"
	+" \n"
	+" \n"
	+"    function showTrack(map, layerMarkers, url, name, color) {\n"
	+"        // Add the next Layer with the GPX Track                                                                                                   \n"
	+"        var lgpx2 = new OpenLayers.Layer.Vector(name, {\n"
	+"            strategies: [new OpenLayers.Strategy.Fixed()],\n"
	+"            protocol: new OpenLayers.Protocol.HTTP({\n"
	+"                url: url,\n"
	+"                format: new OpenLayers.Format.GPX()\n"
	+"            }),\n"
	+"            style: {strokeColor: color, strokeWidth: 5, strokeOpacity: 0.5},\n"
	+"            projection: new OpenLayers.Projection(\"EPSG:4326\")\n"
	+"        });\n"
	+"        map.addLayer(lgpx2);\n"
	+"    var xmlDoc=loadXMLDoc(url);\n"
	+"    var points = xmlDoc.getElementsByTagName(\"trkpt\"); \n"
	+"    var point = points[0];\n"
	+"    var lat=Number(point.getAttribute(\"lat\")); \n"
	+"    var lon=Number(point.getAttribute(\"lon\"));\n"
	+"    var size = new OpenLayers.Size(21, 25);\n"
	+"    var offset = new OpenLayers.Pixel(-(size.w/2), -size.h);\n"
	+"    var lonLat = new OpenLayers.LonLat(lon, lat).transform(new OpenLayers.Projection(\"EPSG:4326\"), map.getProjectionObject());\n"
	+"    var icon = new OpenLayers.Icon('http://www.openstreetmap.org/openlayers/img/marker.png',size,offset);\n"
	+"    layerMarkers.addMarker(new OpenLayers.Marker(lonLat,icon));\n"
	+"    point = points[points.length-1];\n"
	+"    lat=Number(point.getAttribute(\"lat\")); \n"
	+"    lon=Number(point.getAttribute(\"lon\"));\n"
	+"    lonLat = new OpenLayers.LonLat(lon, lat).transform(new OpenLayers.Projection(\"EPSG:4326\"), map.getProjectionObject());\n"
	+"    icon = new OpenLayers.Icon('http://www.openstreetmap.org/openlayers/img/marker.png',size,offset);\n"
	+"    layerMarkers.addMarker(new OpenLayers.Marker(lonLat,icon));\n"
	+" \n"
	+"  }\n"
	+"  \n"
	+"    var gpstrack = \"" + firstPath + "\";\n"
	+" \n"
	+"    var xmlDoc=loadXMLDoc(gpstrack);\n"
	+"    var points = xmlDoc.getElementsByTagName(\"trkpt\");\n"
	+" \n"
	+"    var point = points[0];\n"
	+"    var lat=Number(point.getAttribute(\"lat\")); \n"
	+"    var lon=Number(point.getAttribute(\"lon\"));\n"
	+"    var zoom=" + Math.min(19, mContext.getZoom()) + ";\n"
	+" \n"
	+"    var map; //complex object of type OpenLayers.Map\n"
	+" \n"
	+"    function init() {\n"
	+"    map = new OpenLayers.Map (\"map\", {\n"
	+"    controls:[\n"
	+"    new OpenLayers.Control.Navigation(),\n"
	+"    new OpenLayers.Control.PanZoomBar(),\n"
	+"    new OpenLayers.Control.LayerSwitcher(),\n"
	+"    new OpenLayers.Control.Attribution()],\n"
	+"    maxExtent: new OpenLayers.Bounds(-20037508.34,-20037508.34,20037508.34,20037508.34),\n"
	+"    maxResolution: 156543.0399,\n"
	+"    numZoomLevels: 19,\n"
	+"    units: 'm',\n"
	+"    projection: new OpenLayers.Projection(\"EPSG:900913\"),\n"
	+"    displayProjection: new OpenLayers.Projection(\"EPSG:4326\")\n"
	+"    } );\n"
	+" \n"
	+"    // Define the map layer\n"
	+"    // Here we use a predefined layer that will be kept up to date with URL changes\n"
	+"    layerMapnik = new OpenLayers.Layer.OSM.Mapnik(\"Mapnik\");\n"
	+"    map.addLayer(layerMapnik);\n"
	+"    layerCycleMap = new OpenLayers.Layer.OSM.CycleMap(\"CycleMap\");\n"
	+"    map.addLayer(layerCycleMap);\n"
	+"    layerMarkers = new OpenLayers.Layer.Markers(\"Markers\");\n"
	+"    map.addLayer(layerMarkers);\n"
	+" \n";
		for (SelectedGpxFile selectedGpxFile : localList.keySet()) {
			String lname = localList.get(selectedGpxFile);
			out += "        showTrack(map, layerMarkers, \"" + lname + "\", \"" + lname + "\", \"blue\"); \n";
		}
		out += "\n"
	+" \n"
	+"    var lonLat = new OpenLayers.LonLat(lon, lat).transform(new OpenLayers.Projection(\"EPSG:4326\"), map.getProjectionObject());\n"
	+"    map.setCenter(lonLat, zoom);\n"
	+"  }\n"
	+"    \n"
	+"    </script>\n"
	+" \n"
	+"</head>\n"
	+"<!-- body.onload is called once the page is loaded (call the 'init' function) -->\n"
	+"<body onload=\"init();\">\n"
	+"  <!-- define a DIV into which the map will appear. Make it take up the whole window -->\n"
	+"  <div style=\"width:100%; height:100%\" id=\"map\"></div>\n"
	+"</body>\n"
	+"</html>\n"
	+"";
		return out;
	}

}
