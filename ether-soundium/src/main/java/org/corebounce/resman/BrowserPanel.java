package org.corebounce.resman;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.corebounce.engine.Engine;
import org.corebounce.io.OSC;
import org.corebounce.ui.AutoComplete;
import org.corebounce.ui.GridDataFactory;
import org.corebounce.video.Pusher;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolTip;

import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.platform.Platform.OS;
import ch.fhnw.util.ClassUtilities;
import ch.fhnw.util.LRUList;
import ch.fhnw.util.Log;
import ch.fhnw.util.TextUtilities;
import ch.fhnw.util.net.osc.IOSCHandler;

public class BrowserPanel implements SelectionListener, IChangeListener, Runnable, Comparator<Resource> {	
	private static final Log log = Log.create();

	private static final String K_RES  = "res";
	private static final String SEARCH = "search_history.txt";

	private final Engine         engine;
	private final MetaDB         db;
	private final PreviewFactory pf;
	private final OSC            osc;
	private List<Resource>       resources  = new ArrayList<>();
	private Table                table;
	private AtomicBoolean        scheduled = new AtomicBoolean();
	private String               search    = "";
	private String               sort      = Resource.NAME;
	private boolean              tooltip;
	private LRUList<String>      searchHistory;
	private AutoComplete         searchComplete;
	private int                  sortDir  = 1;
	private final Transfer[]     dndTypes = new Transfer[] { TextTransfer.getInstance() };
	private       Image          sortAscendingIcon; 
	private       Image          sortDescendingIcon; 
	private       Image          moviesIcon;
	private       Image          imagesIcon;
	private       Image          fontsIcon;
	private       Image          geometryIcon;
	private       Image          connectedIcon;
	private       Image          disconnectedIcon;
	private       boolean        showMovies   = true;
	private       boolean        showImages   = true;
	private       boolean        showFonts    = false;
	private       boolean        showGeometry = false;
	private final Pusher         pusher;

	public BrowserPanel(Engine engine, PreviewFactory pf, OSC osc, MetaDB db) {
		this.engine        = engine;
		this.db            = db;
		this.pf            = pf;
		this.osc           = osc;
		this.searchHistory = loadHistory();
		this.pusher        = new Pusher(db, pf, osc);

		this.db.addChangeListener(this);
	}

	private LRUList<String> loadHistory() {
		LRUList<String> result = new LRUList<>(16);
		try(BufferedReader in = new BufferedReader(new FileReader(new File(db.getConstraintsDir(), SEARCH)))) {
			for(;;) {
				String line = in.readLine();
				if(line == null) break;
				result.used(line);
			}
		} catch(Throwable t) {}
		return result;
	}

	private void storeHistory(LRUList<String> history) {
		try(FileWriter out = new FileWriter(new File(db.getConstraintsDir(), SEARCH))) {
			for(int i = searchHistory.size(); --i >= 0;)
				out.write(searchHistory.get(i) + "\n");
		} catch(Throwable t) {}
	}

	private Image icon(Display display, String name) throws IOException {
		return new Image(display, getClass().getResourceAsStream("/"+name+".png"));
	}

	public Composite createPartControl(Composite parent) {
		try {
			Display display = parent.getDisplay();
			sortAscendingIcon  = icon(display, "sort_ascending");
			sortDescendingIcon = icon(display, "sort_descending");
			moviesIcon         = icon(display, "video");
			imagesIcon         = icon(display, "image");
			fontsIcon          = icon(display, "font");
			geometryIcon       = icon(display, "3d");
			connectedIcon      = icon(display, "connected");
			disconnectedIcon   = icon(display, "disconnected");
		} catch(Throwable t) {
			log.severe(t);
		}
		Composite result = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth  = 0;
		layout.marginHeight = 0;
		result.setLayout(layout);
		result.setLayoutData(GridDataFactory.fill(true, true));

		toolbar(result);
		SashForm sash = new SashForm(result, SWT.HORIZONTAL | SWT.SMOOTH);
		sash.setLayoutData(GridDataFactory.fill(true, true));
		resList(sash);
		slots(sash);
		sash.setWeights(new int[] {70,30});

		run();

		return result;
	}

