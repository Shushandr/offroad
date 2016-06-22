package net.osmand.plus.views;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.routing.RoutingHelper;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.R;
import net.sourceforge.offroad.actions.NavigationRotationAction;
import net.sourceforge.offroad.ui.DirectOffroadLayer;
import net.sourceforge.offroad.ui.Drawable;
import net.sourceforge.offroad.ui.OsmBitmapPanel;
import net.sourceforge.offroad.ui.RoundButton;

public class MapControlsLayer extends OsmandMapLayer implements DirectOffroadLayer{

	private static final int TIMEOUT_TO_SHOW_BUTTONS = 7000;
	public static final int REQUEST_ADDRESS_SELECT = 2;
	private static final int REQUEST_LOCATION_FOR_NAVIGATION_PERMISSION = 200;

	public MapHudButton createHudButton(JButton iv, String resId) {
		MapHudButton mc = new MapHudButton();
		mc.iv = iv;
		mc.resId = resId;
		return mc;
	}

	private List<MapHudButton> controls = new ArrayList<>();
	private final OsmBitmapPanel mapActivity;
	private Color shadowColor = null;
	// private RulerControl rulerControl;
	// private List<MapControls> allControls = new ArrayList<MapControls>();

//	private SeekBar transparencyBar;
//	private LinearLayout transparencyBarLayout;
//	private static CommonPreference<Integer> settingsToTransparency;
//	private boolean isTransparencyBarEnabled = true;
	private OsmandSettings settings;

//	private MapRouteInfoMenu mapRouteInfoMenu;
	private MapHudButton backToLocationControl;
	private MapHudButton menuControl;
	private MapHudButton compassHud;
	private float cachedRotate = 0;
	private BufferedImage appModeIcon;
//	private TextView zoomText;
//	private OsmBitmapPanel mapView;
	private OsmWindow app;
//	private View mapAppModeShadow;
	private MapHudButton routePlanningBtn;
	private long touchEvent;
	private MapHudButton mapZoomOut;
	private MapHudButton mapZoomIn;
	private MapHudButton layersHud;
	private long lastZoom;
	private boolean hasTargets;

	public MapControlsLayer(OsmBitmapPanel activity) {
		this.mapActivity = activity;
		app = activity.getMyApplication();
		settings = activity.getMyApplication().getSettings();
//		mapView = mapActivity.getMapView();
	}

//	public MapRouteInfoMenu getMapRouteInfoMenu() {
//		return mapRouteInfoMenu;
//	}
//
	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void initLayer(final OsmBitmapPanel view) {
		initTopControls();
//		initTransparencyBar();
//		initZooms();
		initDasboardRelatedControls();
		updateControls(view.copyCurrentTileBox(), null);
	}

	public void initDasboardRelatedControls() {
		initControls();
		initRouteControls();
	}

	private class CompassDrawable extends Drawable {

		private Drawable original;

		public CompassDrawable(Drawable original) {
			this.original = original;
		}

		@Override
		public void draw(Graphics2D canvas) {
//			canvas.save();
			canvas.rotate(cachedRotate, getIntrinsicWidth() / 2, getIntrinsicHeight() / 2);
			original.draw(canvas);
//			canvas.restore();
		}

		@Override
		public int getMinimumHeight() {
			return original.getMinimumHeight();
		}

		@Override
		public int getMinimumWidth() {
			return original.getMinimumWidth();
		}

		@Override
		public int getIntrinsicHeight() {
			return original.getIntrinsicHeight();
		}

		@Override
		public int getIntrinsicWidth() {
			return original.getIntrinsicWidth();
		}

		@Override
		public void setChangingConfigurations(int configs) {
			super.setChangingConfigurations(configs);
			original.setChangingConfigurations(configs);
		}

		@Override
		public void setBounds(int left, int top, int right, int bottom) {
			super.setBounds(left, top, right, bottom);
			original.setBounds(left, top, right, bottom);
		}

		@Override
		public void setAlpha(int alpha) {
			original.setAlpha(alpha);
		}

//		@Override
//		public void setColorFilter(ColorFilter cf) {
//			original.setColorFilter(cf);
//		}

