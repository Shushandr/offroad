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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.MapMarkersHelper.MapMarkerChangedListener;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.ui.FavoriteGroupRenderer;

public class AddFavoriteAction extends OffRoadAction implements DocumentListener {
	private JTextField mNameTextField;
	private JTextField mDescriptionTextField;
	private JComboBox<FavoriteGroup> mComboBox;
	private DefaultComboBoxModel<FavoriteGroup> mComboBoxModel;
	private JCheckBox mNewGroupCheckBox;
	private JTextField mGroupNameTextField;
	private JColorChooser mGroupColorChooser;
	private FavouritePoint mUpdatePoint;
	private JButton mOkButton;

	public AddFavoriteAction(OsmWindow pContext, String pName, Icon pIcon, FavouritePoint pUpdatePoint) {
		super(pContext, pName, pIcon);
		mUpdatePoint = pUpdatePoint;
	}

	@Override
	public void actionPerformed(ActionEvent pE) {
		createDialog();
		mDialog.setTitle(getWindowTitle());
		Container contentPane = mDialog.getContentPane();
		GridBagLayout gbl = new GridBagLayout();
		gbl.columnWeights = new double[] { 1.0f };
		gbl.rowWeights = new double[] { 1.0f };
		contentPane.setLayout(gbl);
		int y = 0;
		contentPane.add(new JLabel(getResourceString("offroad.favoriteName")), new GridBagConstraints(0, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		mNameTextField = new JTextField();
		mNameTextField.getDocument().addDocumentListener(this);
		contentPane.add(mNameTextField, new GridBagConstraints(1, y++, 2, 1, 4.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
		contentPane.add(new JLabel(getResourceString("offroad.favoriteDescription")), new GridBagConstraints(0, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		mDescriptionTextField = new JTextField();
		contentPane.add(mDescriptionTextField, new GridBagConstraints(1, y++, 2, 1, 4.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
		contentPane.add(new JLabel(getResourceString("offroad.favoriteGroup")), new GridBagConstraints(0, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		mComboBox = new JComboBox<>();
		mComboBoxModel = new DefaultComboBoxModel<>();
		for (FavoriteGroup filter : mContext.getFavorites().getFavoriteGroups()) {
			mComboBoxModel.addElement(filter);
		}
		mComboBox.setModel(mComboBoxModel);
		mComboBox.setFocusable(true);
		mComboBox.setRenderer(new FavoriteGroupRenderer<>());
		contentPane.add(mComboBox, new GridBagConstraints(1, y++, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
		contentPane.add(new JLabel(getResourceString("offroad.favoriteNewGroup")), new GridBagConstraints(0, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
		mNewGroupCheckBox = new JCheckBox();
		contentPane.add(mNewGroupCheckBox, new GridBagConstraints(1, y++, 2, 1, 4.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
		mNewGroupCheckBox.addActionListener(t -> toggleNewGroup());
		contentPane.add(new JLabel(getResourceString("offroad.favoriteGroupName")), new GridBagConstraints(0, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		mGroupNameTextField = new JTextField();
		mGroupNameTextField.getDocument().addDocumentListener(this);
		contentPane.add(mGroupNameTextField, new GridBagConstraints(1, y++, 2, 1, 4.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));

		contentPane.add(new JLabel(getResourceString("offroad.favoriteGroupColor")), new GridBagConstraints(0, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		mGroupColorChooser = new JColorChooser();
		contentPane.add(mGroupColorChooser, new GridBagConstraints(1, y++, 2, 1, 4.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
		
		
		mOkButton = new JButton(getResourceString("offroad.addFavoriteOK"));
		contentPane.add(mOkButton, new GridBagConstraints(2, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 0, 0), 0, 0));
		mOkButton.addActionListener(t -> terminate(true));
		JButton cancelButton = new JButton(getResourceString("offroad.addFavoriteCancel"));
		cancelButton.addActionListener(t -> terminate(false));
		contentPane.add(cancelButton, new GridBagConstraints(1, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 0, 0), 0, 0));
		mNewGroupCheckBox.setSelected(mComboBoxModel.getSize()==0);
		toggleNewGroup();
		
		if(mUpdatePoint != null){
			// fill with values:
			mNameTextField.setText(mUpdatePoint.getName());
			mDescriptionTextField.setText(mUpdatePoint.getDescription());
			mGroupColorChooser.setColor(mUpdatePoint.getColor());
			mGroupNameTextField.setText(mUpdatePoint.getCategory());
			// find category in combo:
			for (int i = 0; i < mComboBoxModel.getSize(); ++i){
				FavoriteGroup fg = mComboBoxModel.getElementAt(i);
				if(fg.name != null && fg.name.equals(mUpdatePoint.getCategory())){
					if(fg.color == mUpdatePoint.getColor()){
						mComboBox.setSelectedIndex(i);
						break;
					}
				}
			}
		} else {
			final MapMarkersHelper helper = mContext.getMapMarkersHelper();
			LatLon position = mContext.getCursorPosition();
			helper.addListener(new MapMarkerChangedListener() {
				
				@Override
				public void onMapMarkersChanged() {
				}
				
				@Override
				public void onMapMarkerChanged(final MapMarker pMapMarker) {
					// FIXME: I don't know, if this is the right map-marker...
					MapMarkerChangedListener inst = this;
					SwingUtilities.invokeLater(() -> {
						helper.removeListener(inst);
						mNameTextField.setText(pMapMarker.getPointDescription(mContext).getName());
					});
				}
			});
			helper.addMapMarker(position, null);
		}
		mDialog.pack();
		decorateDialog();
		mDialog.setVisible(true);

	}

	protected String getWindowTitle() {
		return getResourceString("offroad.addFavorite");
	}

	private void toggleNewGroup() {
		boolean sel = mNewGroupCheckBox.isSelected();
		mGroupNameTextField.setEnabled(sel);
		mGroupColorChooser.setEnabled(sel);
		mComboBox.setEnabled(!sel);
		validate();
	}

	private void terminate(boolean pSaveResults) {
		LatLon pos = mContext.getCursorPosition();
		if (pSaveResults) {
			String groupName=(mNewGroupCheckBox.isSelected())?createEmptyCategory():getSelectedCategory();
			if(mUpdatePoint!=null){
				// edit favorite:
				mContext.getFavorites().editFavouriteName(mUpdatePoint, mNameTextField.getText(), groupName, mDescriptionTextField.getText());
			} else {
				FavouritePoint point = new FavouritePoint(pos.getLatitude(), pos.getLongitude(), mNameTextField.getText(),
						groupName);
				point.setDescription(mDescriptionTextField.getText());
				mContext.getFavorites().addFavourite(point);
			}
			mContext.getDrawPanel().drawLater();
		}
		disposeDialog();
	}

	protected String createEmptyCategory() {
		String groupName;
		groupName = mGroupNameTextField.getText();
		mContext.getFavorites().addEmptyCategory(groupName, mGroupColorChooser.getColor().getRGB());
		return groupName;
	}

	protected String getSelectedCategory() {
		return mComboBoxModel.getElementAt(mComboBox.getSelectedIndex()).name;
	}

	@Override
	public void insertUpdate(DocumentEvent pE) {
		validate();
	}

	@Override
	public void removeUpdate(DocumentEvent pE) {
		validate();
	}

	private void validate() {
		mOkButton.setEnabled(mNameTextField.getText().length() > 0 && (!mNewGroupCheckBox.isSelected() || mGroupNameTextField.getText().length() > 0));
	}

	@Override
	public void changedUpdate(DocumentEvent pE) {
		validate();
	}
}
