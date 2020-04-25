

 ██████╗ ███████╗███████╗██████╗  ██████╗  █████╗ ██████╗ 
██╔═══██╗██╔════╝██╔════╝██╔══██╗██╔═══██╗██╔══██╗██╔══██╗
██║   ██║█████╗  █████╗  ██████╔╝██║   ██║███████║██║  ██║
██║   ██║██╔══╝  ██╔══╝  ██╔══██╗██║   ██║██╔══██║██║  ██║
╚██████╔╝██║     ██║     ██║  ██║╚██████╔╝██║  ██║██████╔╝
 ╚═════╝ ╚═╝     ╚═╝     ╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═╝╚═════╝ 
                                                          

An offline map viewer based on OsmAnd.


   -------------------------- GPLv3 ----------------------------------
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    -------------------------------------------------------------------

Version History:

0_6:
============
* Fixed bug where routing might incorrectly report "no enough memory" [sic].
* Removed special font size fix for HiDPI screens, relying
  on Java 9 and later native HiDPI support (testing needed!)
* Much improved smoothness and general experience when dragging the map
* Add button to delete downloads in download dialog
* Generate submenus for favorite groups that contain one or more '/'.
  This allows sorting favorites in a hierarchy instead of just flat groups.
* Allow loading resources from files when there is no
  jar (easier development with some IDEs)

0_5:
============
* Fixed a bug of 0.4 under windows not showing POI icons

0_4:
============
* Fixed a bug of 0.3 under windows
* Support modifying String type render options (thanks for the patch)
* Fixed storage of render options

0_3:
============
* Moved maps to ~/.OffRoad/maps (you have to **move your maps** or to re-download)
* Improved the map-server
* Added jaxb to be compatible with java > 9.

0_2:
============
* Added polylines (use shift+click to create a polyline)
* Measurement of length and area of polylines
* Polylines are stored
* Polylines can be changed by moving the edges

0_1:
============
* First version of elevation correction for GPX files. Using contour line maps, the GPX elevations can be adjusted according to their position. This is useful, if the elevations are not accurate (as recorded by my phone, for example).
* Elevation diagram for routes. 

0_1-Beta4:
============
* Fast Search (Ctrl+Shift+F) added. It searches in the visible map for words. Fuzzy support.
* Export tracks as webpage added.
* Track information window as context menu entry for tracks added
  * Elevation is shown in the track information window as a curve.
* Enabled relief curves in the offline maps.

0_1-Beta3:
============
* If the worldmap is missing, a dialog is displayed.
* Download dialog has a download button, now.
* Icon size is changeable now.
* Camera move for jumps to locations added

0_1-Beta2:
============
* routing results are displayed in the search table, now
* application mode (car, bike, etc.) as view option added.
* debian package created
* optical additions: ruler and compass
* context menus enhanced
* more navigation actions
* copy link (to http://osm.org/) of current selected location to clipboard added

0_1_Beta1:
============
* Perimeter added
* Rendering properties added
* Windows installer added

0_1_Alpha9:
============
* Import and organization of GPX-Tracks added.
* Renderer can be chosen (in the view menu)

0_1_Alpha8:
============
* Favorites added
* Different routing services added
* Background rendering of adjacent maps added 

0_1_Alpha7:
============
* Routing with intermediate points and different types (pedestrian, bicycle, car)
* Thread pool for faster rendering

0_1_Alpha6:
============
* Search bar à la browser added.
	- POI offline search
	- Nominatim address + POI search
* Search result is displayed in a table in the main window
* Wikipedia display added

0_1_Alpha5:
============
* Smaller icons used.

0_1_Alpha4:
============
* POI Layer added.

0_1_Alpha3:
============
* Moved map files to <HOME>/.OffRoad
  - **Downloaded files must be moved**
  - Navigation
  - Internationalization started

0_1_Alpha2:
============
* Corrected class paths
* New feature integrated from OsmAnd:
  - Routing

0_1_Alpha1:
============
* First release with 
  - Download dialog
  - Search dialog for addresses
  - Distance measuring
  - Offline routing
  - Several map animations (zoom, move)
  