		@Override
		public int getOpacity() {
			return original.getOpacity();
		}
	}

	private void initTopControls() {
//		View configureMap = mapActivity.findViewById(R.id.map_layers_button);
//		layersHud = createHudButton(configureMap, "map_layer_dark")
//				.setIconsId("map_layer_dark", "map_layer_night")
//				.setBg("btn_inset_circle_trans", "btn_inset_circle_night");
//		controls.add(layersHud);
//		configureMap.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				MapActivity.clearPrevActivityIntent();
//				mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_MAP);
//			}
//		});

		JButton compass = mapActivity.findViewById(R.id.map_compass_button);
		compassHud = createHudButton(compass, "map_compass").setIconColorId(R.color.color_black).
				setBg("map_bt_round_2_shadow", R.color.map_widget_light_trans, "btn_inset_circle_night", R.color.map_widget_dark);
		compassHud.compass = true;
		controls.add(compassHud);
		NavigationRotationAction rotationAction = new NavigationRotationAction(app);
		rotationAction.setAbsolute(0);
		compass.addActionListener(rotationAction);
	}

	private void initRouteControls() {
//		mapRouteInfoMenu = new MapRouteInfoMenu(mapActivity, this);
	}

//	public void updateRouteButtons(View main, boolean routeInfo) {
//		boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
//		BufferedImage cancelRouteButton = (BufferedImage) main.findViewById(R.id.map_cancel_route_button);
//		cancelRouteButton.setImageDrawable(app.getIconsCache().getContentIcon("map_action_cancel", !nightMode));
//		AndroidUtils.setBackground(mapActivity, cancelRouteButton, nightMode, "dashboard_button_light", "dashboard_button_dark");
//		cancelRouteButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				clickRouteCancel();
//			}
//		});
//
//		BufferedImage waypointsButton = (BufferedImage) main.findViewById(R.id.map_waypoints_route_button);
//		waypointsButton.setImageDrawable(app.getIconsCache().getContentIcon("map_action_waypoint", !nightMode));
//		AndroidUtils.setBackground(mapActivity, waypointsButton, nightMode, "dashboard_button_light", "dashboard_button_dark");
//		waypointsButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				clickRouteWaypoints();
//			}
//		});
//
//		BufferedImage options = (BufferedImage) main.findViewById(R.id.map_options_route_button);
//		options.setImageDrawable(!routeInfo ? app.getIconsCache().getIcon("map_action_settings",
//				R.color.osmand_orange) : app.getIconsCache().getContentIcon("map_action_settings", !nightMode));
//		AndroidUtils.setBackground(mapActivity, options, nightMode, "dashboard_button_light", "dashboard_button_dark");
//		options.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				clickRouteParams();
//			}
//		});
//
//		TextView routeGoButton = (TextView) main.findViewById(R.id.map_go_route_button);
//		routeGoButton.setCompoundDrawablesWithIntrinsicBounds(app.getIconsCache().getIcon("map_start_navigation", R.color.color_myloc_distance), null, null, null);
//		routeGoButton.setText(mapActivity.getString(R.string.shared_string_go));
//		AndroidUtils.setTextSecondaryColor(mapActivity, routeGoButton, nightMode);
//		AndroidUtils.setBackground(mapActivity, routeGoButton, nightMode, "dashboard_button_light", "dashboard_button_dark");
//		routeGoButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				clickRouteGo();
//			}
//		});
//	}

	public void setControlsClickable(boolean clickable) {
		for (MapHudButton mb : controls) {
			mb.iv.setEnabled(clickable);
		}
	}

