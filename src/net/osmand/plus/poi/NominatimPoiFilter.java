package net.osmand.plus.poi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.util.MapUtils;
import net.sf.junidecode.Junidecode;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.R;


public class NominatimPoiFilter extends PoiUIFilter {

	private static final String FILTER_ID = "name_finder"; //$NON-NLS-1$
	private static final Log log = PlatformUtil.getLog(NominatimPoiFilter.class);
	private static final int LIMIT = 300;
	
	private String lastError = ""; //$NON-NLS-1$
	private boolean addressQuery;
	
	public NominatimPoiFilter(OsmWindow application, boolean addressQuery) {
		super(application);
		this.addressQuery = addressQuery;
		this.name =
				application.getString(R.string.poi_filter_nominatim); //$NON-NLS-1$
		if(addressQuery) {
			this.name += " - " + application.getString(R.string.shared_string_address); 
		} else {
			this.name += " - " + application.getString(R.string.shared_string_places);
		}
		if(addressQuery) {
			this.distanceToSearchValues = new double[] {500};
		} else {
			this.distanceToSearchValues = new double[] {1, 2, 5, 10, 20, 50, 100, 200, 500 };
		}
		this.filterId = FILTER_ID + (addressQuery ? "_address" : "_places");
	}
	
	public boolean isPlacesQuery() {
		return !addressQuery;
	}
	
	@Override
	public boolean isAutomaticallyIncreaseSearch() {
		return false;
	}
	
	@Override
	protected List<Amenity> searchAmenitiesInternal(double lat, double lon, double topLatitude,
			double bottomLatitude, double leftLongitude, double rightLongitude, ResultMatcher<Amenity> matcher) {
		String NOMINATIM_API;
		// FIXME: Which is working??
			NOMINATIM_API = "https://nominatim.openstreetmap.org/search/";
//			NOMINATIM_API = "http://nominatim.openstreetmap.org/search/";
		currentSearchResult = new ArrayList<Amenity>();
		String filter = getFilterByName();
		if (filter == null) return currentSearchResult;
		String viewbox = "viewboxlbrt="+((float) leftLongitude)+","+((float) bottomLatitude)+","+((float) rightLongitude)+","+((float) topLatitude);
		try {
			lastError = "";
			String urlq ;
			if(addressQuery) {
				urlq = NOMINATIM_API + "?format=xml&addressdetails=0&accept-language="+ Locale.getDefault().getLanguage() 
						+ "&q=" + URLEncoder.encode(filter, "UTF-8");
			} else {
				urlq = NOMINATIM_API + URLEncoder.encode(filter, "UTF-8") + "?format=xml&addressdetails=1&limit=" + LIMIT
						+ "&bounded=1&" + viewbox;	
			}
			log.info(urlq);
			URLConnection connection = NetworkUtils.getHttpURLConnection(urlq); //$NON-NLS-1$
			InputStream stream = connection.getInputStream();
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			parser.setInput(stream, "UTF-8"); //$NON-NLS-1$
			int eventType;
			int namedDepth = 0;
			Amenity a = null;
			MapPoiTypes poiTypes = ((OsmWindow) getApplication()).getPoiTypes();
			while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					if (parser.getName().equals("searchresults")) { //$NON-NLS-1$
						String err = parser.getAttributeValue("", "error"); //$NON-NLS-1$ //$NON-NLS-2$
						if (err != null && err.length() > 0) {
							lastError = err;
							stream.close();
							return currentSearchResult;
						}
					}
					if (parser.getName().equals("place")) { //$NON-NLS-1$
						namedDepth++;
						if (namedDepth == 1) {
							try {
								a = new Amenity();
								a.setLocation(Double.parseDouble(parser.getAttributeValue("", "lat")), //$NON-NLS-1$//$NON-NLS-2$
										Double.parseDouble(parser.getAttributeValue("", "lon"))); //$NON-NLS-1$//$NON-NLS-2$
								a.setId(Long.parseLong(parser.getAttributeValue("", "place_id"))); //$NON-NLS-1$ //$NON-NLS-2$
								String name = parser.getAttributeValue("", "display_name"); //$NON-NLS-1$//$NON-NLS-2$
								a.setName(name);
								a.setEnName(Junidecode.unidecode(name));
								a.setType(poiTypes.getOtherPoiCategory());
								a.setSubType(parser.getAttributeValue("", "type")); //$NON-NLS-1$//$NON-NLS-2$
								if (matcher == null || matcher.publish(a)) {
									currentSearchResult.add(a);
								}
							} catch (NumberFormatException e) {
								log.info("Invalid attributes", e); //$NON-NLS-1$
							}
						}
					} else if (a != null && parser.getName().equals(a.getSubType())) {
						if (parser.next() == XmlPullParser.TEXT) {
							String name = parser.getText();
							if (name != null) {
								a.setName(name);
								a.setEnName(Junidecode.unidecode(name));
							}
						}
					}
				} else if (eventType == XmlPullParser.END_TAG) {
					if (parser.getName().equals("place")) { //$NON-NLS-1$
						namedDepth--;
						if (namedDepth == 0) {
							a = null;
						}
					}
				}
			}
			stream.close();
		} catch (IOException e) {
			log.error("Error loading name finder poi", e); //$NON-NLS-1$
			lastError = getApplication().getString(R.string.shared_string_io_error); //$NON-NLS-1$
		} catch (XmlPullParserException e) {
			log.error("Error parsing name finder poi", e); //$NON-NLS-1$
			lastError = getApplication().getString(R.string.shared_string_io_error); //$NON-NLS-1$
		}
		MapUtils.sortListOfMapObject(currentSearchResult, lat, lon);
		return currentSearchResult;
	}
	
	public String getLastError() {
		return lastError;
	}
	

}
