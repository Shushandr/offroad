package net.sourceforge.offroad.actions;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.MapMarkersHelper.MapMarkerChangedListener;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.ui.FavoriteGroupRenderer;

public class AddFavoriteAction extends OffRoadAction {
	private JTextField mNameTextField;
	private JTextField mDescriptionTextField;
	private JComboBox<FavoriteGroup> mComboBox;
	private DefaultComboBoxModel<FavoriteGroup> mComboBoxModel;
	private JCheckBox mNewGroupCheckBox;
	private JTextField mGroupNameTextField;
	private JColorChooser mGroupColorChooser;

	public AddFavoriteAction(OsmWindow pContext, String pName, Icon pIcon) {
		super(pContext, pName, pIcon);
	}

	@Override
	public void actionPerformed(ActionEvent pE) {
		createDialog();
		mDialog.setTitle(getResourceString("offroad.addFavorite"));
		Container contentPane = mDialog.getContentPane();
		GridBagLayout gbl = new GridBagLayout();
		gbl.columnWeights = new double[] { 1.0f };
		gbl.rowWeights = new double[] { 1.0f };
		contentPane.setLayout(gbl);
		int y = 0;
		contentPane.add(new JLabel(getResourceString("offroad.favoriteName")), new GridBagConstraints(0, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		mNameTextField = new JTextField();
		contentPane.add(mNameTextField, new GridBagConstraints(1, y++, 2, 1, 4.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		contentPane.add(new JLabel(getResourceString("offroad.favoriteDescription")), new GridBagConstraints(0, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		mDescriptionTextField = new JTextField();
		contentPane.add(mDescriptionTextField, new GridBagConstraints(1, y++, 2, 1, 4.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		mComboBox = new JComboBox<FavoriteGroup>();
		mComboBoxModel = new DefaultComboBoxModel<FavoriteGroup>();
		for (FavoriteGroup filter : mContext.getFavorites().getFavoriteGroups()) {
			mComboBoxModel.addElement(filter);
		}
		mComboBox.setModel(mComboBoxModel);
		mComboBox.setFocusable(false);
		mComboBox.setRenderer(new FavoriteGroupRenderer());
		contentPane.add(mComboBox, new GridBagConstraints(1, y++, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		contentPane.add(new JLabel(getResourceString("offroad.favoriteNewGroup")), new GridBagConstraints(0, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		mNewGroupCheckBox = new JCheckBox();
		contentPane.add(mNewGroupCheckBox, new GridBagConstraints(1, y++, 2, 1, 4.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		mNewGroupCheckBox.addActionListener(t -> toggleNewGroup());

		contentPane.add(new JLabel(getResourceString("offroad.favoriteGroupName")), new GridBagConstraints(0, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		mGroupNameTextField = new JTextField();
		contentPane.add(mGroupNameTextField, new GridBagConstraints(1, y++, 2, 1, 4.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

		contentPane.add(new JLabel(getResourceString("offroad.favoriteGroupColor")), new GridBagConstraints(0, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		mGroupColorChooser = new JColorChooser();
		contentPane.add(mGroupColorChooser, new GridBagConstraints(1, y++, 2, 1, 4.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		
		
		JButton okButton = new JButton(getResourceString("offroad.addFavoriteOK"));
		contentPane.add(okButton, new GridBagConstraints(2, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 0, 0), 0, 0));
		okButton.addActionListener(t -> terminate(true));
		JButton cancelButton = new JButton(getResourceString("offroad.addFavoriteCancel"));
		cancelButton.addActionListener(t -> terminate(false));
		contentPane.add(cancelButton, new GridBagConstraints(1, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 0, 0), 0, 0));
		toggleNewGroup();
		
		// fill with values:
		
		final MapMarkersHelper helper = mContext.getMapMarkersHelper();
		LatLon position = mContext.getCursorPosition();
		PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
		helper.addListener(new MapMarkerChangedListener() {
			
			@Override
			public void onMapMarkersChanged() {
			}
			
			@Override
			public void onMapMarkerChanged(final MapMarker pMapMarker) {
				// FIXME: I don't know, if this is the right map-marker...
				MapMarkerChangedListener inst = this;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						helper.removeListener(inst);
						mNameTextField.setText(pMapMarker.getPointDescription(mContext).getName());
					}
				});
			}
		});
		helper.addMapMarker(position, null);
		mDialog.pack();
		mDialog.setVisible(true);

	}

	private void toggleNewGroup() {
		boolean sel = mNewGroupCheckBox.isSelected();
		mGroupNameTextField.setEnabled(sel);
		mGroupColorChooser.setEnabled(sel);
		mComboBox.setEnabled(!sel);
	}

	private void terminate(boolean pSaveResults) {
		if (pSaveResults) {
			// FIXME: Validierung1!
			LatLon pos = mContext.getCursorPosition();
			String groupName;
			if (mNewGroupCheckBox.isSelected()) {
				groupName = mGroupNameTextField.getText();
				mContext.getFavorites().addEmptyCategory(groupName, mGroupColorChooser.getColor().getRGB());
			} else {
				groupName = mComboBoxModel.getElementAt(mComboBox.getSelectedIndex()).name;
			}
			FavouritePoint point = new FavouritePoint(pos.getLatitude(), pos.getLongitude(), mNameTextField.getText(),
					groupName);
			point.setDescription(mDescriptionTextField.getText());
			mContext.getFavorites().addFavourite(point);
			mContext.getDrawPanel().drawLater();
		}
		disposeDialog();
	}

	@Override
	public void save() {
	}
}