	private TargetPointsHelper getTargets() {
		return mapActivity.getMyApplication().getTargetPointsHelper();
	}

//	protected void clickRouteParams() {
//		mapActivity.getMapActions().openRoutePreferencesDialog();
//	}
//
//	protected void clickRouteWaypoints() {
//		if (getTargets().checkPointToNavigateShort()) {
//			mapActivity.getMapActions().openIntermediatePointsDialog();
//		}
//	}
//
//	protected void clickRouteCancel() {
//		mapRouteInfoMenu.hide();
//		if (mapActivity.getRoutingHelper().isFollowingMode()) {
//			mapActivity.getMapActions().stopNavigationActionConfirm();
//		} else {
//			mapActivity.getMapActions().stopNavigationWithoutConfirm();
//		}
//	}
//
//	protected void clickRouteGo() {
//		if (app.getTargetPointsHelper().getPointToNavigate() != null) {
//			mapRouteInfoMenu.hide();
//		}
//		startNavigation();
//	}
//
//	public void showRouteInfoControlDialog() {
//		mapRouteInfoMenu.showHideMenu();
//	}
//
//	public void showDialog() {
//		mapRouteInfoMenu.setShowMenu();
//	}
//
	private void initControls() {
//		View backToLocation = mapActivity.findViewById(R.id.map_my_location_button);
//		backToLocationControl = createHudButton(backToLocation, "map_my_location")
//				.setBg("btn_circle_blue");
//		controls.add(backToLocationControl);
//
//		backToLocation.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				if (OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
//					mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
//				} else {
//					ActivityCompat.requestPermissions(mapActivity,
//							new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//							OsmAndLocationProvider.REQUEST_LOCATION_PERMISSION);
//				}
//			}
//		});
//		controls.add(createHudButton(mapActivity.findViewById(R.id.map_app_mode_shadow), 0).setBg(
//				"btn_round_trans", "btn_round_transparent"));
//		View backToMenuButton = mapActivity.findViewById(R.id.map_menu_button);
//
//		final boolean dash = settings.SHOW_DASHBOARD_ON_MAP_SCREEN.get();
//		menuControl = createHudButton(backToMenuButton,
//				!dash ? "map_drawer" : "map_dashboard").setBg(
//				"btn_round", "btn_round_night");
//		controls.add(menuControl);
//		backToMenuButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				MapActivity.clearPrevActivityIntent();
//				if (dash) {
//					mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.DASHBOARD);
//				} else {
//					mapActivity.openDrawer();
//				}
//			}
//		});
//		mapAppModeShadow = mapActivity.findViewById(R.id.map_app_mode_shadow);
//		mapAppModeShadow.setOnClickListener(new View.OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				onApplicationModePress(v);
//			}
//		});
//		appModeIcon = (BufferedImage) mapActivity.findViewById(R.id.map_app_mode_icon);
//		zoomText = (TextView) mapActivity.findViewById(R.id.map_app_mode_text);
//
//		View routePlanButton = mapActivity.findViewById(R.id.map_route_info_button);
//		routePlanningBtn = createHudButton(routePlanButton, "map_directions").setBg(
//				"btn_round", "btn_round_night");
//		controls.add(routePlanningBtn);
//		routePlanButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				doRoute(false);
//			}
//		});
	}

//	public void doRoute(boolean hasTargets) {
//		this.hasTargets = hasTargets;
//		if (OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
//			onNavigationClick();
//		} else {
//			ActivityCompat.requestPermissions(mapActivity,
//					new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//					REQUEST_LOCATION_FOR_NAVIGATION_PERMISSION);
//		}
//	}
//
//	public void doNavigate() {
//		mapRouteInfoMenu.hide();
//		startNavigation();
//	}
//
//	private void onNavigationClick() {
//		MapActivity.clearPrevActivityIntent();
//		RoutingHelper routingHelper = mapActivity.getRoutingHelper();
//		if (!routingHelper.isFollowingMode() && !routingHelper.isRoutePlanningMode()) {
//			if (settings.USE_MAP_MARKERS.get() && !hasTargets) {
//				getTargets().restoreTargetPoints(false);
//				if (getTargets().getPointToNavigate() == null) {
//					mapActivity.getMapActions().setFirstMapMarkerAsTarget();
//				}
//			}
//			TargetPoint start = getTargets().getPointToStart();
//			if (start != null) {
//				mapActivity.getMapActions().enterRoutePlanningMode(
//						new LatLon(start.getLatitude(), start.getLongitude()), start.getOriginalPointDescription());
//			} else {
//				mapActivity.getMapActions().enterRoutePlanningMode(null, null);
//			}
//		} else {
//			showRouteInfoControlDialog();
//		}
//		hasTargets = false;
//	}
//
//
//	public void switchToRouteFollowingLayout() {
//		touchEvent = 0;
//		mapActivity.getMyApplication().getRoutingHelper().setRoutePlanningMode(false);
//		mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
//		mapActivity.refreshMap();
//	}
//
//	public boolean switchToRoutePlanningLayout() {
//		if (!mapActivity.getRoutingHelper().isRoutePlanningMode() && mapActivity.getRoutingHelper().isFollowingMode()) {
//			mapActivity.getRoutingHelper().setRoutePlanningMode(true);
//			mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
//			mapActivity.refreshMap();
//			return true;
//		}
//		return false;
//	}

//	private void initZooms() {
//		final OsmBitmapPanel view = mapActivity.getMapView();
//		View zoomInButton = mapActivity.findViewById(R.id.map_zoom_in_button);
//		mapZoomIn = createHudButton(zoomInButton, "map_zoom_in").
//				setIconsId("map_zoom_in", "map_zoom_in_night").setRoundTransparent();
//		controls.add(mapZoomIn);
//		zoomInButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				if (view.isZooming()) {
//					mapActivity.changeZoom(2, System.currentTimeMillis());
//				} else {
//					mapActivity.changeZoom(1, System.currentTimeMillis());
//				}
//
//			}
//		});
//		final View.OnLongClickListener listener = MapControlsLayer.getOnClickMagnifierListener(view);
//		zoomInButton.setOnLongClickListener(listener);
//		View zoomOutButton = mapActivity.findViewById(R.id.map_zoom_out_button);
//		mapZoomOut = createHudButton(zoomOutButton, "map_zoom_out").
//				setIconsId("map_zoom_out", "map_zoom_out_night").setRoundTransparent();
//		controls.add(mapZoomOut);
//		zoomOutButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				mapActivity.changeZoom(-1, System.currentTimeMillis());
//			}
//		});
//		zoomOutButton.setOnLongClickListener(listener);
//	}
//
//	public void startNavigation() {
//		OsmWindow app = mapActivity.getMyApplication();
//		RoutingHelper routingHelper = app.getRoutingHelper();
//		if (routingHelper.isFollowingMode()) {
//			switchToRouteFollowingLayout();
//			if (app.getSettings().APPLICATION_MODE.get() != routingHelper.getAppMode()) {
//				app.getSettings().APPLICATION_MODE.set(routingHelper.getAppMode());
//			}
//		} else {
//			if (!app.getTargetPointsHelper().checkPointToNavigateShort()) {
//				mapRouteInfoMenu.show();
//			} else {
//				touchEvent = 0;
//				app.getSettings().APPLICATION_MODE.set(routingHelper.getAppMode());
//				mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
//				app.getSettings().FOLLOW_THE_ROUTE.set(true);
//				routingHelper.setFollowingMode(true);
//				routingHelper.setRoutePlanningMode(false);
//				mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
//				app.getRoutingHelper().notifyIfRouteIsCalculated();
//				routingHelper.setCurrentLocation(app.getLocationProvider().getLastKnownLocation(), false);
//			}
//		}
//	}

