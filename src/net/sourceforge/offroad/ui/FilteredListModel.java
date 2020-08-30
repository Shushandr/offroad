package net.sourceforge.offroad.ui;

import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * Taken from http://stackoverflow.com/questions/14758313/filtering-jlist-based-on-jtextfield
 * 
 * @author foltin
 * @date 05.04.2016
 */
public class FilteredListModel<T> extends AbstractListModel<T> {
	public abstract class Filter {
		public abstract boolean accept(T element);
		public String getText() {
			return mText;
		}
		public void setText(String pText) {
			mText = pText;
		}
		private String mText;
	}

	private final DefaultListModel<T> _source;
	private Filter _filter;
	private final ArrayList<Integer> _indices = new ArrayList<>();
	private ListDataListener mListener;

	public FilteredListModel(DefaultListModel<T> source) {
		if (source == null)
			throw new IllegalArgumentException("Source is null");
		_source = source;
		mListener = new ListDataListener() {
			public void intervalRemoved(ListDataEvent e) {
				doFilter();
			}

			public void intervalAdded(ListDataEvent e) {
				doFilter();
			}

			public void contentsChanged(ListDataEvent e) {
				doFilter();
			}
		};
		_source.addListDataListener(mListener);
	}

	public void setFilter(Filter f) {
		_filter = f;
		doFilter();
	}

	private void doFilter() {
		_indices.clear();

		Filter f = _filter;
		if (f != null) {
			int count = _source.getSize();
			for (int i = 0; i < count; i++) {
				T element = _source.getElementAt(i);
				if (f.accept(element)) {
					_indices.add(i);
				}
			}
		}
		fireContentsChanged(this, 0, getSize() - 1);
	}

	public int getSize() {
		return (_filter != null) ? _indices.size() : _source.getSize();
	}

	public T getElementAt(int index) {
		return (_filter != null) ? _source.getElementAt(_indices.get(index)) : _source.getElementAt(index);
	}

}
