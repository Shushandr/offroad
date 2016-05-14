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

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 09.04.2016
 */
public abstract class OffRoadAction extends AbstractAction {
	protected final static Log log = PlatformUtil.getLog(OffRoadAction.class);

	public interface SelectableAction {

	}

	protected OsmWindow mContext;
	protected JDialog mDialog;

	public OffRoadAction(OsmWindow pContext) {
		this(pContext, null, null);
	}

	public OffRoadAction(OsmWindow pContext, String name, Icon icon) {
		super(name, icon);
		mContext = pContext;
	}

	public static void addEscapeActionToDialog(JDialog dialog, Action action) {
		addKeyActionToDialog(dialog, action, "ESCAPE", "end_dialog");
	}

	public static void addKeyActionToDialog(JDialog dialog, Action action, String keyStroke, String actionId) {
		action.putValue(Action.NAME, actionId);
		// Register keystroke
		dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyStroke),
				action.getValue(Action.NAME));

		// Register action
		dialog.getRootPane().getActionMap().put(action.getValue(Action.NAME), action);
	}

	protected void setWaitingCursor() {
		mDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		mContext.setWaitingCursor(true);
	}

	protected void removeWaitingCursor() {
		mDialog.setCursor(Cursor.getDefaultCursor());
		mContext.setWaitingCursor(false);
	}

	protected void disposeDialog() {
		save();
		mDialog.setVisible(false);
		mDialog.dispose();
	}

	public abstract void save();

	protected String getResourceString(String pWindowTitle) {
		return mContext.getOffRoadString(pWindowTitle);
	}

	public void createDialog() {
		mDialog = new JDialog(mContext.getWindow(), true /* modal */);
		mDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		mDialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				disposeDialog();
			}
		});
		addEscapeActionToDialog(mDialog, new AbstractAction() {
			public void actionPerformed(ActionEvent arg0) {
				disposeDialog();
			}
		});
	}

	public boolean isSelected() {
		return true;
	}

	public static class OffRoadMenuItem extends JCheckBoxMenuItem {
		private JMenu mMenu;

		public OffRoadMenuItem(PoiFilterAction pPoiFilterAction, JMenu pMenu) {
			super(pPoiFilterAction);
			mMenu = pMenu;
			mMenu.addMenuListener(new MenuListener() {
				
				@Override
				public void menuSelected(MenuEvent pE) {
					OffRoadMenuItem.this.setSelected(OffRoadMenuItem.this.isSelected());
				}
				
				@Override
				public void menuDeselected(MenuEvent pE) {
				}
				
				@Override
				public void menuCanceled(MenuEvent pE) {
				}
			});
		}

		@Override
		public boolean isSelected() {
			Action action = getAction();
			if (action instanceof SelectableAction && action instanceof OffRoadAction) {
				OffRoadAction ora = (OffRoadAction) action;
				return ora.isSelected();
			}
			return super.isSelected();
		}
	}

}