	@Override
	public void destroyLayer() {
		controls.clear();
	}

	@Override
	public void onDraw(Graphics2D canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		updateControls(tileBox, nightMode);
	}

	private void updateControls(RotatedTileBox tileBox, DrawSettings nightMode) {
		boolean isNight = nightMode != null && nightMode.isNightMode();
		Color shadw = (isNight ? R.color.color_transparent : R.color.color_white);
		Color textColor = isNight ? R.color.widgettext_night: R.color.color_black;
		if (shadowColor != shadw) {
			shadowColor = shadw;
			// TODOnightMode
			// updatextColor(textColor, shadw, rulerControl, zoomControls, mapMenuControls);
		}
		// default buttons
		boolean routePlanningMode = false;
		RoutingHelper rh = mapActivity.getApplication().getRoutingHelper();
		if (rh.isRoutePlanningMode()) {
			routePlanningMode = true;
		} else if ((rh.isRouteCalculated() || rh.isRouteBeingCalculated()) && !rh.isFollowingMode()) {
			routePlanningMode = true;
		}
		boolean routeFollowingMode = !routePlanningMode && rh.isFollowingMode();
		boolean dialogOpened = false; //mapRouteInfoMenu.isVisible();
		boolean showRouteCalculationControls = routePlanningMode ||
				((System.currentTimeMillis() - touchEvent < TIMEOUT_TO_SHOW_BUTTONS) && routeFollowingMode);
//		updateMyLocation(rh, dialogOpened);
		boolean showButtons = (showRouteCalculationControls || !routeFollowingMode);
		//routePlanningBtn.setIconResId(routeFollowingMode ? "ic_action_gabout_dark" : "map_directions");
//		if (rh.isFollowingMode()) {
//			routePlanningBtn.setIconResId("map_start_navigation");
//			routePlanningBtn.setIconColorId(R.color.color_myloc_distance);
//		} else if (routePlanningMode) {
//			routePlanningBtn.setIconResId("map_directions");
//			routePlanningBtn.setIconColorId(R.color.color_myloc_distance);
//		} else {
//			routePlanningBtn.setIconResId("map_directions");
//			routePlanningBtn.resetIconColors();
//		}
//		routePlanningBtn.updateVisibility(showButtons);
//		menuControl.updateVisibility(showButtons);
//
//		mapZoomIn.updateVisibility(!dialogOpened);
//		mapZoomOut.updateVisibility(!dialogOpened);
		compassHud.updateVisibility(!dialogOpened);
//		layersHud.updateVisibility(!dialogOpened);

//		if (routePlanningMode || routeFollowingMode) {
//			mapAppModeShadow.setVisibility(View.GONE);
//		} else {
//			if (mapView.isZooming()) {
//				lastZoom = System.currentTimeMillis();
//			}
//			mapAppModeShadow.setVisibility(View.VISIBLE);
//			//if (!mapView.isZooming() || !OsmandPlugin.isDevelopment()) {
//			if ((System.currentTimeMillis() - lastZoom > 1000) || !OsmandPlugin.isDevelopment()) {
//				zoomText.setVisibility(View.GONE);
//				appModeIcon.setVisibility(View.VISIBLE);
//				appModeIcon.setImageDrawable(
//						app.getIconsCache().getIcon(
//								settings.getApplicationMode().getSmallIconDark(), !isNight));
//			} else {
//				appModeIcon.setVisibility(View.GONE);
//				zoomText.setVisibility(View.VISIBLE);
//				zoomText.setTextColor(textColor);
//				zoomText.setText(getZoomLevel(tileBox));
//			}
//		}
//
//		mapRouteInfoMenu.setVisible(showRouteCalculationControls);
		updateCompass(isNight);

		for (MapHudButton mc : controls) {
			mc.update(mapActivity.getMyApplication(), isNight);
		}
	}