	private void slots(Composite parent) {
		Table table = new Table(parent, SWT.V_SCROLL);
		table.setLayoutData(GridDataFactory.fill(true, true));
		table.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		table.addListener(SWT.KeyDown, save(event->focusSearch(event)));

		itemPainter(table);
		dropTarget(table);

		osc.addHandler("/state", new IOSCHandler() {
			private AtomicReference<Object[]> args       = new AtomicReference<Object[]>(ClassUtilities.EMPTY_ObjectA);
			private AtomicBoolean             slotsUpdate = new AtomicBoolean();

			@Override
			public Object[] handle(String[] address, int addrIdx, StringBuilder typeString, long timestamp, Object ... cargs) {
				this.args.set(cargs);

				if(!(slotsUpdate.getAndSet(true))) {
					Display.getDefault().asyncExec(()->{
						if(table.isDisposed()) return;

						final Object[] args = this.args.get();

						boolean changed = false;
						Display display = Display.getDefault();
						if(args.length / 2 != table.getItemCount()) {
							for(int i = table.getItemCount(); --i >= args.length / 2;) {
								table.getItem(i).dispose();
								changed = true;
							}
						}
						engine.setMaster(((Number)args[0]).floatValue());
						for(int i = 1; i < args.length; i += 2) {
							String slot = args[i+0].toString();
							String md5  = args[i+1].toString();
							int tidx = i/2;
							TableItem item = null;
							if(tidx < table.getItemCount()) {
								item = table.getItem(tidx);
								if(!(item.getText().equals(slot))) {
									item.setText(slot);
									changed = true;
								}
							} else {
								item = new TableItem(table, SWT.NONE);
								item.setText(slot);
								changed = true;
							}
							if(item != null) {
								if(md5.length() > 8) {
									Resource res = db.getResourceForMD(md5);
									if(res != null)
										changed |= setItem(display, item, res, false);
								} else {
									changed |= item.getImage() != null;
									item.setImage((Image)null);
								}
							}
						}
						if(changed) {
							table.redraw();
						}
						slotsUpdate.set(false);
					});
				}
				return null;
			}
		});
	}

