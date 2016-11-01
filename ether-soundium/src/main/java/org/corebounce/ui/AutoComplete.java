package org.corebounce.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class AutoComplete {
	private final List<TableItem> items = new ArrayList<>();
	private final Table           table;
	private final Shell           popupShell;

	public AutoComplete(Text text) {
		popupShell = new Shell(text.getDisplay(), SWT.ON_TOP);
		popupShell.setLayout(new FillLayout());
		table = new Table(popupShell, SWT.SINGLE);

		text.addListener(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if(table.getItemCount() > 0) {
					switch (event.keyCode) {
					case SWT.ARROW_DOWN:
						int index = (table.getSelectionIndex() + 1) % table.getItemCount();
						table.setSelection(index);
						event.doit = false;
						break;
					case SWT.ARROW_UP:
						index = table.getSelectionIndex() - 1;
						if (index < 0) index = table.getItemCount() - 1;
						table.setSelection(index);
						event.doit = false;
						break;
					case SWT.CR:
						if (popupShell.isVisible() && table.getSelectionIndex() != -1) {
							text.setText(table.getSelection()[0].getText());
							text.selectAll();
							popupShell.setVisible(false);
						}
						break;
					case SWT.ESC:
						popupShell.setVisible(false);
						break;
					}
				}
			}
		});
		text.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				String string = text.getText();
				if (string.length() == 0) {
					popupShell.setVisible(false);
				} else {
					Rectangle textBounds = text.getDisplay().map(text, null, text.getBounds());
					popupShell.setBounds(textBounds.x, textBounds.y + textBounds.height, textBounds.width, table.getItemHeight() * table.getItemCount());
					popupShell.setVisible(true);
					String prefix = text.getText();
					for(int i = 0; i < items.size(); i++)
						if(items.get(i).getText().startsWith(prefix)) {
							table.select(i);
							return;
						}
					table.deselectAll();
				}
			}
		});

		table.addListener(SWT.DefaultSelection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				text.setText(table.getSelection()[0].getText());
				text.selectAll();
				popupShell.setVisible(false);
			}
		});
		table.addListener(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.keyCode == SWT.ESC) {
					popupShell.setVisible(false);
				}
			}
		});

		Listener focusOutListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				Display display = Display.getDefault();
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						if (display.isDisposed()) return;
						if(event.type == SWT.FocusOut) {
							Control control = display.getFocusControl();
							if(!(popupShell.isDisposed()) && (control == null || (control != text && control != table)))
								popupShell.setVisible(false);
						} else if(event.type == SWT.FocusIn) {
							Control control = display.getFocusControl();
							if (control instanceof Text)
								((Text)control).selectAll();
						}
					}
				});
			}
		};
		table.addListener(SWT.FocusOut, focusOutListener);
		text.addListener(SWT.FocusOut,  focusOutListener);
		text.addListener(SWT.FocusIn,   focusOutListener);

		text.getShell().addListener(SWT.Move, new Listener() {
			@Override
			public void handleEvent(Event event) {
				popupShell.setVisible(false);
			}
		});
	}

	public void addItem(String item) {
		TableItem titem = new TableItem(table, SWT.NONE);
		items.add(titem);
		titem.setText(item);
	}

	public void clear() {
		for(TableItem titem : items)
			titem.dispose();
		items.clear();
	}

	public void setVisible(boolean visible) {
		popupShell.setVisible(visible);
	}
}