	private void updateCompass(boolean isNight) {
		float mapRotate = mapActivity.copyCurrentTileBox().getRotate();
		if (mapRotate != cachedRotate) {
			cachedRotate = mapRotate;
			// Apply animation to image view
			((RoundButton)compassHud.iv).setRotate(cachedRotate);
			compassHud.iv.invalidate();
		}
		if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_NONE) {
			compassHud.setIconResId(isNight ? "map_compass_niu_white" : "map_compass_niu");
		} else if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING) {
			compassHud.setIconResId(isNight ? "map_compass_bearing_white" : "map_compass_bearing");
		} else {
			compassHud.setIconResId(isNight ? "map_compass_white" : "map_compass");
		}
	}

//	private void updateMyLocation(RoutingHelper rh, boolean dialogOpened) {
//		boolean enabled = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation() != null;
//		boolean tracked = mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();
//
//		if (!enabled) {
//			backToLocationControl.setBg("btn_circle", "btn_circle_night");
//			backToLocationControl.setIconColorId(R.color.icon_color, 0);
//		} else if (tracked) {
//			backToLocationControl.setBg("btn_circle", "btn_circle_night");
//			backToLocationControl.setIconColorId(R.color.color_myloc_distance);
//		} else {
//			backToLocationControl.setIconColorId(0);
//			backToLocationControl.setBg("btn_circle_blue");
//		}
//		boolean visible = !(tracked && rh.isFollowingMode());
//		backToLocationControl.updateVisibility(visible && !dialogOpened);
//	}
//

	public boolean onSingleTap(Point2D point, RotatedTileBox tileBox) {
//		return mapRouteInfoMenu.onSingleTap(point, tileBox);
		return true;
	}

