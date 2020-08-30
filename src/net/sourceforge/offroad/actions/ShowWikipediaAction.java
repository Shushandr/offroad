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

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.R;

/**
 * @author foltin
 * @date 22.04.2016
 */
public class ShowWikipediaAction extends OffRoadAction {

	private String mContent;
	private String mTitle;
	private String mArticle;

	public ShowWikipediaAction(OsmWindow pContext, String pContent, String pTitle, String pArticle) {
		super(pContext);
		mContent = pContent;
		mTitle = pTitle;
		mArticle = pArticle;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
		createDialog();
		mDialog.setTitle(mTitle);
		Container contentPane = mDialog.getContentPane();
		GridBagLayout gbl = new GridBagLayout();
		gbl.columnWeights = new double[] { 1.0f };
		gbl.rowWeights = new double[] { 1.0f };
		contentPane.setLayout(gbl);
		int y = 0;
		String htmlContent = "<html><body><a href=''>" + mArticle + "</a></body></html>";
		JTextPane contentDisplay = new JTextPane();
		contentDisplay.setContentType("text/html"); // let the text pane know this is what you want
		contentDisplay.setText(mContent); // showing off
		contentDisplay.setEditable(false); // as before
		contentDisplay.setBackground(null); // this is the same as a JLabel
		contentDisplay.setBorder(null); // remove the border
		JLabel articleLabel = new JLabel(htmlContent);
		articleLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		articleLabel.setToolTipText(mArticle);
		articleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(mArticle));
                } catch (URISyntaxException | IOException ex) {
                	ex.printStackTrace();
                }
            }
        });
		contentPane.add(new JScrollPane(contentDisplay), new GridBagConstraints(0, y++, 4, 1, 4.0, 10.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		contentPane.add(articleLabel, new GridBagConstraints(0, y++, 4, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		JButton okButton = new JButton(mContext.getString(R.string.shared_string_ok));
		okButton.addActionListener(pE1 -> disposeDialog());
		contentPane.add(okButton, new GridBagConstraints(3, y++, 1, 1, 1.0, 0.0, GridBagConstraints.EAST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		mDialog.getRootPane().setDefaultButton(okButton);
		// select region:
		mDialog.pack();
		mDialog.setVisible(true);
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.offroad.actions.OffRoadAction#save()
	 */
	@Override
	public void save() {
	}

}