	private void dropTarget(Table table) {
		DropTarget target = new DropTarget(table, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
		target.setTransfer(dndTypes);
		target.addDropListener(new DropTargetAdapter() {
			@Override
			public void dragEnter(DropTargetEvent event) {
				try {
					if (event.detail == DND.DROP_DEFAULT) {
						event.detail = (event.operations & DND.DROP_COPY) != 0 ? DND.DROP_COPY : DND.DROP_NONE;
					}
					for (int i = 0, n = event.dataTypes.length; i < n; i++) {
						if (TextTransfer.getInstance().isSupportedType(event.dataTypes[i]))
							event.currentDataType = event.dataTypes[i];
					}
				} catch(Throwable t) {
					log.warning(t);
				}
			}

			@Override
			public void dragOver(DropTargetEvent event) {
				event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
			}

			@Override
			public void drop(DropTargetEvent event) {
				try {
					if (TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
						DropTarget target = (DropTarget) event.widget;
						Table table = (Table) target.getControl();
						int selection  = table.getItemCount();
						for(int i = table.getItemCount(); --i >= 0;)
							if(table.getItem(i) == event.item)
								selection = i;
						for(String path : TextUtilities.split((String) event.data, ';')) {
							Resource res = db.getResourceForPath(path);
							if(res != null && selection < table.getItemCount()) {
								TableItem item = table.getItem(selection);
								setItem(event.display, item, res, false);
								pusher.pushResource(item, res, selection);
								selection++;
							}
						}
					}
				} catch(Throwable t) {
					log.warning(t);
				}

			}
		});
	}

	private boolean setItem(Display display, TableItem item, Resource res, boolean setLabel) {
		try {
			boolean changed = false;
			if(setLabel) {
				changed |= !(res.getLabel().equals(item.getText()));
				item.setText(res.getLabel());
			}
			item.setData(K_RES, res);
			Image prevw = pf.getPreviewImage(res, display);
			changed |= item.getImage() != prevw;
			item.setImage(prevw);
			return changed;
		} catch(Throwable t) {
			return true;
		}
	}

	private void resList(Composite parent) {
		if(table != null) table.dispose();
		table = new Table(parent, SWT.VIRTUAL| SWT.V_SCROLL);
		table.setLayoutData(GridDataFactory.fill(true, true));
		table.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		table.addListener(SWT.SetData, save(event->setItem(event.display, (TableItem) event.item, resources.get(event.index), true)));
		table.addListener(SWT.KeyDown, save(event->focusSearch(event)));

		dragSource(table);
		itemPainter(table);
		tooltips(table); 
	}

	private static Listener save(Listener listner) {
		return new Listener() {
			@Override
			public void handleEvent(Event event) {
				try {listner.handleEvent(event);} catch(Throwable t) {log.warning(t);}
			}
		};
	}

	private void focusSearch(Event event) {
		try {
			if(Character.isLetterOrDigit(event.character)) {
				searchFieldUI.setFocus();
				event.widget = searchFieldUI;
				searchFieldUI.getDisplay().asyncExec(()->searchFieldUI.getDisplay().post(event));
			}
		} catch(Throwable t) {
			log.warning(t);
		}
	}

	private void tooltips(Table table) {
		Listener tableListener = save(new Listener(){ 
			ToolTip tooltip = null; 
			@Override
			public void handleEvent(Event event) { 
				if(event.type == SWT.KeyDown ); 
				else if(event.type == SWT.Dispose); 
				else if(event.type == SWT.MouseMove){ 
					if(tooltip != null)
						tooltip.dispose(); 
				} 
				else if(event.type == SWT.MouseHover && BrowserPanel.this.tooltip) { 
					TableItem item = table.getItem(new Point(event.x,event.y)); 
					if(item != null){ 
						Resource  res  = (Resource) item.getData(K_RES);
						if(res != null) {
							tooltip = new ToolTip(table.getShell(), SWT.BALLOON | SWT.ICON_INFORMATION);
							tooltip.setMessage(res.listProperties()); 
							tooltip.setVisible(true);
						} 
					}
				} 
			} 
		}); 

		table.addListener(SWT.MouseHover, tableListener); 
		table.addListener(SWT.MouseMove,  tableListener); 
		table.addListener(SWT.Dispose,    tableListener); 
		table.addListener(SWT.KeyDown,    tableListener);
	}

	private void dragSource(Table table) {
		DragSource source = new DragSource(table, DND.DROP_MOVE | DND.DROP_COPY);
		source.setTransfer(dndTypes);

		source.addDragListener(new DragSourceAdapter() {
			@Override
			public void dragSetData(DragSourceEvent event) {
				try {
					// Get the selected items in the drag source
					DragSource ds = (DragSource) event.widget;
					Table table = (Table) ds.getControl();
					StringBuffer buff = new StringBuffer();
					for(TableItem item : table.getSelection()) {
						Resource res = (Resource)item.getData(K_RES);
						if(res != null)
							buff.append(res.getPath()).append(';');
					}
					event.data = buff.toString();
				} catch(Throwable t) {
					log.warning(t);
				}
			}
		});
	}

	private void itemPainter(Table table) {
		final int UPDATE_MASK = ~(Platform.getOS() == OS.WINDOWS ? SWT.FOREGROUND | SWT.SELECTED | SWT.HOT : SWT.FOREGROUND);
		Listener paintListener = save(event->{
			TableItem item = (TableItem)event.item;

			switch(event.type) {		
			case SWT.MeasureItem: {
				event.x       = 0;
				event.width   = table.getClientArea().width;
				event.height  = PreviewFactory.STRIP_HEIGHT + event.gc.textExtent(item.getText()).y;
				break;
			}
			case SWT.PaintItem: {
				GC       gc   = event.gc;

				event.width   = table.getClientArea().width;

				gc.setClipping(0, event.y, event.width, event.height);
				gc.setBackground(event.display.getSystemColor(SWT.COLOR_BLACK));
				event.gc.fillRectangle(0, event.y, event.width, event.height);
				if((event.detail & SWT.SELECTED) != 0) {
					gc.setBackground(event.display.getSystemColor(SWT.COLOR_LIST_SELECTION));
					int svAlpha = gc.getAlpha();
					gc.setAlpha(64);
					event.gc.fillRectangle(0, event.y, event.width, event.height);
					gc.setAlpha(svAlpha);
				}

				String text      = item.getText(event.index);
				Point  size      = event.gc.textExtent(text);					
				int    offset2   = event.height - (size.y + 2);
				gc.setForeground(event.display.getSystemColor(SWT.COLOR_WHITE));
				gc.drawText(text, 0, event.y + offset2, true);
				if(item.getImage() != null)
					gc.drawImage(item.getImage(), 0, event.y);
				else {
					Resource res = (Resource)item.getData(K_RES); 
					if(res != null && res.getProgress() >= 0) {
						gc.setBackground(event.display.getSystemColor(SWT.COLOR_GRAY));
						int off = 5;
						int sz  = PreviewFactory.STRIP_HEIGHT - (2*off);
						gc.fillArc(off,  event.y + off, sz, sz, 90, (int)(-360 * res.getProgress()));
						gc.drawOval(off, event.y + off, sz, sz);
					}
				}
				break;
			}
			case SWT.EraseItem: {
				event.detail &= UPDATE_MASK;
				break;
			}
			}
		});
		table.addListener(SWT.MeasureItem, paintListener);
		table.addListener(SWT.PaintItem,   paintListener);
		table.addListener(SWT.EraseItem,   paintListener);
	}

	private Text   searchFieldUI;
	private Button sortDirectionUI;
	private Combo  sortKeyUI;
	private Button tooltipUI;
	private Label  countUI;
	private Button moviesUI;
	private Button imagesUI;
	private Button fontsUI;
	private Button geometryUI;
	private Label  connectionUI;

	private Label vsep(Composite parent) {
		Label result = new Label(parent, SWT.SEPARATOR | SWT.VERTICAL);
		result.setLayoutData(GridDataFactory.fill(false, true, SWT.DEFAULT, 24));
		return result;
	}

	private void toolbar(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout(14, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing   = 0;
		panel.setLayout(layout);
		panel.setLayoutData(GridDataFactory.fill(true, false));

		searchFieldUI = new Text(panel, SWT.SEARCH | SWT.ICON_CANCEL | SWT.ICON_SEARCH);
		searchFieldUI.setLayoutData(GridDataFactory.fill(true, false));
		searchFieldUI.addSelectionListener(this);
		searchComplete = new AutoComplete(searchFieldUI);
		for(String item : searchHistory)
			searchComplete.addItem(item);

		countUI = new Label(panel, SWT.CENTER);
		countUI.setLayoutData(GridDataFactory.fill(false, false, 100, SWT.DEFAULT));

		vsep(panel);

		moviesUI   = filter(panel, moviesIcon,   showMovies);
		imagesUI   = filter(panel, imagesIcon,   showImages);
		geometryUI = filter(panel, geometryIcon, showGeometry);
		fontsUI    = filter(panel, fontsIcon,    showFonts);

		vsep(panel);

		sortDirectionUI = new Button(panel, SWT.NONE);
		sortDirectionUI.setImage(sortAscendingIcon);
		sortDirectionUI.addSelectionListener(this);

		sortKeyUI = new Combo(panel, SWT.READ_ONLY);
		for(String prop : Resource.getUserProperties())
			sortKeyUI.add(prop);
		sortKeyUI.select(0);
		sortKeyUI.addSelectionListener(this);

		vsep(panel);

		tooltipUI = new Button(panel, SWT.CHECK);
		tooltipUI.setText("Tooltips");
		tooltipUI.addSelectionListener(this);

		vsep(panel);

		connectionUI = new Label(panel, SWT.NONE);
		connectionUI.setImage(disconnectedIcon);
		new Repeating(500, 500, ()->{
			if(connectionUI.isDisposed()) return;
			connectionUI.setImage(System.currentTimeMillis()-osc.lastMessageTime() > 2000 ? disconnectedIcon : connectedIcon);
		});

		searchFieldUI.setFocus();
	}

	private Button filter(Composite panel, Image icon, boolean state) {
		Button result = new Button(panel, SWT.TOGGLE);
		result.setImage(icon);
		result.addSelectionListener(this);
		result.setSelection(state);
		return result;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		widgetDefaultSelected(e);
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		try {
			Object src = e.getSource();
			if(src == searchFieldUI) {
				search = searchFieldUI.getText().trim();
				searchHistory.used(search);
				storeHistory(searchHistory);
				searchComplete.clear();
				for(String item : searchHistory)
					searchComplete.addItem(item);
				searchComplete.setVisible(false);
			}
			else if(src == sortKeyUI)  
				sort = sortKeyUI.getText();
			else if(src == tooltipUI)
				tooltip = tooltipUI.getSelection();
			else if(src == sortDirectionUI) {
				if(sortDirectionUI.getImage() == sortAscendingIcon) {
					sortDirectionUI.setImage(sortDescendingIcon);
					sortDir = -1;
				} else {
					sortDirectionUI.setImage(sortAscendingIcon);
					sortDir = 1;
				}
			} 
			else if(src == moviesUI)   showMovies   = moviesUI.getSelection();
			else if(src == imagesUI)   showImages   = imagesUI.getSelection();
			else if(src == geometryUI) showGeometry = geometryUI.getSelection();
			else if(src == fontsUI)    showFonts    = fontsUI.getSelection();
			run();
		} catch(Throwable t) {
			log.warning(t);
		}

	}

	@Override
	public void metaDBchanged() {
		if(!(scheduled.getAndSet(true)))
			Display.getDefault().asyncExec(this);
	}

	@Override
	public void run() {
		if(table.isDisposed()) return;
		resources.clear();
		for(Resource res : db.getResources())
			if(show(res) && res.getPath().contains(search))
				resources.add(res);
		resources.sort(this);
		table.setItemCount(resources.size());
		table.clearAll();
		countUI.setText(Integer.toString(resources.size()));
		scheduled.set(false);
	}

	private boolean show(Resource res) {
		if((showMovies || showImages) && res.getProperty(PreviewFactory.P_FRAMES) != null) {
			int frames = Integer.parseInt(res.getProperty(PreviewFactory.P_FRAMES));
			if(frames == 1 && showImages) return true;
			if(frames > 1 && showMovies) return true;
		}
		if(showFonts    && res.getProperty(PreviewFactory.P_FONT_NAME) != null) return true;
		if(showGeometry && res.getProperty(PreviewFactory.P_FACES)     != null) return true;
		return false;
	}

	@Override
	public int compare(Resource r1, Resource r2) {
		if(Resource.DATE.equals(sort)) {
			return r1.getDate().compareTo(r2.getDate()) * sortDir;
		} else {
			String p1 = r1.getProperty(sort);
			String p2 = r2.getProperty(sort);
			if(p1 == null) p1 = ClassUtilities.EMPTY_String;
			if(p2 == null) p2 = ClassUtilities.EMPTY_String;
			try {
				if(isNumber(p1) && isNumber(p2))
					return Double.compare(Double.parseDouble(p1), Double.parseDouble(p2)) * sortDir;
			} catch(Throwable t) {}
			return p1.compareTo(p2) * sortDir;
		}
	}

	private boolean isNumber(String s) {
		return s.length() > 0 && TextUtilities.NUMBERS.indexOf(s.charAt(0)) >= 0;
	}
}