//	@Override
//	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {
//		touchEvent = System.currentTimeMillis();
//		RoutingHelper rh = mapActivity.getRoutingHelper();
//		if (rh.isFollowingMode()) {
//			mapActivity.refreshMap();
//		}
//		return false;
//	}
//
//	// /////////////// Transparency bar /////////////////////////
//	private void initTransparencyBar() {
//		transparencyBarLayout = (LinearLayout) mapActivity.findViewById(R.id.map_transparency_layout);
//		transparencyBar = (SeekBar) mapActivity.findViewById(R.id.map_transparency_seekbar);
//		transparencyBar.setMax(255);
//		if (settingsToTransparency != null) {
//			transparencyBar.setProgress(settingsToTransparency.get());
//			transparencyBarLayout.setVisibility(View.VISIBLE);
//		} else {
//			transparencyBarLayout.setVisibility(View.GONE);
//		}
//		transparencyBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//
//			@Override
//			public void onStopTrackingTouch(SeekBar seekBar) {
//			}
//
//			@Override
//			public void onStartTrackingTouch(SeekBar seekBar) {
//			}
//
//			@Override
//			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//				if (settingsToTransparency != null) {
//					settingsToTransparency.set(progress);
//					mapActivity.getMapView().refreshMap();
//				}
//			}
//		});
//		ImageButton imageButton = (ImageButton) mapActivity.findViewById(R.id.map_transparency_hide);
//		imageButton.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				transparencyBarLayout.setVisibility(View.GONE);
//				settings.SHOW_LAYER_TRANSPARENCY_SEEKBAR.set(false);
//				hideTransparencyBar(settingsToTransparency);
//			}
//		});
//	}
//
//	public void showTransparencyBar(CommonPreference<Integer> transparenPreference) {
//		if (MapControlsLayer.settingsToTransparency != transparenPreference) {
//			MapControlsLayer.settingsToTransparency = transparenPreference;
//			if (isTransparencyBarEnabled) {
//				transparencyBarLayout.setVisibility(View.VISIBLE);
//			}
//			transparencyBar.setProgress(transparenPreference.get());
//		}
//	}
//
//	public void hideTransparencyBar(CommonPreference<Integer> transparentPreference) {
//		if (settingsToTransparency == transparentPreference) {
//			transparencyBarLayout.setVisibility(View.GONE);
//			settingsToTransparency = null;
//		}
//	}
//
//	public void setTransparencyBarEnabled(boolean isTransparencyBarEnabled) {
//		this.isTransparencyBarEnabled = isTransparencyBarEnabled;
//		if (settingsToTransparency != null) {
//			if(isTransparencyBarEnabled) {
//				transparencyBarLayout.setVisibility(View.VISIBLE);
//			} else {
//				transparencyBarLayout.setVisibility(View.GONE);
//			}
//		}
//	}
//
//	public boolean isTransparencyBarInitialized() {
//		return settingsToTransparency != null;
//	}
//
	private class MapHudButton {
		JButton iv;
		String bgDark;
		String bgLight;
		String resId;
		String resLightId;
		String resDarkId;
		Color resClrLight = R.color.icon_color;
		Color resClrDark = R.color.color_black;


		boolean nightMode = false;
		boolean f = true;
		boolean compass;
		private Color bgLightColor;
		private Color bgDarkColor;

		public MapHudButton setRoundTransparent() {
			setBg("map_bt_round_1_shadow", R.color.map_widget_light_trans, "map_bt_round_1_shadow", R.color.map_widget_dark);
			return this;
		}


		public MapHudButton setBg(String dayBg, Color pColorDay, String nightBg, Color pColorNight) {
			if (nightBg.equals(bgDark) && bgLight.equals(dayBg)) {
				return this;
			}
			bgDark = nightBg;
			bgLight = dayBg;
			bgLightColor = pColorDay;
			bgDarkColor = pColorNight;
			f = true;
			return this;
		}

		public boolean updateVisibility(boolean visible) {
			if (visible != (iv.isVisible())) {
				iv.setVisible(visible);
				iv.invalidate();
				return true;
			}
			return false;
		}

		public MapHudButton setBg(String bg) {
			if (bg.equals(bgDark) && bg.equals(bgLight)) {
				return this;
			}
			bgDark = bg;
			bgLight = bg;
			f = true;
			return this;
		}

		public boolean setIconResId(String resId) {
			if (resId.equals(this.resId)) {
				return false;
			}
			this.resId = resId;
			f = true;
			return true;
		}

		public boolean resetIconColors() {
			if (R.color.icon_color.equals(resClrLight) && R.color.color_black.equals(resClrDark)) {
				return false;
			}
			resClrLight = R.color.icon_color;
			resClrDark = R.color.color_black;
			f = true;
			return true;
		}

		public MapHudButton setIconColorId(Color clr) {
			if (clr.equals(resClrLight) && clr.equals(resClrDark)) {
				return this;
			}
			resClrLight = clr;
			resClrDark = clr;
			f = true;
			return this;
		}

		public MapHudButton setIconsId(String icnLight, String icnDark) {
			if (icnLight.equals(resLightId) && icnDark.equals(resDarkId)) {
				return this;
			}
			resLightId = icnLight;
			resDarkId = icnDark;
			f = true;
			return this;
		}

		public MapHudButton setIconColorId(Color clrLight, Color clrDark) {
			if (clrLight.equals(resClrLight) && clrDark.equals(resClrDark)) {
				return this;
			}
			resClrLight = clrLight;
			resClrDark = clrDark;
			f = true;
			return this;
		}

		public void update(OsmWindow ctx, boolean night) {
			if (nightMode == night && !f) {
				return;
			}
			f = false;
			nightMode = night;
//			if (bgDark != null && bgLight != null) {
//				if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
//					iv.setBackground(ctx.getResources().getDrawable(night ? bgDark : bgLight,
//							mapActivity.getTheme()));
//				} else {
//					iv.setBackgroundDrawable(ctx.getResources().getDrawable(night ? bgDark : bgLight));
			if (compass) {
				RoundButton roundButton = (RoundButton) iv;
				roundButton.setBackgroundIcon(readImage(night ? bgDark : bgLight, mapActivity));
				roundButton.setBackground(night?bgDarkColor:bgLightColor);
			} 

//				}
//			}
//			Drawable d = null;
					
			BufferedImage d = null;
			if (resDarkId != null && nightMode) {
				d  = readImage(resDarkId, mapActivity);
			} else if (resLightId != null && !nightMode) {
				d = readImage(resLightId, mapActivity);
			} else if (resId != null) {
				d = readImage(resId, mapActivity); // FIXME: Color, nightMode ? resClrDark : resClrLight);
			}

			if (compass) {
				RoundButton roundButton = (RoundButton) iv;
				roundButton.setForegroundIcon(d);
			} else {
				((JButton) iv).setIcon(new ImageIcon(d));
			}
		}

	}

//	private void onApplicationModePress(View v) {
//		final QuickAction mQuickAction = new QuickAction(v);
//		mQuickAction.setOnAnchorOnTop(true);
//		List<ApplicationMode> vls = ApplicationMode.values(mapActivity.getMyApplication().getSettings());
//		final ApplicationMode[] modes = vls.toArray(new ApplicationMode[vls.size()]);
//		Drawable[] icons = new Drawable[vls.size()];
//		int[] values = new int[vls.size()];
//		for (int k = 0; k < modes.length; k++) {
//			icons[k] = app.getIconsCache().getIcon(modes[k].getSmallIconDark(), R.color.icon_color);
//			values[k] = modes[k].getStringResource();
//		}
//		for (int i = 0; i < modes.length; i++) {
//			final ActionItem action = new ActionItem();
//			action.setTitle(mapActivity.getResources().getString(values[i]));
//			action.setIcon(icons[i]);
//			final int j = i;
//			action.setOnClickListener(new OnClickListener() {
//				@Override
//				public void onClick(View v) {
//					mapActivity.getMyApplication().getSettings().APPLICATION_MODE.set(modes[j]);
//					mQuickAction.dismiss();
//				}
//			});
//			mQuickAction.addActionItem(action);
//		}
//		mQuickAction.setAnimStyle(QuickAction.ANIM_AUTO);
//		mQuickAction.show();
//	}
//
//	private String getZoomLevel(@NonNull RotatedTileBox tb) {
//		String zoomText = tb.getZoom() + "";
//		double frac = tb.getMapDensity();
//		if (frac != 0) {
//			int ifrac = (int) (frac * 10);
//			zoomText += " ";
//			zoomText += Math.abs(ifrac) / 10;
//			if (ifrac % 10 != 0) {
//				zoomText += "." + Math.abs(ifrac) % 10;
//			}
//		}
//		return zoomText;
//	}

//	public static View.OnLongClickListener getOnClickMagnifierListener(final OsmBitmapPanel view) {
//		return new View.OnLongClickListener() {
//
//			@Override
//			public boolean onLongClick(View notUseCouldBeNull) {
//				final OsmandSettings.OsmandPreference<Float> mapDensity = view.getSettings().MAP_DENSITY;
//				final AlertDialog.Builder bld = new AlertDialog.Builder(view.getContext());
//				int p = (int) (mapDensity.get() * 100);
//				final TIntArrayList tlist = new TIntArrayList(new int[]{20, 25, 33, 50, 75, 100, 150, 200, 300, 400});
//				final List<String> values = new ArrayList<>();
//				int i = -1;
//				for (int k = 0; k <= tlist.size(); k++) {
//					final boolean end = k == tlist.size();
//					if (i == -1) {
//						if ((end || p < tlist.get(k))) {
//							values.add(p + " %");
//							i = k;
//						} else if (p == tlist.get(k)) {
//							i = k;
//						}
//
//					}
//					if (k < tlist.size()) {
//						values.add(tlist.get(k) + " %");
//					}
//				}
//				if (values.size() != tlist.size()) {
//					tlist.insert(i, p);
//				}
//
//				bld.setTitle(R.string.map_magnifier);
//				bld.setSingleChoiceItems(values.toArray(new String[values.size()]), i,
//						new DialogInterface.OnClickListener() {
//							@Override
//							public void onClick(DialogInterface dialog, int which) {
//								int p = tlist.get(which);
//								mapDensity.set(p / 100.0f);
//								view.setComplexZoom(view.getZoom(), view.getSettingsMapDensity());
//								MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
//								if (mapContext != null) {
//									mapContext.updateMapSettings();
//								}
//								dialog.dismiss();
//							}
//						});
//				bld.show();
//				return true;
//			}
//		};
//	}
//
//	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//		if (requestCode == REQUEST_ADDRESS_SELECT && resultCode == SearchAddressFragment.SELECT_ADDRESS_POINT_RESULT_OK) {
//			String name = data.getStringExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_INTENT_KEY);
//			boolean target = data.getBooleanExtra(MapRouteInfoMenu.TARGET_SELECT, true);
//			LatLon latLon = new LatLon(
//					data.getDoubleExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_LAT, 0),
//					data.getDoubleExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_LON, 0));
//			if (name != null) {
//				mapRouteInfoMenu.selectAddress(name, latLon, target);
//			} else {
//				mapRouteInfoMenu.selectAddress("", latLon, target);
//			}
//		}
//	}
//
//	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//		if (requestCode == REQUEST_LOCATION_FOR_NAVIGATION_PERMISSION
//				&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//			onNavigationClick();
//		}
//	}
}
